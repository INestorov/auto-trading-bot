package com.backend.services;

import com.backend.dto.BotMode;
import com.backend.models.Candles;
import com.backend.models.Signal;
import com.backend.repository.AccountRepository;
import com.backend.repository.PositionRepository;
import com.backend.repository.SnapshotRepository;
import com.backend.repository.TradeRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class BotEngine {

    private final AccountRepository accounts;
    private final PositionRepository positions;
    private final TradeRepository trades;
    private final SnapshotRepository snapshots;

    // session status (simple single-session design)
    private volatile boolean running = false;
    private volatile BotMode mode = BotMode.TRAIN;
    private volatile String symbol = "BTCUSDT";
    private volatile String interval = "1m";

    // Strategy params
    private static final int FAST = 12;
    private static final int SLOW = 26;
    private static final int RSI_PERIOD = 14;
    private static final BigDecimal FEE_RATE = new BigDecimal("0.001"); // 0.1%

    // Indicator state (used for LIVE; backtest uses local lists)
    private List<BigDecimal> liveCloses = new ArrayList<>();
    private BigDecimal livePrevFast = null;
    private BigDecimal livePrevSlow = null;

    public BotEngine(AccountRepository accounts,
                     PositionRepository positions,
                     TradeRepository trades,
                     SnapshotRepository snapshots) {
        this.accounts = accounts;
        this.positions = positions;
        this.trades = trades;
        this.snapshots = snapshots;
    }

    public boolean isRunning() { return running; }
    public BotMode getMode() { return mode; }
    public String getSymbol() { return symbol; }
    public String getInterval() { return interval; }

    public void stop() { this.running = false; }

    public void reset(BotMode mode, String symbol) {
        long accountId = accounts.getDefaultAccountId();
        trades.deleteByModeAndSymbol(accountId, mode.name(), symbol);
        snapshots.deleteByModeAndSymbol(accountId, mode.name(), symbol);
        positions.upsertPosition(accountId, symbol, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public void runBacktest(String symbol,
                            String interval,
                            List<Candles> candles,
                            BigDecimal initialBalance,
                            BigDecimal riskPct) {

        long accountId = accounts.getDefaultAccountId();
        initSession(BotMode.TRAIN, symbol, interval, accountId, initialBalance);

        List<BigDecimal> closes = new ArrayList<>();
        BigDecimal prevFast = null;
        BigDecimal prevSlow = null;

        for (Candles c : candles) {
            if (!running) break;

            appendCloseAndTrim(closes, c.close());

            if (!hasEnoughData(closes)) {
                snapshot(accountId, BotMode.TRAIN, symbol, c.openTime(), c.close());
                continue;
            }

            Signal signal = computeSignal(closes, prevFast, prevSlow);
            applySignal(accountId, BotMode.TRAIN, symbol, c.close(), c.openTime(), riskPct, signal);

            snapshot(accountId, BotMode.TRAIN, symbol, c.openTime(), c.close());

            prevFast = signal.fast();
            prevSlow = signal.slow();
        }

        this.running = false;
    }

    public void startLive(String symbol, String interval, BigDecimal initialBalance) {
        long accountId = accounts.getDefaultAccountId();
        initSession(BotMode.LIVE, symbol, interval, accountId, initialBalance);
        resetLiveIndicatorState();
    }

    public void processLiveTick(BigDecimal price, OffsetDateTime ts, BigDecimal riskPct) {
        if (!running || mode != BotMode.LIVE) return;

        long accountId = accounts.getDefaultAccountId();

        appendCloseAndTrim(liveCloses, price);

        if (!hasEnoughData(liveCloses)) {
            snapshot(accountId, BotMode.LIVE, symbol, ts, price);
            return;
        }

        Signal signal = computeSignal(liveCloses, livePrevFast, livePrevSlow);
        applySignal(accountId, BotMode.LIVE, symbol, price, ts, riskPct, signal);

        snapshot(accountId, BotMode.LIVE, symbol, ts, price);

        livePrevFast = signal.fast();
        livePrevSlow = signal.slow();
    }

    private void initSession(BotMode mode,
                             String symbol,
                             String interval,
                             long accountId,
                             BigDecimal initialBalance) {
        accounts.setCash(accountId, initialBalance);
        positions.upsertPosition(accountId, symbol, BigDecimal.ZERO, BigDecimal.ZERO);

        this.mode = mode;
        this.symbol = symbol;
        this.interval = interval;
        this.running = true;
    }

    private void resetLiveIndicatorState() {
        this.liveCloses = new ArrayList<>();
        this.livePrevFast = null;
        this.livePrevSlow = null;
    }

    private boolean hasEnoughData(List<BigDecimal> closes) {
        return closes.size() >= SLOW + 2;
    }

    private void appendCloseAndTrim(List<BigDecimal> closes, BigDecimal close) {
        closes.add(close);

        // Keep the window from growing forever (works for LIVE and backtest)
        if (closes.size() > 2000) {
            closes.subList(0, closes.size() - 500).clear();
        }
    }

    private Signal computeSignal(List<BigDecimal> closes, BigDecimal prevFast, BigDecimal prevSlow) {
        BigDecimal fast = TradingStrategy.simpleMovingAverage(closes.subList(closes.size() - FAST, closes.size()));
        BigDecimal slow = TradingStrategy.simpleMovingAverage(closes.subList(closes.size() - SLOW, closes.size()));
        BigDecimal relativeStrengthIndex  = TradingStrategy.relativeStrengthIndex(closes, RSI_PERIOD);

        boolean crossUp = prevFast != null && prevSlow != null
                && prevFast.compareTo(prevSlow) <= 0 && fast.compareTo(slow) > 0;

        boolean crossDn = prevFast != null && prevSlow != null
                && prevFast.compareTo(prevSlow) >= 0 && fast.compareTo(slow) < 0;

        return new Signal(fast, slow, relativeStrengthIndex, crossUp, crossDn);
    }

    private void applySignal(long accountId,
                             BotMode mode,
                             String symbol,
                             BigDecimal price,
                             OffsetDateTime ts,
                             BigDecimal riskPct,
                             Signal signal) {

        var pos = positions.getPosition(accountId, symbol);
        boolean hasPosition = pos.quantity().compareTo(BigDecimal.ZERO) > 0;

        boolean buyOk = signal.crossUp() && signal.relativeStrengthIndex().compareTo(new BigDecimal("70")) < 0;
        boolean sellOk = signal.crossDn() || signal.relativeStrengthIndex().compareTo(new BigDecimal("75")) > 0;

        if (!hasPosition && buyOk) {
            tryBuy(accountId, mode, symbol, price, ts, riskPct);
        } else if (hasPosition && sellOk) {
            trySellAll(accountId, mode, symbol, price, ts);
        }
    }

    private void tryBuy(long accountId, BotMode mode, String symbol, BigDecimal price, OffsetDateTime ts, BigDecimal riskPct) {
        BigDecimal cash = accounts.getCash(accountId);
        BigDecimal spend = cash.multiply(riskPct).setScale(8, RoundingMode.HALF_UP);
        if (spend.compareTo(BigDecimal.ZERO) <= 0) return;

        BigDecimal quantity = spend.divide(price, 8, RoundingMode.HALF_UP);
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) return;

        BigDecimal fee = spend.multiply(FEE_RATE).setScale(8, RoundingMode.HALF_UP);
        BigDecimal totalCost = spend.add(fee);

        if (cash.compareTo(totalCost) < 0) return;

        var pos = positions.getPosition(accountId, symbol);
        BigDecimal newQuantity = pos.quantity().add(quantity);

        BigDecimal newAvg = (pos.avgEntry().multiply(pos.quantity()).add(price.multiply(quantity)))
                .divide(newQuantity, 8, RoundingMode.HALF_UP);

        accounts.setCash(accountId, cash.subtract(totalCost));
        positions.upsertPosition(accountId, symbol, newQuantity, newAvg);

        trades.insertTrade(accountId, mode.name(), symbol, "BUY", quantity, price, fee, BigDecimal.ZERO, ts);
    }

    private void trySellAll(long accountId, BotMode mode, String symbol, BigDecimal price, OffsetDateTime ts) {
        var pos = positions.getPosition(accountId, symbol);
        if (pos.quantity().compareTo(BigDecimal.ZERO) <= 0) return;

        BigDecimal proceeds = pos.quantity().multiply(price).setScale(8, RoundingMode.HALF_UP);
        BigDecimal fee = proceeds.multiply(FEE_RATE).setScale(8, RoundingMode.HALF_UP);

        BigDecimal realized = (price.subtract(pos.avgEntry()))
                .multiply(pos.quantity())
                .setScale(8, RoundingMode.HALF_UP);

        BigDecimal cash = accounts.getCash(accountId);
        accounts.setCash(accountId, cash.add(proceeds.subtract(fee)));

        positions.upsertPosition(accountId, symbol, BigDecimal.ZERO, BigDecimal.ZERO);

        trades.insertTrade(accountId, mode.name(), symbol, "SELL", pos.quantity(), price, fee, realized, ts);
    }

    private void snapshot(long accountId, BotMode mode, String symbol, OffsetDateTime ts, BigDecimal price) {
        BigDecimal cash = accounts.getCash(accountId);
        var pos = positions.getPosition(accountId, symbol);

        BigDecimal posValue = pos.quantity().multiply(price).setScale(8, RoundingMode.HALF_UP);
        BigDecimal total = cash.add(posValue).setScale(8, RoundingMode.HALF_UP);

        snapshots.insertSnapshot(accountId, mode.name(), symbol, ts, cash, pos.quantity(), posValue, total);
    }
}
