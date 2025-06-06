package com.sourcemeta.blaze;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.lang.foreign.Arena;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.net.URLDecoder;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(Lifecycle.PER_CLASS)
public class CustomResolverTest {

    private HttpServer mockServer;
    private static final int PORT = 1234;
    
    @BeforeAll
    public void setupServer() throws IOException {
        // Start the mock server to serve schemas from resources
        mockServer = HttpServer.create(new InetSocketAddress(PORT), 0);
        mockServer.createContext("/", new SchemaFileHandler());
        mockServer.setExecutor(Executors.newFixedThreadPool(2));
        mockServer.start();
        System.out.println("Mock schema server started on port " + PORT);
    }

    @AfterAll
    public void tearDownServer() {
        if (mockServer != null) {
            mockServer.stop(0);
            System.out.println("Mock schema server stopped");
        }
    }

    /**
     * Test validation with a custom resolver using a schema with remote reference
     */
    @Test
    public void testCustomResolverWithRemoteRef() {
        // Schema that references an integer schema on our mock server
        String schemaJson = "{\n" +
            "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" +
            "  \"$ref\": \"http://localhost:" + PORT + "/draft2020-12/subSchemas.json#/$defs/integer\"\n" +
            "}";

        try (Arena arena = Arena.ofConfined();
             CompiledSchema schema = Blaze.compile(schemaJson, arena)) {
            
            final BlazeValidator validator = new BlazeValidator();
            
            // Test valid integer
            boolean validResult = validator.validate(schema, "42");
            System.out.println("Validation result for integer 42: " + validResult);
            assertTrue(validResult, "Integer should be valid against integer schema");
            
            // Test invalid string
            boolean invalidResult = validator.validate(schema, "\"not an integer\"");
            System.out.println("Validation result for string: " + invalidResult);
            assertFalse(invalidResult, "String should be invalid against integer schema");
        }
    }
    
    
    /**
     * Test validation with fragment within remote ref
     * Based on the "fragment within remote ref" test in refRemote.json
     */
    @Test
    public void testFragmentWithinRemoteRef() {
        String schemaJson = "{\n" +
            "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" +
            "  \"$ref\": \"http://localhost:" + PORT + "/draft2020-12/subSchemas.json#/$defs/integer\"\n" +
            "}";

        try (Arena arena = Arena.ofConfined();
             CompiledSchema schema = Blaze.compile(schemaJson, arena)) {
            
            final BlazeValidator validator = new BlazeValidator();
            
            // Test valid integer
            boolean validResult = validator.validate(schema, "1");
            System.out.println("Validation result for integer 1 with fragment: " + validResult);
            assertTrue(validResult, "Integer should be valid against fragment reference");
            
            // Test invalid string
            boolean invalidResult = validator.validate(schema, "\"a\"");
            System.out.println("Validation result for string with fragment: " + invalidResult);
            assertFalse(invalidResult, "String should be invalid against fragment reference");
        }
    }
    
    /**
     * Test validation with anchor within remote ref
     * Based on the "anchor within remote ref" test in refRemote.json
     */
    @Test
    public void testAnchorWithinRemoteRef() {
        String schemaJson = "{\n" +
            "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" +
            "  \"$ref\": \"http://localhost:" + PORT + "/draft2020-12/locationIndependentIdentifier.json#foo\"\n" +
            "}";

        try (Arena arena = Arena.ofConfined();
             CompiledSchema schema = Blaze.compile(schemaJson, arena)) {
            
            final BlazeValidator validator = new BlazeValidator();
            
            // Test valid integer
            boolean validResult = validator.validate(schema, "1");
            System.out.println("Validation result for integer 1 with anchor: " + validResult);
            assertTrue(validResult, "Integer should be valid against anchor reference");
            
            // Test invalid string
            boolean invalidResult = validator.validate(schema, "\"a\"");
            System.out.println("Validation result for string with anchor: " + invalidResult);
            assertFalse(invalidResult, "String should be invalid against anchor reference");
        }
    }
    
    /**
     * Test validation with ref within remote ref
     * Based on the "ref within remote ref" test in refRemote.json
     */
    @Test
    public void testRefWithinRemoteRef() {
        String schemaJson = "{\n" +
            "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" +
            "  \"$ref\": \"http://localhost:" + PORT + "/draft2020-12/subSchemas.json#/$defs/refToInteger\"\n" +
            "}";

        try (Arena arena = Arena.ofConfined();
             CompiledSchema schema = Blaze.compile(schemaJson, arena)) {
            
            final BlazeValidator validator = new BlazeValidator();
            
            // Test valid integer
            boolean validResult = validator.validate(schema, "1");
            System.out.println("Validation result for integer 1 with nested ref: " + validResult);
            assertTrue(validResult, "Integer should be valid against nested reference");
            
            // Test invalid string
            boolean invalidResult = validator.validate(schema, "\"a\"");
            System.out.println("Validation result for string with nested ref: " + invalidResult);
            assertFalse(invalidResult, "String should be invalid against nested reference");
        }
    }
    
    /**
     * Test validation with root ref in remote ref
     * Based on the "root ref in remote ref" test in refRemote.json
     */
    @Test
    public void testRootRefInRemoteRef() {
        String schemaJson = "{\n" +
            "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" +
            "  \"$id\": \"http://localhost:" + PORT + "/draft2020-12/object\",\n" +
            "  \"type\": \"object\",\n" +
            "  \"properties\": {\n" +
            "    \"name\": {\"$ref\": \"http://localhost:" + PORT + "/draft2020-12/name-defs.json#/$defs/orNull\"}\n" +
            "  }\n" +
            "}";

        try (Arena arena = Arena.ofConfined();
             CompiledSchema schema = Blaze.compile(schemaJson, arena)) {
            
            final BlazeValidator validator = new BlazeValidator();
            
            // Test valid string
            String validStringJson = "{\"name\": \"foo\"}";
            boolean validStringResult = validator.validate(schema, validStringJson);
            System.out.println("Validation result for string in root ref: " + validStringResult);
            assertTrue(validStringResult, "String should be valid");
            
            // Test valid null
            String validNullJson = "{\"name\": null}";
            boolean validNullResult = validator.validate(schema, validNullJson);
            System.out.println("Validation result for null in root ref: " + validNullResult);
            assertTrue(validNullResult, "Null should be valid");
            
            // Test invalid object
            String invalidObjectJson = "{\"name\": {\"name\": null}}";
            boolean invalidObjectResult = validator.validate(schema, invalidObjectJson);
            System.out.println("Validation result for object in root ref: " + invalidObjectResult);
            assertFalse(invalidObjectResult, "Object should be invalid");
        }
    }
    
    /**
     * Test validation with remote ref with different $id
     * Based on the "remote HTTP ref with different $id" test in refRemote.json
     */
    @Test
    public void testRemoteRefWithDifferentId() {
        String schemaJson = "{\n" +
            "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" +
            "  \"$ref\": \"http://localhost:" + PORT + "/draft2020-12/locationIndependentIdentifier.json#foo\"\n" +
            "}";

        try (Arena arena = Arena.ofConfined();
             CompiledSchema schema = Blaze.compile(schemaJson, arena)) {
            
            final BlazeValidator validator = new BlazeValidator();
            
            // Test valid integer
            boolean validResult = validator.validate(schema, "1");
            System.out.println("Validation result for string with different id: " + validResult);
            assertTrue(validResult, "Integer should be valid with different id reference");
            
            // Test invalid string
            boolean invalidResult = validator.validate(schema, "\"a\"");
            System.out.println("Validation result for number with different id: " + invalidResult);
            assertFalse(invalidResult, "String should be invalid with different id reference");
        }
    }

    /**
     * Handler for serving schema files from resources
     */
    private static class SchemaFileHandler implements HttpHandler {
        private static final String TEST_SUITE_REMOTES = "JSON-Schema-Test-Suite/remotes";

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Decode the path to handle URL-encoded characters
            String path = URLDecoder.decode(
                exchange.getRequestURI().getPath(),
                StandardCharsets.UTF_8
            );
            System.out.println("Mock server received request for: " + path);

            // Try potential resource locations in order
            String[] searchPaths = {
                TEST_SUITE_REMOTES + path,
                TEST_SUITE_REMOTES + "/draft2020-12" + path,
                TEST_SUITE_REMOTES + "/draft2020-12" + path.replace("/draft2020-12", "")
            };

            InputStream resourceStream = null;
            String foundResourcePath = null;

            // Try each potential path
            for (String currentSearchPath : searchPaths) {
                System.out.println("Looking for resource at: " + currentSearchPath);
                resourceStream = getClass().getClassLoader().getResourceAsStream(currentSearchPath);
                if (resourceStream != null) {
                    foundResourcePath = currentSearchPath;
                    break; 
                }
            }

            if (resourceStream != null) {
                try {
                    // Resource found
                    byte[] resourceBytes = resourceStream.readAllBytes(); 
                    resourceStream.close();

                    // Convert to string to check/add $schema
                    String schemaJson = new String(resourceBytes, StandardCharsets.UTF_8);
                    if (!schemaJson.contains("\"$schema\"")) {
                        // Add $schema if missing
                        schemaJson = schemaJson.replaceFirst("\\{", "{\n    \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",");
                        resourceBytes = schemaJson.getBytes(StandardCharsets.UTF_8);
                    }

                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, resourceBytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(resourceBytes);
                        os.flush(); // Ensure data is flushed
                    }
                    System.out.println("Mock server served schema from resource: " + foundResourcePath);
                    System.out.println("Fetched schema (length: " + resourceBytes.length + ")");
                } catch (Exception e) {
                    System.err.println("Error serving schema: " + e.getMessage());
                    String errorMessage = "{ \"error\": \"Server error: " + e.getMessage() + "\" }";
                    byte[] errorBytes = errorMessage.getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(500, errorBytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(errorBytes);
                        os.flush();
                    }
                }
            } else {
                // Not found
                System.out.println("Mock server could not find schema resource for path: " + path);
                System.out.println("Searched in: " + String.join(", ", searchPaths));
                String notFoundMessage = "{ \"error\": \"Schema not found: " + path + "\" }";
                byte[] responseBytes = notFoundMessage.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(404, responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                    os.flush(); // Ensure data is flushed
                }
            }
        }
    }
}