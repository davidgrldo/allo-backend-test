package com.allobank.backend_test.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component("latest_idr_rates")
public class LatestIdrRatesFetcher implements IDRDataFetcher {

    private static final Logger log = LoggerFactory.getLogger(LatestIdrRatesFetcher.class);
    private static final BigDecimal SPREAD_MULTIPLIER = new BigDecimal("1.005");

    private final RestTemplate restTemplate;
    private final DataStore dataStore;

    public LatestIdrRatesFetcher(RestTemplate restTemplate, DataStore dataStore) {
        this.restTemplate = restTemplate;
        this.dataStore = dataStore;
    }

    @Override
    public void fetchFromExternal() {
        log.info("Fetching latest IDR rates from Frankfurter");
        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.getForObject("/latest?base=IDR", Map.class);
        if (response != null && response.containsKey("rates")) {
            response = new LinkedHashMap<>(response);
            computeUsdBuySpread(response);
        }
        dataStore.store("latest_idr_rates", response != null ? List.of(response) : null);
    }

    @SuppressWarnings("unchecked")
    private void computeUsdBuySpread(Map<String, Object> response) {
        Map<String, Object> rates = (Map<String, Object>) response.get("rates");
        if (rates == null) return;
        Object usdRateObj = rates.get("USD");
        if (usdRateObj instanceof Number usdRate) {
            BigDecimal rateUsd = BigDecimal.valueOf(usdRate.doubleValue());
            if (rateUsd.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal usdBuySpread = BigDecimal.ONE.divide(rateUsd, 10, RoundingMode.HALF_UP)
                        .multiply(SPREAD_MULTIPLIER);
                Map<String, Object> mutableRates = new LinkedHashMap<>(rates);
                mutableRates.put("USD_BuySpread_IDR", usdBuySpread);
                response.put("rates", mutableRates);
            }
        }
    }

    @Override
    public Object getData() {
        return dataStore.get("latest_idr_rates");
    }
}
