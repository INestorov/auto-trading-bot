package com.backend.controller;

import com.backend.dto.SnapshotDTO;
import com.backend.dto.TradeDTO;
import com.backend.models.Candles;
import com.backend.repository.SnapshotRepository;
import com.backend.repository.TradeRepository;
import com.backend.services.MarketDataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataControllerTest {

    @Test
    void tradesDelegatesToRepository() {
        TradeRepository trades = mock(TradeRepository.class);
        SnapshotRepository snapshots = mock(SnapshotRepository.class);
        MarketDataService market = mock(MarketDataService.class);

        List<TradeDTO> expected = List.of(mock(TradeDTO.class));
        when(trades.listTrades("modeA", "SYM", 500)).thenReturn(expected);

        DataController controller = new DataController(trades, snapshots, market);
        List<TradeDTO> actual = controller.trades("modeA", "SYM", 500);

        assertSame(expected, actual);
        verify(trades).listTrades("modeA", "SYM", 500);
        verifyNoMoreInteractions(trades, snapshots, market);
    }

    @Test
    void snapshotsDelegatesToRepository() {
        TradeRepository trades = mock(TradeRepository.class);
        SnapshotRepository snapshots = mock(SnapshotRepository.class);
        MarketDataService market = mock(MarketDataService.class);

        List<SnapshotDTO> expected = List.of(mock(SnapshotDTO.class));
        when(snapshots.list("modeB", "ASSET", 2000)).thenReturn(expected);

        DataController controller = new DataController(trades, snapshots, market);
        List<SnapshotDTO> actual = controller.snapshots("modeB", "ASSET", 2000);

        assertSame(expected, actual);
        verify(snapshots).list("modeB", "ASSET", 2000);
        verifyNoMoreInteractions(trades, snapshots, market);
    }

    @Test
    void candlesForwardsParametersAndClampsLimit() {
        TradeRepository trades = mock(TradeRepository.class);
        SnapshotRepository snapshots = mock(SnapshotRepository.class);
        MarketDataService market = mock(MarketDataService.class);

        List<Candles> expected = List.of(mock(Candles.class));
        when(market.candles("BTCUSDT", "1m", null, null, 1000)).thenReturn(expected);

        DataController controller = new DataController(trades, snapshots, market);
        List<Candles> actual = controller.candles("BTCUSDT", "1m", null, null, 2000);

        assertSame(expected, actual);
        // verify limit was clamped to 1000
        verify(market).candles(eq("BTCUSDT"), eq("1m"), isNull(), isNull(), eq(1000));
    }

    @Test
    void candlesForwardsStartEnd() {
        TradeRepository trades = mock(TradeRepository.class);
        SnapshotRepository snapshots = mock(SnapshotRepository.class);
        MarketDataService market = mock(MarketDataService.class);

        long start = 1_600_000_000L;
        long end = 1_600_000_500L;
        List<Candles> expected = List.of(mock(Candles.class));
        when(market.candles("ETHUSDT", "5m", start, end, 300)).thenReturn(expected);

        DataController controller = new DataController(trades, snapshots, market);
        List<Candles> actual = controller.candles("ETHUSDT", "5m", start, end, 300);

        assertSame(expected, actual);
        verify(market).candles("ETHUSDT", "5m", start, end, 300);
    }
}