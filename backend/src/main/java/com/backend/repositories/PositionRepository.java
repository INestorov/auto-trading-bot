package com.backend.repositories;

import com.backend.models.PositionRow;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public class PositionRepository {
    private final JdbcTemplate jdbc;

    public PositionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public PositionRow getPosition(long accountId, String symbol) {
        return jdbc.query(
                "SELECT quantity, avg_entry_price FROM positions WHERE account_id=? AND symbol=?",
                rs -> rs.next()
                        ? new PositionRow(rs.getBigDecimal("quantity"), rs.getBigDecimal("avg_entry_price"))
                        : new PositionRow(BigDecimal.ZERO, BigDecimal.ZERO),
                accountId, symbol
        );
    }

    public void upsertPosition(long accountId, String symbol, BigDecimal quantity, BigDecimal avgEntry) {
        jdbc.update("""
                INSERT INTO positions (account_id, symbol, quantity, avg_entry_price, updated_at)
                VALUES (?, ?, ?, ?, NOW())
                ON CONFLICT (account_id, symbol)
                DO UPDATE SET quantity=EXCLUDED.quantity, avg_entry_price=EXCLUDED.avg_entry_price, updated_at=NOW()
                """, accountId, symbol, quantity, avgEntry);
    }
}