package com.backend.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StartBotRequest(
        @NotNull BotMode mode,
        @NotBlank String symbol,
        @NotBlank String interval,
        String startTime,
        String endTime,
        @NotNull Double initialBalance,
        @NotNull Double riskPct
) {}