package com.backend.repository;

import com.backend.dto.SnapshotDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SnapshotRepositoryTest {

    @Mock
    JdbcTemplate jdbc;

    SnapshotRepository repo() {
        return new SnapshotRepository(jdbc);
    }

    @Test
    @SuppressWarnings("unchecked")
    void insertSnapshot_returnsGeneratedId() {
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(123L);

        long id = repo().insertSnapshot(1L, "TEST", "BTCUSD",
                OffsetDateTime.parse("2024-01-01T00:00:00Z"),
                BigDecimal.valueOf(100), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        assertEquals(123L, id);
        verify(jdbc).queryForObject(anyString(), eq(Long.class), any(Object[].class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void list_returnsMappedSnapshots() {
        SnapshotDTO dto = new SnapshotDTO(1L, "TEST", "BTCUSD",
                OffsetDateTime.parse("2024-01-01T00:00:00Z"),
                BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of(dto));

        List<SnapshotDTO> result = repo().list("TEST", "BTCUSD", 10);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());
        verify(jdbc).query(anyString(), any(RowMapper.class), any(Object[].class));
    }

    @Test
    void deleteByModeAndSymbol_callsUpdate() {
        repo().deleteByModeAndSymbol(2L, "TEST", "BTCUSD");
        verify(jdbc).update(eq("DELETE FROM portfolio_snapshots WHERE account_id=? AND mode=? AND symbol=?"),
                eq(2L), eq("TEST"), eq("BTCUSD"));
    }
}