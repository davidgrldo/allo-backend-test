package com.allobank.backend_test;

import com.allobank.backend_test.service.DataStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "frankfurter.base-url=http://localhost:0",
        "frankfurter.historical.date-range=2024-01-01..2024-01-02"
})
class DataInitializationRunnerTest {

    @Autowired
    private DataStore dataStore;

    @TestConfiguration
    static class MockRestTemplateConfig {

        @Bean
        @Primary
        public RestTemplate restTemplate() {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(java.time.Duration.ofMillis(100));
            factory.setReadTimeout(java.time.Duration.ofMillis(100));
            return new RestTemplate(factory);
        }
    }

    @Test
    void contextLoadsSuccessfully() {
        assertNotNull(dataStore);
    }

    @Test
    void applicationStartsEvenWhenExternalCallsFail() {
        Object latest = dataStore.get("latest_idr_rates");
        Object historical = dataStore.get("historical_idr_usd");
        Object currencies = dataStore.get("supported_currencies");

        boolean allNull = latest == null && historical == null && currencies == null;
        assertTrue(allNull || (latest == null || historical == null || currencies == null),
                "At least one resource should have failed to load (mock server not running)");
    }
}
