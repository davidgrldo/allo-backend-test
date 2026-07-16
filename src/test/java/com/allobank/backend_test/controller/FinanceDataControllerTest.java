package com.allobank.backend_test.controller;

import com.allobank.backend_test.service.IDRDataFetcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceDataControllerTest {

    @Mock
    private IDRDataFetcher latestFetcher;

    @Mock
    private IDRDataFetcher historicalFetcher;

    @Mock
    private IDRDataFetcher currenciesFetcher;

    private FinanceDataController controller;

    @BeforeEach
    void setUp() {
        Map<String, IDRDataFetcher> fetchers = Map.of(
                "latest_idr_rates", latestFetcher,
                "historical_idr_usd", historicalFetcher,
                "supported_currencies", currenciesFetcher
        );
        controller = new FinanceDataController(fetchers);
    }

    @Test
    void shouldReturn200ForKnownResourceType() {
        List<String> mockData = List.of("test");
        when(latestFetcher.getData()).thenReturn(mockData);

        ResponseEntity<?> response = controller.getData("latest_idr_rates");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockData, response.getBody());
    }

    @Test
    void shouldReturn400ForUnknownResourceType() {
        ResponseEntity<?> response = controller.getData("unknown_type");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertNotNull(body);
        assertTrue(body.get("error").contains("Unknown resource type"));
    }

    @Test
    void shouldReturn503WhenDataNotLoaded() {
        when(currenciesFetcher.getData()).thenReturn(null);

        ResponseEntity<?> response = controller.getData("supported_currencies");

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertNotNull(body);
        assertTrue(body.get("error").contains("currently unavailable"));
    }

    @Test
    void shouldReturn200ForHistoricalIdrUsd() {
        List<String> mockData = List.of("historical");
        when(historicalFetcher.getData()).thenReturn(mockData);

        ResponseEntity<?> response = controller.getData("historical_idr_usd");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void shouldReturn200ForSupportedCurrencies() {
        List<String> mockData = List.of("currencies");
        when(currenciesFetcher.getData()).thenReturn(mockData);

        ResponseEntity<?> response = controller.getData("supported_currencies");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
