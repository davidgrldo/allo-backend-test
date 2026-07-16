package com.allobank.backend_test.service;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
public class DataStore {

    private volatile Map<String, Object> store = Collections.emptyMap();

    public void store(String resourceType, Object data) {
        Map<String, Object> copy = new HashMap<>(store);
        copy.put(resourceType, data);
        store = Map.copyOf(copy);
    }

    public Object get(String resourceType) {
        return store.get(resourceType);
    }
}
