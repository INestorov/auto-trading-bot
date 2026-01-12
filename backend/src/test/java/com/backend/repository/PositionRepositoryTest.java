package com.backend.repository;

import com.backend.models.PositionRow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.math.BigDecimal;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PositionRepositoryTest {

    @Test
    void getPosition_returnsRowWhenExists() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        ResultSet rs = mock(ResultSet.class);
        when(rs.next()).thenReturn(true);
        when(rs.getBigDecimal("quantity")).thenReturn(new BigDecimal("2.5"));
        when(rs.getBigDecimal("avg_entry_price")).thenReturn(new BigDecimal("10.75"));

        when(jdbc.query(anyString(), any(ResultSetExtractor.class), anyLong(), anyString()))
                .thenAnswer(invocation -> {
                    ResultSetExtractor<?> extractor = invocation.getArgument(1);
                    return extractor.extractData(rs);
                });

        PositionRepository repo = new PositionRepository(jdbc);
        PositionRow row = repo.getPosition(123L, "BTCUSD");

        assertEquals(new BigDecimal("2.5"), row.quantity());
        assertEquals(new BigDecimal("10.75"), row.avgEntry());
    }

    @Test
    void getPosition_returnsZeroWhenNotExists() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        ResultSet rs = mock(ResultSet.class);
        when(rs.next()).thenReturn(false);

        when(jdbc.query(anyString(), any(ResultSetExtractor.class), anyLong(), anyString()))
                .thenAnswer(invocation -> {
                    ResultSetExtractor<?> extractor = invocation.getArgument(1);
                    return extractor.extractData(rs);
                });

        PositionRepository repo = new PositionRepository(jdbc);
        PositionRow row = repo.getPosition(123L, "BTCUSD");

        assertEquals(BigDecimal.ZERO, row.quantity());
        assertEquals(BigDecimal.ZERO, row.avgEntry());
    }

    @Test
    void upsertPosition_callsUpdateWithExpectedParams() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        PositionRepository repo = new PositionRepository(jdbc);

        BigDecimal qty = new BigDecimal("3");
        BigDecimal avg = new BigDecimal("5.5");
        repo.upsertPosition(1L, "BTCUSD", qty, avg);

        verify(jdbc).update(anyString(), eq(1L), eq("BTCUSD"), eq(qty), eq(avg));
    }
}