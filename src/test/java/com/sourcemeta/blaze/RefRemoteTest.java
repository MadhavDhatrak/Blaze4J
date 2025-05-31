package com.sourcemeta.blaze;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.Arena;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for the remote reference validation from the JSON Schema Test Suite
 */
@TestInstance(Lifecycle.PER_CLASS)
public class RefRemoteTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private CustomResolverTest resolverTest;
    
    @BeforeAll
    public void setup() throws IOException {
        // Start the mock server using CustomResolverTest's mock server
        resolverTest = new CustomResolverTest();
        resolverTest.setupServer();
    }
    
    @AfterAll
    public void cleanup() {
        // Shutdown the mock server
        if (resolverTest != null) {
            resolverTest.tearDownServer();
        }
    }
    
    /**
     * Load test cases from the refRemote.json file in JSON-Schema-Test-Suite
     */
    static Stream<Arguments> refRemoteTestCases() throws IOException {
        List<Arguments> testCases = new ArrayList<>();
        
        // Load the refRemote.json file from the test suite
        try (InputStream is = RefRemoteTest.class.getClassLoader()
                .getResourceAsStream("JSON-Schema-Test-Suite/tests/draft2020-12/refRemote.json")) {
            
            if (is == null) {
                throw new IOException("Could not load refRemote.json from resources");
            }
            
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            JsonNode testsArray = MAPPER.readTree(content);
            
            // Process test groups
            for (JsonNode testGroup : testsArray) {
                String groupDescription = testGroup.get("description").asText();
                JsonNode schema = testGroup.get("schema");
                JsonNode tests = testGroup.get("tests");
                
                // Process individual tests in each group
                for (JsonNode test : tests) {
                    String testDescription = test.get("description").asText();
                    JsonNode data = test.get("data");
                    boolean expectedValid = test.get("valid").asBoolean();
                    
                    // Add as an argument for the parameterized test
                    testCases.add(Arguments.of(
                        groupDescription,
                        testDescription, 
                        MAPPER.writeValueAsString(schema),
                        MAPPER.writeValueAsString(data),
                        expectedValid
                    ));
                }
            }
        }
        
        return testCases.stream();
    }
    
    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("refRemoteTestCases")
    @DisplayName("Test cases from refRemote.json")
    public void testRefRemoteCases(String groupDescription, String testDescription, 
                                  String schemaJson, String dataJson, boolean expectedValid) {
        try {
            // Add $schema declaration if it doesn't exist to fix "json document not found" errors
            if (!schemaJson.contains("$schema")) {
                // Parse the schema to a JsonNode
                JsonNode schemaNode = MAPPER.readTree(schemaJson);
                
                // Create a new ObjectNode with $schema as the first property
                com.fasterxml.jackson.databind.node.ObjectNode newSchemaNode = 
                    MAPPER.createObjectNode();
                
                // Add $schema declaration
                newSchemaNode.put("$schema", "https://json-schema.org/draft/2020-12/schema");
                
                // Copy all fields from the original schema
                schemaNode.fields().forEachRemaining(entry -> 
                    newSchemaNode.set(entry.getKey(), entry.getValue()));
                
                // Convert back to string
                schemaJson = MAPPER.writeValueAsString(newSchemaNode);
            }
            
            try (Arena arena = Arena.ofConfined();
                 CompiledSchema schema = Blaze.compile(schemaJson, arena)) {
                
                final BlazeValidator validator = new BlazeValidator();
                boolean result = validator.validate(schema, dataJson);
                
                assertEquals(expectedValid, result, 
                        groupDescription + " - " + testDescription);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error processing schema JSON", e);
        }
    }
}