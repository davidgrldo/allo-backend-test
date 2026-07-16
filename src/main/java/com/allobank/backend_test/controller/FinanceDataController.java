package com.allobank.backend_test.controller;

import com.allobank.backend_test.service.IDRDataFetcher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/finance/data")
public class FinanceDataController {

    private final Map<String, IDRDataFetcher> fetchers;

    public FinanceDataController(Map<String, IDRDataFetcher> fetchers) {
        this.fetchers = fetchers;
    }

    @GetMapping("/{resourceType}")
    public ResponseEntity<?> getData(@PathVariable String resourceType) {
        IDRDataFetcher fetcher = fetchers.get(resourceType);
        if (fetcher == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Unknown resource type: " + resourceType));
        }
        Object data = fetcher.getData();
        if (data == null) {
            return ResponseEntity.status(503)
                    .body(Map.of("error", "Resource '" + resourceType + "' is currently unavailable. The data source did not respond at startup."));
        }
        return ResponseEntity.ok(data);
    }
}
