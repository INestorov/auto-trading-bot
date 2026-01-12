package com.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record SnapshotDTO(
        long id,
        String mode,
        String symbol,
        OffsetDateTime purchasedAt,
        BigDecimal cashBalance,
        BigDecimal positionQty,
        BigDecimal positionValue,
        BigDecimal totalValue
) {}
