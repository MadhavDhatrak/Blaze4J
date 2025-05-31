package com.sourcemeta.blaze.draft2020;

import java.io.IOException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Json {
    private static final ObjectMapper mapper = new ObjectMapper();
    
    public static ObjectMapper mapper() {
        return mapper;
    }
    
    public static JsonNode parse(String json) throws IOException {
        return mapper.readTree(json);
    }
} 