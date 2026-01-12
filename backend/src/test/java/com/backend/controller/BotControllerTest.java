package com.backend.controller;

import com.backend.dto.BotMode;
import com.backend.dto.StartBotRequest;
import com.backend.services.BotEngine;
import com.backend.services.LiveBotRunner;
import com.backend.services.MarketDataService;
import com.backend.models.Candles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BotControllerTest {

    @Mock
    private BotEngine engine;

    @Mock
    private MarketDataService market;

    @Mock
    private LiveBotRunner liveRunner;

    @InjectMocks
    private BotController controller;

    @Captor
    private ArgumentCaptor<List<Candles>> candlesCaptor;

    @BeforeEach
    void setUp() {
        // injected by Mockito
    }

    @Test
    void pauseShouldStopEngine() {
        controller.pause();
        verify(engine, times(1)).stop();
    }

    @Test
    void resetShouldDelegateToEngine() {
        controller.reset(BotMode.TRAIN, "BTCUSD");
        verify(engine, times(1)).reset(BotMode.TRAIN, "BTCUSD");
    }

    @Test
    void statusShouldDelegateToEngine() {
        when(engine.isRunning()).thenReturn(true);
        when(engine.getMode()).thenReturn(BotMode.LIVE);
        when(engine.getSymbol()).thenReturn("ETHUSD");
        when(engine.getInterval()).thenReturn("1m");

        var status = controller.status();

        assertNotNull(status);
        verify(engine).isRunning();
        verify(engine).getMode();
        verify(engine).getSymbol();
        verify(engine).getInterval();
        assertEquals(BotMode.LIVE, status.mode());
        assertEquals("ETHUSD", status.symbol());
        assertEquals("1m", status.interval());
        assertTrue(status.running());
    }

    @Test
    void startInTrainModeShouldFetchCandlesAndRunBacktest() {
        StartBotRequest req = mock(StartBotRequest.class);
        when(req.mode()).thenReturn(BotMode.TRAIN);
        when(req.symbol()).thenReturn("BTCUSD");
        when(req.interval()).thenReturn("5m");
        when(req.startTime()).thenReturn("2025-01-01T00:00:00Z");
        when(req.endTime()).thenReturn("2025-01-02T00:00:00Z");
        when(req.initialBalance()).thenReturn(1000.0);
        when(req.riskPct()).thenReturn(1.5);

        long expectedStart = OffsetDateTime.parse("2025-01-01T00:00:00Z").toInstant().toEpochMilli();
        long expectedEnd = OffsetDateTime.parse("2025-01-02T00:00:00Z").toInstant().toEpochMilli();

        when(market.candles(eq("BTCUSD"), eq("5m"), eq(expectedStart), eq(expectedEnd), eq(1000)))
                .thenReturn(Collections.emptyList());

        controller.start(req);

        verify(market).candles(eq("BTCUSD"), eq("5m"), eq(expectedStart), eq(expectedEnd), eq(1000));
        verify(engine).runBacktest(
                eq("BTCUSD"),
                eq("5m"),
                candlesCaptor.capture(),
                eq(BigDecimal.valueOf(1000.0)),
                eq(BigDecimal.valueOf(1.5))
        );
        assertEquals(Collections.emptyList(), candlesCaptor.getValue());
    }

    @Test
    void startInTrainModeWithNullEndShouldPassNullToMarket() {
        StartBotRequest req = mock(StartBotRequest.class);
        when(req.mode()).thenReturn(BotMode.TRAIN);
        when(req.symbol()).thenReturn("BTCUSD");
        when(req.interval()).thenReturn("1h");
        when(req.startTime()).thenReturn("");
        when(req.endTime()).thenReturn(null);
        when(req.initialBalance()).thenReturn(200.0);
        when(req.riskPct()).thenReturn(0.75);

        when(market.candles(eq("BTCUSD"), eq("1h"), isNull(), isNull(), eq(1000)))
                .thenReturn(Collections.emptyList());

        controller.start(req);

        verify(market).candles(eq("BTCUSD"), eq("1h"), isNull(), isNull(), eq(1000));
        verify(engine).runBacktest(
                eq("BTCUSD"),
                eq("1h"),
                candlesCaptor.capture(),
                eq(BigDecimal.valueOf(200.0)),
                eq(BigDecimal.valueOf(0.75))
        );
        assertEquals(Collections.emptyList(), candlesCaptor.getValue());
    }

    @Test
    void startInLiveModeShouldStartLiveAndSetRiskPct() {
        StartBotRequest req = mock(StartBotRequest.class);
        when(req.mode()).thenReturn(BotMode.LIVE);
        when(req.symbol()).thenReturn("ETHUSD");
        when(req.interval()).thenReturn("1m");
        when(req.initialBalance()).thenReturn(500.0);
        when(req.riskPct()).thenReturn(2.25);

        controller.start(req);

        verify(engine).startLive(eq("ETHUSD"), eq("1m"), eq(BigDecimal.valueOf(500.0)));
        verify(liveRunner).setRiskPct(eq(BigDecimal.valueOf(2.25)));
    }

    @Test
    void startWithInvalidIsoShouldThrow() {
        StartBotRequest req = mock(StartBotRequest.class);
        when(req.mode()).thenReturn(BotMode.TRAIN);
        when(req.startTime()).thenReturn("not-a-date");

        assertThrows(IllegalArgumentException.class, () -> controller.start(req));
        verify(engine).stop();
    }
}