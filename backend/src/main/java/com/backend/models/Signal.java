package com.backend.models;


import java.math.BigDecimal;

public record Signal(BigDecimal fast, BigDecimal slow, BigDecimal rsi, boolean crossUp, boolean crossDn) {}
