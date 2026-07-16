package com.allobank.backend_test.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupportedCurrenciesFetcherTest {

    @Mock
    private RestTemplate restTemplate;

    private DataStore dataStore;
    private SupportedCurrenciesFetcher fetcher;

    @BeforeEach
    void setUp() {
        dataStore = new DataStore();
        fetcher = new SupportedCurrenciesFetcher(restTemplate, dataStore);
    }

    @Test
    void shouldFetchAndReturnCurrencies() {
        Map<String, Object> mockResponse = Map.of(
                "USD", "United States Dollar",
                "EUR", "Euro",
                "IDR", "Indonesian Rupiah"
        );
        when(restTemplate.getForObject(eq("/currencies"), eq(Map.class)))
                .thenReturn(mockResponse);

        fetcher.fetchFromExternal();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) dataStore.get("supported_currencies");
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).containsKey("USD"));
        assertTrue(result.get(0).containsKey("IDR"));
    }

    @Test
    void shouldReturnListWrappedResponse() {
        Map<String, Object> mockResponse = Map.of("USD", "United States Dollar");
        when(restTemplate.getForObject(eq("/currencies"), eq(Map.class)))
                .thenReturn(mockResponse);

        fetcher.fetchFromExternal();

        Object data = dataStore.get("supported_currencies");
        assertInstanceOf(List.class, data);
    }
}
