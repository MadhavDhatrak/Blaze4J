package com.github.madhavdhatrak.blaze4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe schema registry for JSON schema references
 */
public class SchemaRegistry {
    private final Map<String, String> schemas = new ConcurrentHashMap<>();
    
    public void register(String uri, String schemaJson) {
        if (uri == null || schemaJson == null) {
            throw new IllegalArgumentException("uri and schemaJson must not be null");
        }
        schemas.put(uri, schemaJson);
    }
    
    public void unregister(String uri) {
        schemas.remove(uri);
    }
    
    public void clear() {
        schemas.clear();
    }
    
    public boolean contains(String uri) {
        return schemas.containsKey(uri);
    }
    
    public String resolve(String uri) {
        return schemas.get(uri);
    }
}
