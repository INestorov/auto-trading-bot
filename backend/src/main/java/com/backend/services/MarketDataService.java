package com.backend.services;

import com.backend.models.Candles;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
public class MarketDataService {
    private final RestClient client = RestClient.create("https://api.binance.com");

    public BigDecimal latestPrice(String symbol) {
        var res = client.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v3/ticker/price").queryParam("symbol", symbol).build())
                .retrieve()
                .body(PriceResponse.class);
        if (res == null) throw new IllegalStateException("No price response");
        return new BigDecimal(res.price());
    }

    public List<Candles> candles(String symbol, String interval, Long startMs, Long endMs, int limit) {
        var body = client.get()
                .uri(uriBuilder -> {
                    var b = uriBuilder.path("/api/v3/klines")
                            .queryParam("symbol", symbol)
                            .queryParam("interval", interval)
                            .queryParam("limit", limit);
                    if (startMs != null) b = b.queryParam("startTime", startMs);
                    if (endMs != null) b = b.queryParam("endTime", endMs);
                    return b.build();
                })
                .retrieve()
                .body(Object[][].class);

        if (body == null) return List.of();

        List<Candles> out = new ArrayList<>(body.length);
        for (Object[] k : body) {
            long openTime = ((Number) k[0]).longValue();
            BigDecimal openPrice = new BigDecimal((String) k[1]);
            BigDecimal high = new BigDecimal((String) k[2]);
            BigDecimal low  = new BigDecimal((String) k[3]);
            BigDecimal closePrice = new BigDecimal((String) k[4]);
            BigDecimal vol  = new BigDecimal((String) k[5]);
            out.add(new Candles(
                    OffsetDateTime.ofInstant(Instant.ofEpochMilli(openTime), ZoneOffset.UTC),
                    openPrice, high, low, closePrice, vol
            ));
        }
        return out;
    }

    public record PriceResponse(String symbol, String price) {}
}
