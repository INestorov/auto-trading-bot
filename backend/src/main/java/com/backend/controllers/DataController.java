package com.backend.controllers;

import com.backend.dto.SnapshotDTO;
import com.backend.dto.TradeDTO;
import com.backend.models.Candles;
import com.backend.logic.MarketDataService;
import com.backend.repositories.SnapshotRepository;
import com.backend.repositories.TradeRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class DataController {

    private final TradeRepository trades;
    private final SnapshotRepository snapshots;
    private final MarketDataService market;

    public DataController(TradeRepository trades, SnapshotRepository snapshots, MarketDataService market) {
        this.trades = trades;
        this.snapshots = snapshots;
        this.market = market;
    }

    @GetMapping("/trades")
    public List<TradeDTO> trades(@RequestParam String mode, @RequestParam String symbol,
                                 @RequestParam(defaultValue = "500") int limit) {
        return trades.listTrades(mode, symbol, limit);
    }

    @GetMapping("/portfolio/snapshots")
    public List<SnapshotDTO> snapshots(@RequestParam String mode, @RequestParam String symbol,
                                       @RequestParam(defaultValue = "2000") int limit) {
        return snapshots.list(mode, symbol, limit);
    }

    @GetMapping("/market/candles")
    public List<Candles> candles(@RequestParam String symbol, @RequestParam String interval,
                                    @RequestParam(required = false) Long startMs,
                                    @RequestParam(required = false) Long endMs,
                                    @RequestParam(defaultValue = "500") int limit) {
        return market.candles(symbol, interval, startMs, endMs, Math.min(limit, 1000));
    }
}