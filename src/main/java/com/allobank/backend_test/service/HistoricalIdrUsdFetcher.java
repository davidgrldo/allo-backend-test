package com.allobank.backend_test.service;

import com.allobank.backend_test.config.FrankfurterProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component("historical_idr_usd")
public class HistoricalIdrUsdFetcher implements IDRDataFetcher {

    private static final Logger log = LoggerFactory.getLogger(HistoricalIdrUsdFetcher.class);

    private final RestTemplate restTemplate;
    private final DataStore dataStore;
    private final FrankfurterProperties properties;

    public HistoricalIdrUsdFetcher(RestTemplate restTemplate, DataStore dataStore, FrankfurterProperties properties) {
        this.restTemplate = restTemplate;
        this.dataStore = dataStore;
        this.properties = properties;
    }

    @Override
    public void fetchFromExternal() {
        String dateRange = properties.getHistorical().getDateRange();
        String baseCurrency = properties.getBaseCurrency();
        String targetCurrency = properties.getTargetCurrency();
        log.info("Fetching historical {}-{} rates for date range: {}", baseCurrency, targetCurrency, dateRange);
        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.getForObject(
                "/{dateRange}?from={from}&to={to}", Map.class, dateRange, baseCurrency, targetCurrency);
        dataStore.store("historical_idr_usd", response != null ? List.of(response) : null);
    }

    @Override
    public Object getData() {
        return dataStore.get("historical_idr_usd");
    }
}
