package com.backend.controllers;

import com.backend.dto.BotMode;
import com.backend.dto.BotStatusDTO;
import com.backend.dto.StartBotRequest;
import com.backend.logic.BotEngine;
import com.backend.logic.LiveBotRunner;
import com.backend.models.Candles;
import com.backend.logic.MarketDataService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/api/bot")
public class BotController {

    private final BotEngine engine;
    private final MarketDataService market;
    private final LiveBotRunner liveRunner;

    public BotController(BotEngine engine, MarketDataService market, LiveBotRunner liveRunner) {
        this.engine = engine;
        this.market = market;
        this.liveRunner = liveRunner;
    }

    @GetMapping("/status")
    public BotStatusDTO status() {
        return new BotStatusDTO(engine.isRunning(), engine.getMode(), engine.getSymbol(), engine.getInterval());
    }

    @PostMapping("/pause")
    public void pause() {
        engine.stop();
    }

    @PostMapping("/reset")
    public void reset(@RequestParam BotMode mode, @RequestParam String symbol) {
        engine.reset(mode, symbol);
    }

    @PostMapping("/start")
    public void start(@Valid @RequestBody StartBotRequest req) {
        // Prevent overlapping sessions
        engine.stop();

        if (req.mode() == BotMode.TRAIN) {
            Long startMs = parseIsoToMs(req.startTime());
            Long endMs = parseIsoToMs(req.endTime());

            List<Candles> candles = market.candles(
                    req.symbol(),
                    req.interval(),
                    startMs,
                    endMs,
                    1000
            );

            engine.runBacktest(
                    req.symbol(),
                    req.interval(),
                    candles,
                    BigDecimal.valueOf(req.initialBalance()),
                    BigDecimal.valueOf(req.riskPct())
            );

        } else {
            engine.startLive(
                    req.symbol(),
                    req.interval(),
                    BigDecimal.valueOf(req.initialBalance())
            );

            liveRunner.setRiskPct(BigDecimal.valueOf(req.riskPct()));
        }
    }

    private static Long parseIsoToMs(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try {
            return OffsetDateTime.parse(iso).toInstant().toEpochMilli();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Invalid datetime: " + iso + " (expected e.g. 2026-01-01T00:00:00Z)"
            );
        }
    }
}
