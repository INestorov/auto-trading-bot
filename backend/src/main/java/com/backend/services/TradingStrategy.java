package com.backend.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class TradingStrategy {
    public static BigDecimal simpleMovingAverage(List<BigDecimal> values) {
        if (values.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = BigDecimal.ZERO;
        for (var v : values) sum = sum.add(v);
        return sum.divide(BigDecimal.valueOf(values.size()), 8, RoundingMode.HALF_UP);
    }

    public static BigDecimal relativeStrengthIndex(List<BigDecimal> closes, int period) {
        if (closes.size() < period + 1) return BigDecimal.valueOf(50);

        BigDecimal gains = BigDecimal.ZERO;
        BigDecimal losses = BigDecimal.ZERO;

        for (int i = closes.size() - period; i < closes.size(); i++) {
            BigDecimal diff = closes.get(i).subtract(closes.get(i - 1));
            if (diff.signum() > 0) gains = gains.add(diff);
            else losses = losses.add(diff.abs());
        }

        BigDecimal avgGain = gains.divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP);
        BigDecimal avgLoss = losses.divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP);

        if (avgLoss.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.valueOf(100);

        BigDecimal rs = avgGain.divide(avgLoss, 8, RoundingMode.HALF_UP);
        BigDecimal relativeStrengthIndex = BigDecimal.valueOf(100).subtract(
                BigDecimal.valueOf(100).divide(BigDecimal.ONE.add(rs), 8, RoundingMode.HALF_UP)
        );
        return relativeStrengthIndex;
    }
}