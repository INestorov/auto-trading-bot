package com.backend.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class AccountRepositoryTest {

    private JdbcTemplate jdbc;
    private AccountRepository repo;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        repo = new AccountRepository(jdbc);
    }

    @Test
    void getDefaultAccountId_returnsValue() {
        when(jdbc.queryForObject(anyString(), eq(Long.class))).thenReturn(42L);

        long result = repo.getDefaultAccountId();

        assertEquals(42L, result);
        verify(jdbc).queryForObject("SELECT id FROM accounts ORDER BY id ASC LIMIT 1", Long.class);
    }

    @Test
    void getCash_returnsBigDecimal() {
        long accountId = 7L;
        BigDecimal expected = new BigDecimal("100.50");
        when(jdbc.queryForObject(anyString(), eq(BigDecimal.class), anyLong())).thenReturn(expected);

        BigDecimal result = repo.getCash(accountId);

        assertNotNull(result);
        assertEquals(0, expected.compareTo(result));
        verify(jdbc).queryForObject("SELECT cash_balance FROM accounts WHERE id=?", BigDecimal.class, accountId);
    }

    @Test
    void setCash_callsUpdate() {
        long accountId = 5L;
        BigDecimal cash = new BigDecimal("250.00");

        repo.setCash(accountId, cash);

        verify(jdbc).update("UPDATE accounts SET cash_balance=? WHERE id=?", cash, accountId);
    }
}