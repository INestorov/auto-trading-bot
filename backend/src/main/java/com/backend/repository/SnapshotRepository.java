package com.backend.repository;


import com.backend.dto.SnapshotDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public class SnapshotRepository {
    private final JdbcTemplate jdbc;

    public SnapshotRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long insertSnapshot(long accountId, String mode, String symbol, OffsetDateTime purchased_at,
                               BigDecimal cash, BigDecimal posQty, BigDecimal posValue, BigDecimal total) {
        return jdbc.queryForObject("""
                INSERT INTO portfolio_snapshots (account_id, mode, symbol, purchased_at, cash_balance, position_qty, position_value, total_value)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """, Long.class, accountId, mode, symbol, purchased_at, cash, posQty, posValue, total);
    }

    public List<SnapshotDTO> list(String mode, String symbol, int limit) {
        return jdbc.query("""
                        SELECT id, mode, symbol, purchased_at, cash_balance, position_qty, position_value, total_value
                        FROM portfolio_snapshots
                        WHERE mode=? AND symbol=?
                        ORDER BY purchased_at ASC
                        LIMIT ?
                        """,
                (rs, i) -> new SnapshotDTO(
                        rs.getLong("id"),
                        rs.getString("mode"),
                        rs.getString("symbol"),
                        rs.getObject("purchased_at", OffsetDateTime.class),
                        rs.getBigDecimal("cash_balance"),
                        rs.getBigDecimal("position_qty"),
                        rs.getBigDecimal("position_value"),
                        rs.getBigDecimal("total_value")
                ),
                mode, symbol, limit
        );
    }

    public void deleteByModeAndSymbol(long accountId, String mode, String symbol) {
        jdbc.update("DELETE FROM portfolio_snapshots WHERE account_id=? AND mode=? AND symbol=?", accountId, mode, symbol);
    }
}