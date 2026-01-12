package com.backend.repository;

import com.backend.dto.TradeDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public class TradeRepository {
    private final JdbcTemplate jdbc;

    public TradeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long insertTrade(long accountId, String mode, String symbol, String transaction_type,
                            BigDecimal qty, BigDecimal price,
                            BigDecimal fee, BigDecimal realizedPnl,
                            OffsetDateTime purchasedAt) {
        return jdbc.queryForObject("""
                INSERT INTO trades (account_id, mode, symbol, transaction_type, quantity, price, fee, realized_pnl, purchased_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """, Long.class, accountId, mode, symbol, transaction_type, qty, price, fee, realizedPnl, purchasedAt);
    }

    public List<TradeDTO> listTrades(String mode, String symbol, int limit) {
        return jdbc.query("""
                        SELECT id, mode, symbol, transaction_type, quantity, price, fee, realized_pnl, purchased_at
                        FROM trades
                        WHERE mode=? AND symbol=?
                        ORDER BY purchased_at DESC
                        LIMIT ?
                        """,
                (rs, i) -> new TradeDTO(
                        rs.getLong("id"),
                        rs.getString("mode"),
                        rs.getString("symbol"),
                        rs.getString("transaction_type"),
                        rs.getBigDecimal("quantity"),
                        rs.getBigDecimal("price"),
                        rs.getBigDecimal("fee"),
                        rs.getBigDecimal("realized_pnl"),
                        rs.getObject("purchased_at", java.time.OffsetDateTime.class)
                ),
                mode, symbol, limit
        );
    }

    public void deleteByModeAndSymbol(long accountId, String mode, String symbol) {
        jdbc.update("DELETE FROM trades WHERE account_id=? AND mode=? AND symbol=?", accountId, mode, symbol);
    }
}