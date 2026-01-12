package com.backend.repository;

import com.backend.dto.TradeDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TradeRepositoryTest {

    @Mock
    private JdbcTemplate jdbc;

    private TradeRepository repo;

    @BeforeEach
    void setUp() {
        repo = new TradeRepository(jdbc);
    }

    @Test
    void insertTrade_returnsId() {
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(42L);

        long id = repo.insertTrade(1L, "TEST", "BTCUSDT", "BUY",
                BigDecimal.ONE, BigDecimal.valueOf(100), BigDecimal.ZERO, BigDecimal.ZERO,
                OffsetDateTime.now());

        assertEquals(42L, id);
        verify(jdbc, times(1)).queryForObject(anyString(), eq(Long.class), any(Object[].class));
    }

    @Test
    void listTrades_mapsRows() {
        TradeDTO dto = new TradeDTO(1L, "TEST", "BTCUSDT", "BUY",
                BigDecimal.ONE, BigDecimal.valueOf(100), BigDecimal.ZERO, BigDecimal.ZERO,
                OffsetDateTime.now());
        when(jdbc.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(dto));

        List<TradeDTO> result = repo.listTrades("TEST", "BTCUSDT", 10);

        assertEquals(1, result.size());
        assertEquals("BTCUSDT", result.get(0).symbol());
        verify(jdbc, times(1)).query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), any(Object[].class));
    }

    @Test
    void deleteByModeAndSymbol_callsUpdate() {
        repo.deleteByModeAndSymbol(1L, "TEST", "BTCUSDT");

        verify(jdbc, times(1)).update("DELETE FROM trades WHERE account_id=? AND mode=? AND symbol=?", 1L, "TEST", "BTCUSDT");
    }
}