package com.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TradeDTO(
        long id,
        String mode,
        String symbol,
        String side, // Buy, Sell
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal fee,
        BigDecimal realizedPnl,
        OffsetDateTime purchasedAt
) {}