package com.backend.models;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record Candles(
        OffsetDateTime openTime,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        BigDecimal volume
) {}