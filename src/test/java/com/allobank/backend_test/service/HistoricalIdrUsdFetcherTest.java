package com.allobank.backend_test.service;

import com.allobank.backend_test.config.FrankfurterProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistoricalIdrUsdFetcherTest {

    @Mock
    private RestTemplate restTemplate;

    private DataStore dataStore;
    private FrankfurterProperties properties;
    private HistoricalIdrUsdFetcher fetcher;

    @BeforeEach
    void setUp() {
        dataStore = new DataStore();
        properties = new FrankfurterProperties();
        fetcher = new HistoricalIdrUsdFetcher(restTemplate, dataStore, properties);
    }

    @Test
    void shouldFetchWithConfiguredDateRange() {
        Map<String, Object> mockResponse = Map.of(
                "start_date", "2024-01-01",
                "end_date", "2024-01-05",
                "rates", Map.of("2024-01-01", Map.of("USD", 0.000064))
        );
        String dateRange = properties.getHistorical().getDateRange();
        when(restTemplate.getForObject(eq("/{dateRange}?from=IDR&to=USD"), eq(Map.class), eq(dateRange)))
                .thenReturn(mockResponse);

        fetcher.fetchFromExternal();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) dataStore.get("historical_idr_usd");
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("2024-01-01", result.get(0).get("start_date"));
    }

    @Test
    void shouldReturnListWrappedResponse() {
        Map<String, Object> mockResponse = Map.of("rates", Map.of());
        String dateRange = properties.getHistorical().getDateRange();
        when(restTemplate.getForObject(eq("/{dateRange}?from=IDR&to=USD"), eq(Map.class), eq(dateRange)))
                .thenReturn(mockResponse);

        fetcher.fetchFromExternal();

        Object data = dataStore.get("historical_idr_usd");
        assertInstanceOf(List.class, data);
    }
}
