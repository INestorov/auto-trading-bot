package com.backend.services;

import com.backend.dto.BotMode;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Component
public class LiveBotRunner {

    private final BotEngine engine;
    private final MarketDataService market;

    // Live params
    private volatile BigDecimal riskPct = new BigDecimal("0.10");

    public LiveBotRunner(BotEngine engine, MarketDataService market) {
        this.engine = engine;
        this.market = market;
    }

    public void setRiskPct(BigDecimal riskPct) {
        if (riskPct == null) throw new IllegalArgumentException("riskPct cannot be null");
        if (riskPct.compareTo(BigDecimal.ZERO) <= 0 || riskPct.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("riskPct must be in (0, 1]");
        }
        this.riskPct = riskPct;
    }

    // Poll every 5 seconds
    @Scheduled(fixedDelay = 5000)
    public void tick() {
        if (!engine.isRunning() || engine.getMode() != BotMode.LIVE) return;

        BigDecimal price = market.latestPrice(engine.getSymbol());

        engine.processLiveTick(price, OffsetDateTime.now(), riskPct);
    }
}