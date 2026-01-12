package com.backend.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public class AccountRepository {
    private final JdbcTemplate jdbc;

    public AccountRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long getDefaultAccountId() {
        return jdbc.queryForObject("SELECT id FROM accounts ORDER BY id ASC LIMIT 1", Long.class);
    }

    public BigDecimal getCash(long accountId) {
        return jdbc.queryForObject("SELECT cash_balance FROM accounts WHERE id=?", BigDecimal.class, accountId);
    }

    public void setCash(long accountId, BigDecimal cash) {
        jdbc.update("UPDATE accounts SET cash_balance=? WHERE id=?", cash, accountId);
    }
}