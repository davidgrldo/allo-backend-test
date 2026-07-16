package com.allobank.backend_test.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DataInitializationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializationRunner.class);

    private final Map<String, IDRDataFetcher> fetchers;

    public DataInitializationRunner(Map<String, IDRDataFetcher> fetchers) {
        this.fetchers = fetchers;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Starting data initialization for {} resources", fetchers.size());
        fetchers.forEach((resourceType, fetcher) -> {
            try {
                fetcher.fetchFromExternal();
                log.info("Successfully loaded data for resource: {}", resourceType);
            } catch (Exception e) {
                log.warn("Failed to load data for resource '{}': {}", resourceType, e.getMessage());
            }
        });
        log.info("Data initialization complete");
    }
}
