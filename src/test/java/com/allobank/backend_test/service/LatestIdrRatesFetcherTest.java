package com.allobank.backend_test.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LatestIdrRatesFetcherTest {

    @Mock
    private RestTemplate restTemplate;

    private DataStore dataStore;
    private LatestIdrRatesFetcher fetcher;

    @BeforeEach
    void setUp() {
        dataStore = new DataStore();
        fetcher = new LatestIdrRatesFetcher(restTemplate, dataStore);
    }

    @Test
    void shouldComputeUsdBuySpreadCorrectly() {
        Map<String, Object> mockResponse = Map.of(
                "base", "IDR",
                "rates", Map.of("USD", 0.000064)
        );
        when(restTemplate.getForObject(eq("/latest?base=IDR"), eq(Map.class)))
                .thenReturn(mockResponse);

        fetcher.fetchFromExternal();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) dataStore.get("latest_idr_rates");
        assertNotNull(result);
        assertEquals(1, result.size());
        @SuppressWarnings("unchecked")
        Map<String, Object> rates = (Map<String, Object>) result.get(0).get("rates");
        assertTrue(rates.containsKey("USD_BuySpread_IDR"));

        BigDecimal expected = BigDecimal.ONE.divide(new BigDecimal("0.000064"), 10, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("1.005"));
        BigDecimal actual = new BigDecimal(rates.get("USD_BuySpread_IDR").toString());
        assertEquals(0, expected.compareTo(actual));
    }

    @Test
    void shouldHandleMissingUsdRate() {
        Map<String, Object> mockResponse = Map.of(
                "base", "IDR",
                "rates", Map.of("EUR", 0.000059)
        );
        when(restTemplate.getForObject(eq("/latest?base=IDR"), eq(Map.class)))
                .thenReturn(mockResponse);

        fetcher.fetchFromExternal();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) dataStore.get("latest_idr_rates");
        assertNotNull(result);
        @SuppressWarnings("unchecked")
        Map<String, Object> rates = (Map<String, Object>) result.get(0).get("rates");
        assertFalse(rates.containsKey("USD_BuySpread_IDR"));
    }

    @Test
    void shouldHandleZeroUsdRate() {
        Map<String, Object> mockResponse = Map.of(
                "base", "IDR",
                "rates", Map.of("USD", 0.0)
        );
        when(restTemplate.getForObject(eq("/latest?base=IDR"), eq(Map.class)))
                .thenReturn(mockResponse);

        fetcher.fetchFromExternal();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) dataStore.get("latest_idr_rates");
        assertNotNull(result);
        @SuppressWarnings("unchecked")
        Map<String, Object> rates = (Map<String, Object>) result.get(0).get("rates");
        assertFalse(rates.containsKey("USD_BuySpread_IDR"));
    }

    @Test
    void shouldReturnListWithOneElement() {
        Map<String, Object> mockResponse = Map.of("base", "IDR", "rates", Map.of("USD", 0.000064));
        when(restTemplate.getForObject(eq("/latest?base=IDR"), eq(Map.class)))
                .thenReturn(mockResponse);

        fetcher.fetchFromExternal();

        Object data = dataStore.get("latest_idr_rates");
        assertInstanceOf(List.class, data);
        assertEquals(1, ((List<?>) data).size());
    }
}
