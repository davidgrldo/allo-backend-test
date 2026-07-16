package com.allobank.backend_test.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DataStoreTest {

    @Test
    void shouldStoreAndRetrieveData() {
        DataStore store = new DataStore();
        List<String> data = List.of("test");
        store.store("test_resource", data);

        assertEquals(data, store.get("test_resource"));
    }

    @Test
    void shouldReturnNullForUnknownResourceType() {
        DataStore store = new DataStore();
        assertNull(store.get("nonexistent"));
    }

    @Test
    void storedDataShouldBeImmutable() {
        DataStore store = new DataStore();
        Map<String, String> data = Map.of("key", "value");
        store.store("resource", data);

        assertThrows(UnsupportedOperationException.class, () -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> stored = (Map<String, Object>) store.get("resource");
            stored.put("new_key", "new_value");
        });
    }

    @Test
    void shouldPreserveExistingDataOnNewStore() {
        DataStore store = new DataStore();
        store.store("a", "value_a");
        store.store("b", "value_b");

        assertEquals("value_a", store.get("a"));
        assertEquals("value_b", store.get("b"));
    }

    @Test
    void shouldUseVolatileReferenceForThreadSafety() throws Exception {
        var field = DataStore.class.getDeclaredField("store");
        field.setAccessible(true);
        assertTrue(java.lang.reflect.Modifier.isVolatile(field.getModifiers()),
                "DataStore.store must be volatile");
    }
}
