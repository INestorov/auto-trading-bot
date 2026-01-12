package com.backend.models;


import java.math.BigDecimal;

public record Signal(BigDecimal fast, BigDecimal slow, BigDecimal relativeStrengthIndex, boolean crossUp, boolean crossDn) {}
