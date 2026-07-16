package com.allobank.backend_test.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component("supported_currencies")
public class SupportedCurrenciesFetcher implements IDRDataFetcher {

    private static final Logger log = LoggerFactory.getLogger(SupportedCurrenciesFetcher.class);

    private final RestTemplate restTemplate;
    private final DataStore dataStore;

    public SupportedCurrenciesFetcher(RestTemplate restTemplate, DataStore dataStore) {
        this.restTemplate = restTemplate;
        this.dataStore = dataStore;
    }

    @Override
    public void fetchFromExternal() {
        log.info("Fetching supported currencies from Frankfurter");
        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.getForObject("/currencies", Map.class);
        dataStore.store("supported_currencies", response != null ? List.of(response) : null);
    }

    @Override
    public Object getData() {
        return dataStore.get("supported_currencies");
    }
}
