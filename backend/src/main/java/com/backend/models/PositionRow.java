package com.backend.models;

import java.math.BigDecimal;

public record PositionRow(BigDecimal quantity, BigDecimal avgEntry) {
}