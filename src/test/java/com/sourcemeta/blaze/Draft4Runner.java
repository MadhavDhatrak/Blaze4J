package com.sourcemeta.blaze;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.Arena;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test runner for JSON Schema Draft 4 test suite
 */
@TestInstance(Lifecycle.PER_CLASS)
public class Draft4Runner {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private HttpServer server;
    
    // Test statistics counters
    private final AtomicInteger totalTests = new AtomicInteger(0);
    private final AtomicInteger passedTests = new AtomicInteger(0);
    private final AtomicInteger failedTests = new AtomicInteger(0);
    private final AtomicInteger skippedTests = new AtomicInteger(0);
    
    private static final List<String> TEST_FILES = Arrays.asList(
        "additionalItems.json",
        "additionalProperties.json",
        "allOf.json",
        "anyOf.json",
        "default.json",
        "definitions.json",
        "dependencies.json",
        "enum.json",
        "format.json",
        "infinite-loop-detection.json",
        "items.json",
        "maximum.json",
        "maxItems.json",
        "maxLength.json",
        "maxProperties.json",
        "minimum.json",
        "minItems.json",
        "minLength.json",
        "minProperties.json",
        "multipleOf.json",
        "not.json",
        "oneOf.json",
        "pattern.json",
        "patternProperties.json",
        "properties.json",
        "ref.json",
        "refRemote.json",
        "required.json",
        "type.json",
        "uniqueItems.json"
    );
    
    @BeforeAll
    public void setup() throws IOException {
        // Start HTTP server on port 1234
        server = HttpServer.create(new InetSocketAddress(1234), 0);
        server.createContext("/", new SchemaHandler());
        server.start();
        System.out.println("Mock server started on port 1234 for Draft 4 tests");
    }
    
    @AfterAll
    public void cleanup() {
        if (server != null) {
            server.stop(0);
            System.out.println("Mock server stopped for Draft 4 tests");
        }
    }
    
    /**
     * SchemaHandler implementation to serve test schemas for Draft 4
     */
    static class SchemaHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            System.out.println("Received request for: " + path);
            
            // Try to find the schema in resources directory
            List<String> possiblePaths = Arrays.asList(
                "JSON-Schema-Test-Suite/remotes" + path,
                "JSON-Schema-Test-Suite/remotes/draft4" + path,
                "JSON-Schema-Test-Suite/remotes/draft4" + path.replace("/draft4", "")
            );
            
            boolean found = false;
            for (String possiblePath : possiblePaths) {
                try (InputStream resourceStream = Draft4Runner.class.getClassLoader().getResourceAsStream(possiblePath)) {
                    if (resourceStream != null) {
                        String fileContent = new String(resourceStream.readAllBytes(), StandardCharsets.UTF_8);
                        
                        // Add $schema keyword if not present
                        try {
                            JsonNode schemaNode = MAPPER.readTree(fileContent);
                            if (!schemaNode.has("$schema") && schemaNode.isObject()) {
                                ObjectNode modifiedSchema = (ObjectNode) schemaNode;
                                modifiedSchema.put("$schema", "http://json-schema.org/draft-04/schema#");
                                fileContent = MAPPER.writeValueAsString(modifiedSchema);
                            }
                            
                            exchange.getResponseHeaders().set("Content-Type", "application/json");
                            byte[] responseBytes = fileContent.getBytes(StandardCharsets.UTF_8);
                            exchange.sendResponseHeaders(200, responseBytes.length);
                            exchange.getResponseBody().write(responseBytes);
                            found = true;
                            break;
                        } catch (JsonProcessingException e) {
                            System.err.println("Failed to process schema JSON: " + e.getMessage());
                        }
                    }
                }
            }
            
            if (!found) {
                System.err.println("Schema not found: " + path);
                exchange.sendResponseHeaders(404, -1);
            }
            
            exchange.close();
        }
    }
    
    /**
     * Load test cases from all specified JSON files in the test suite
     */
    static Stream<Arguments> refRemoteTestCases() throws IOException {
        List<Arguments> testCases = new ArrayList<>();
        
        for (String fileName : TEST_FILES) {
            String testSuitePath = "JSON-Schema-Test-Suite/tests/draft4/" + fileName;
            try (InputStream is = Draft4Runner.class.getClassLoader()
                    .getResourceAsStream(testSuitePath)) {
                
                if (is == null) {
                    System.err.println("Warning: Could not load " + fileName + " from resources");
                    continue;
                }
                
                String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                JsonNode testsArray = MAPPER.readTree(content);
                
                // Process test groups
                for (JsonNode testGroup : testsArray) {
                    String groupDescription = testGroup.get("description").asText();
                    JsonNode schema = testGroup.get("schema");
                    JsonNode tests = testGroup.get("tests");
                    
                    // Add $schema keyword if not present
                    if (schema.isObject() && !schema.has("$schema")) {
                        ObjectNode modifiedSchema = (ObjectNode) schema;
                        modifiedSchema.put("$schema", "http://json-schema.org/draft-04/schema#");
                        schema = modifiedSchema;
                    }
                    
                    // Process individual tests in each group
                    for (JsonNode test : tests) {
                        String testDescription = test.get("description").asText();
                        JsonNode data = test.get("data");
                        boolean expectedValid = test.get("valid").asBoolean();
                        
                        // Add as an argument for the parameterized test
                        testCases.add(Arguments.of(
                            fileName + " - " + groupDescription,
                            testDescription, 
                            MAPPER.writeValueAsString(schema),
                            MAPPER.writeValueAsString(data),
                            expectedValid
                        ));
                    }
                }
            }
        }
        
        return testCases.stream();
    }
    
    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("refRemoteTestCases")
    @DisplayName("Test cases from JSON Schema Test Suite (Draft 4)")
    public void testRefRemoteCases(String groupDescription, String testDescription, 
                                 String schemaJson, String dataJson, boolean expectedValid) {
        totalTests.incrementAndGet();
        System.out.println("\nTesting: " + groupDescription + " - " + testDescription);
        System.out.println("Schema: " + schemaJson);
        System.out.println("Data: " + dataJson);
        System.out.println("Expected valid: " + expectedValid);
        
        try (Arena arena = Arena.ofConfined()) {
            CompiledSchema schema = null;
            try {
                // Add $schema keyword if not present
                JsonNode schemaNode;
                try {
                    schemaNode = MAPPER.readTree(schemaJson);
                    if (schemaNode.isObject() && !schemaNode.has("$schema")) {
                        ObjectNode modifiedSchema = (ObjectNode) schemaNode;
                        modifiedSchema.put("$schema", "http://json-schema.org/draft-04/schema#");
                        schemaJson = MAPPER.writeValueAsString(modifiedSchema);
                    }
                } catch (JsonProcessingException e) {
                    skippedTests.incrementAndGet();
                    System.err.println("Failed to process schema JSON: " + e.getMessage());
                    System.err.println("Schema that caused error: " + schemaJson);
                    throw new RuntimeException("Failed to process schema JSON", e);
                }
                
                schema = Blaze.compile(schemaJson, arena);
                
                final BlazeValidator validator = new BlazeValidator();
                boolean result = validator.validate(schema, dataJson);
                
                assertEquals(expectedValid, result, 
                        groupDescription + " - " + testDescription);
                passedTests.incrementAndGet();
            } catch (AssertionError e) {
                failedTests.incrementAndGet();
                System.err.println("Test failed: " + groupDescription + " - " + testDescription);
                System.err.println("Error message: " + e.getMessage());
                System.err.println("Schema that caused error: " + schemaJson);
                throw e;
            } catch (Exception e) {
                skippedTests.incrementAndGet();
                System.err.println("Error in test: " + groupDescription + " - " + testDescription);
                System.err.println("Error message: " + e.getMessage());
                System.err.println("Schema that caused error: " + schemaJson);
                throw new RuntimeException("Unexpected error during schema compilation", e);
            } finally {
                if (schema != null) {
                    schema.close();
                }
            }
        }
    }

    @AfterAll
    public void printTestSummary() {
        System.out.println("\n=== Test Summary ===");
        System.out.println("Total Tests: " + totalTests.get());
        System.out.println("Passed: " + passedTests.get());
        System.out.println("Failed: " + failedTests.get());
        System.out.println("Skipped: " + skippedTests.get());
        
        double passRate = totalTests.get() > 0 
            ? (double) passedTests.get() / totalTests.get() * 100 
            : 0;
        System.out.printf("Pass Rate: %.2f%%\n", passRate);
        System.out.println("==================\n");
    }
} 