package com.allobank.backend_test.service;

import com.allobank.backend_test.config.FrankfurterProperties;
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
    private final FrankfurterProperties properties;

    public LatestIdrRatesFetcher(RestTemplate restTemplate, DataStore dataStore, FrankfurterProperties properties) {
        this.restTemplate = restTemplate;
        this.dataStore = dataStore;
        this.properties = properties;
    }

    @Override
    public void fetchFromExternal() {
        String baseCurrency = properties.getBaseCurrency();
        log.info("Fetching latest {} rates from Frankfurter", baseCurrency);
        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.getForObject(
                "/latest?base={base}", Map.class, baseCurrency);
        if (response != null && response.containsKey("rates")) {
            response = new LinkedHashMap<>(response);
            computeUsdBuySpread(response);
        }
        dataStore.store("latest_idr_rates", response != null ? List.of(response) : null);
    }

    @SuppressWarnings("unchecked")
    private void computeUsdBuySpread(Map<String, Object> response) {
        String targetCurrency = properties.getTargetCurrency();
        Map<String, Object> rates = (Map<String, Object>) response.get("rates");
        if (rates == null) return;
        Object targetRateObj = rates.get(targetCurrency);
        if (targetRateObj instanceof Number targetRate) {
            BigDecimal rateTarget = BigDecimal.valueOf(targetRate.doubleValue());
            if (rateTarget.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal buySpread = BigDecimal.ONE.divide(rateTarget, 10, RoundingMode.HALF_UP)
                        .multiply(SPREAD_MULTIPLIER);
                Map<String, Object> mutableRates = new LinkedHashMap<>(rates);
                String spreadField = targetCurrency + "_BuySpread_" + properties.getBaseCurrency();
                mutableRates.put(spreadField, buySpread);
                response.put("rates", mutableRates);
            }
        }
    }

    @Override
    public Object getData() {
        return dataStore.get("latest_idr_rates");
    }
}
