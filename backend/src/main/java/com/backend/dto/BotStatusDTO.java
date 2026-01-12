package com.backend.dto;

public record BotStatusDTO(boolean running,
                           BotMode mode,
                           String symbol,
                           String interval) {}