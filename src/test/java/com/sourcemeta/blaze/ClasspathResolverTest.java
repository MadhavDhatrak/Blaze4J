package com.sourcemeta.blaze;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import java.lang.foreign.Arena;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(Lifecycle.PER_CLASS)
public class ClasspathResolverTest {

    /**
     * Test basic validation with Draft 2020-12
     */
    @Test
    public void testDraft202012Basic() {
        String schemaJson = "{\n" +
            "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" +
            "  \"$ref\": \"classpath://test-schema.json\"\n" +
            "}";

        try (Arena arena = Arena.ofConfined();
             CompiledSchema schema = BlazeWrapper.compileSchema(schemaJson, arena)) {
            
            // Test valid object with name and value
            String validJson = "{ \"name\": \"test\", \"value\": 42 }";
            boolean validResult = BlazeWrapper.validateInstance(schema, validJson);
            assertTrue(validResult, "Object with name and value should be valid");
            
            // Test valid object with only required name field
            String minimalJson = "{ \"name\": \"test\" }";
            boolean minimalResult = BlazeWrapper.validateInstance(schema, minimalJson);
            assertTrue(minimalResult, "Object with only name should be valid");
            
            // Test invalid object missing required name field
            String invalidJson = "{ \"value\": 42 }";
            boolean invalidResult = BlazeWrapper.validateInstance(schema, invalidJson);
            assertFalse(invalidResult, "Object without name should be invalid");
            
            // Test invalid object with wrong type for value
            String wrongTypeJson = "{ \"name\": \"test\", \"value\": \"not-a-number\" }";
            boolean wrongTypeResult = BlazeWrapper.validateInstance(schema, wrongTypeJson);
            assertFalse(wrongTypeResult, "Object with non-integer value should be invalid");
        }
    }

    /**
     * Test advanced validation with Draft 2020-12
     */
    @Test
    public void testDraft202012Advanced() {
        String schemaJson = "{\n" +
            "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" +
            "  \"type\": \"object\",\n" +
            "  \"properties\": {\n" +
            "    \"id\": { \"type\": \"string\", \"format\": \"uuid\" },\n" +
            "    \"status\": { \"type\": \"string\", \"enum\": [\"active\", \"inactive\"] },\n" +
            "    \"config\": { \"$ref\": \"classpath://test-schema.json\" }\n" +
            "  },\n" +
            "  \"required\": [\"id\", \"config\"]\n" +
            "}";

        try (Arena arena = Arena.ofConfined();
             CompiledSchema schema = BlazeWrapper.compileSchema(schemaJson, arena)) {
            
            // Test valid object with references
            String validJson = "{\n" +
                "  \"id\": \"123e4567-e89b-12d3-a456-426614174000\",\n" +
                "  \"status\": \"active\",\n" +
                "  \"config\": { \"name\": \"test-config\", \"value\": 42 }\n" +
                "}";
            boolean validResult = BlazeWrapper.validateInstance(schema, validJson);
            assertTrue(validResult, "Valid object with references should pass validation");
            
            // Test with invalid config (missing required field)
            String invalidConfigJson = "{\n" +
                "  \"id\": \"123e4567-e89b-12d3-a456-426614174000\",\n" +
                "  \"config\": { \"value\": 42 }\n" +
                "}";
            boolean invalidConfigResult = BlazeWrapper.validateInstance(schema, invalidConfigJson);
            assertFalse(invalidConfigResult, "Object with invalid config should fail validation");
            
            // Test with invalid status enum value
            String invalidStatusJson = "{\n" +
                "  \"id\": \"123e4567-e89b-12d3-a456-426614174000\",\n" +
                "  \"status\": \"pending\",\n" +
                "  \"config\": { \"name\": \"test-config\", \"value\": 42 }\n" +
                "}";
            boolean invalidStatusResult = BlazeWrapper.validateInstance(schema, invalidStatusJson);
            assertFalse(invalidStatusResult, "Object with invalid status should fail validation");
        }
    }

    /**
     * Test nested schema references with Draft 2020-12
     */
    @Test
    public void testDraft202012Nested() {
        String schemaJson = "{\n" +
            "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" +
            "  \"$ref\": \"classpath://nested-schema.json\"\n" +
            "}";

        try (Arena arena = Arena.ofConfined();
             CompiledSchema schema = BlazeWrapper.compileSchema(schemaJson, arena)) {
            
            // Test valid object with all references
            String validJson = "{\n" +
                "  \"metadata\": { \"name\": \"test\", \"value\": 42 },\n" +
                "  \"items\": [ {\"id\": \"item1\", \"count\": 5}, {\"id\": \"item2\", \"count\": 10} ],\n" +
                "  \"code\": \"valid-ID-123\"\n" +
                "}";
            boolean validResult = BlazeWrapper.validateInstance(schema, validJson);
            assertTrue(validResult, "Valid object with all references should pass validation");
            
            // Test with invalid string pattern in code field
            String invalidCodeJson = "{\n" +
                "  \"metadata\": { \"name\": \"test\", \"value\": 42 },\n" +
                "  \"code\": \"invalid@string\"\n" +
                "}";
            boolean invalidCodeResult = BlazeWrapper.validateInstance(schema, invalidCodeJson);
            assertFalse(invalidCodeResult, "Object with invalid code pattern should fail validation");
            
            // Test with missing required metadata field
            String missingMetadataJson = "{\n" +
                "  \"code\": \"valid-ID-123\"\n" +
                "}";
            boolean missingMetadataResult = BlazeWrapper.validateInstance(schema, missingMetadataJson);
            assertFalse(missingMetadataResult, "Object missing required metadata should fail validation");
        }
    }

    /**
     * Test basic validation with Draft 2019-09
     */
    @Test
    public void testDraft201909Basic() {
        String schemaJson = "{\n" +
            "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n" +
            "  \"$ref\": \"classpath://test-schema.json\"\n" +
            "}";

        try (Arena arena = Arena.ofConfined();
             CompiledSchema schema = BlazeWrapper.compileSchema(schemaJson, arena)) {
            
            // Test valid object with name and value
            String validJson = "{ \"name\": \"test\", \"value\": 42 }";
            boolean validResult = BlazeWrapper.validateInstance(schema, validJson);
            assertTrue(validResult, "Object with name and value should be valid");
            
            // Test valid object with only required name field
            String minimalJson = "{ \"name\": \"test\" }";
            boolean minimalResult = BlazeWrapper.validateInstance(schema, minimalJson);
            assertTrue(minimalResult, "Object with only name should be valid");
            
            // Test invalid object missing required name field
            String invalidJson = "{ \"value\": 42 }";
            boolean invalidResult = BlazeWrapper.validateInstance(schema, invalidJson);
            assertFalse(invalidResult, "Object without name should be invalid");
        }
    }

    /**
     * Test advanced validation with Draft 2019-09
     */
    @Test
    public void testDraft201909Advanced() {
        String schemaJson = "{\n" +
            "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n" +
            "  \"type\": \"object\",\n" +
            "  \"properties\": {\n" +
            "    \"id\": { \"type\": \"string\" },\n" +
            "    \"items\": {\n" +
            "      \"type\": \"array\",\n" +
            "      \"items\": { \"type\": \"string\" },\n" +
            "      \"minItems\": 1\n" +
            "    },\n" +
            "    \"config\": { \"$ref\": \"classpath://test-schema.json\" }\n" +
            "  },\n" +
            "  \"required\": [\"id\", \"config\", \"items\"]\n" +
            "}";

        try (Arena arena = Arena.ofConfined();
             CompiledSchema schema = BlazeWrapper.compileSchema(schemaJson, arena)) {
            
            // Test valid object
            String validJson = "{\n" +
                "  \"id\": \"test-id\",\n" +
                "  \"items\": [\"item1\", \"item2\"],\n" +
                "  \"config\": { \"name\": \"test-config\", \"value\": 42 }\n" +
                "}";
            boolean validResult = BlazeWrapper.validateInstance(schema, validJson);
            assertTrue(validResult, "Valid object should pass validation");
            
            // Test with empty items array (violates minItems)
            String emptyArrayJson = "{\n" +
                "  \"id\": \"test-id\",\n" +
                "  \"items\": [],\n" +
                "  \"config\": { \"name\": \"test-config\", \"value\": 42 }\n" +
                "}";
            boolean emptyArrayResult = BlazeWrapper.validateInstance(schema, emptyArrayJson);
            assertFalse(emptyArrayResult, "Object with empty array should fail validation");
            
            // Test with wrong type in items array
            String wrongTypeJson = "{\n" +
                "  \"id\": \"test-id\",\n" +
                "  \"items\": [\"item1\", 42],\n" +
                "  \"config\": { \"name\": \"test-config\", \"value\": 42 }\n" +
                "}";
            boolean wrongTypeResult = BlazeWrapper.validateInstance(schema, wrongTypeJson);
            assertFalse(wrongTypeResult, "Object with wrong type in array should fail validation");
        }
    }

    /**
     * Test nested schema references with Draft 2019-09
     */
    @Test
    public void testDraft201909Nested() {
        String schemaJson = "{\n" +
            "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n" +
            "  \"$ref\": \"classpath://nested-schema.json\"\n" +
            "}";

        try (Arena arena = Arena.ofConfined();
             CompiledSchema schema = BlazeWrapper.compileSchema(schemaJson, arena)) {
            
            // Test valid object with all references
            String validJson = "{\n" +
                "  \"metadata\": { \"name\": \"test\", \"value\": 42 },\n" +
                "  \"items\": [ {\"id\": \"item1\", \"count\": 5} ],\n" +
                "  \"code\": \"valid-ID-123\"\n" +
                "}";
            boolean validResult = BlazeWrapper.validateInstance(schema, validJson);
            assertTrue(validResult, "Valid object with all references should pass validation");
            
            // Test with invalid string (too short)
            String invalidStringJson = "{\n" +
                "  \"metadata\": { \"name\": \"test\", \"value\": 42 },\n" +
                "  \"code\": \"ab\"\n" +
                "}";
            boolean invalidStringResult = BlazeWrapper.validateInstance(schema, invalidStringJson);
            assertFalse(invalidStringResult, "Object with string too short should fail validation");
            
            // Test with duplicate items (violates uniqueItems)
            String duplicateItemsJson = "{\n" +
                "  \"metadata\": { \"name\": \"test\", \"value\": 42 },\n" +
                "  \"items\": [ {\"id\": \"item1\", \"count\": 5}, {\"id\": \"item1\", \"count\": 5} ],\n" +
                "  \"code\": \"valid-ID-123\"\n" +
                "}";
            boolean duplicateItemsResult = BlazeWrapper.validateInstance(schema, duplicateItemsJson);
            assertFalse(duplicateItemsResult, "Object with duplicate items should fail validation");
        }
    }

    /**
     * Test basic validation with Draft-07
     */
    @Test
    public void testDraft07Basic() {
        String schemaJson = "{\n" +
            "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" +
            "  \"$ref\": \"classpath://test-schema.json\"\n" +
            "}";

        try (Arena arena = Arena.ofConfined();
             CompiledSchema schema = BlazeWrapper.compileSchema(schemaJson, arena)) {
            
            // Test valid object with name and value
            String validJson = "{ \"name\": \"test\", \"value\": 42 }";
            boolean validResult = BlazeWrapper.validateInstance(schema, validJson);
            assertTrue(validResult, "Object with name and value should be valid");
            
            // Test valid object with only required name field
            String minimalJson = "{ \"name\": \"test\" }";
            boolean minimalResult = BlazeWrapper.validateInstance(schema, minimalJson);
            assertTrue(minimalResult, "Object with only name should be valid");
            
            // Test invalid object with wrong type for value
            String wrongTypeJson = "{ \"name\": \"test\", \"value\": \"not-a-number\" }";
            boolean wrongTypeResult = BlazeWrapper.validateInstance(schema, wrongTypeJson);
            assertFalse(wrongTypeResult, "Object with non-integer value should be invalid");
        }
    }

    /**
     * Test advanced validation with Draft-07
     */
    @Test
    public void testDraft07Advanced() {
        String schemaJson = "{\n" +
            "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" +
            "  \"type\": \"object\",\n" +
            "  \"properties\": {\n" +
            "    \"code\": { \"type\": \"string\", \"pattern\": \"^[A-Z]{2}-\\\\d{3}$\" },\n" +
            "    \"tags\": {\n" +
            "      \"type\": \"array\",\n" +
            "      \"items\": { \"type\": \"string\" },\n" +
            "      \"uniqueItems\": true\n" +
            "    },\n" +
            "    \"test\": { \"$ref\": \"classpath://test-schema.json\" }\n" +
            "  },\n" +
            "  \"required\": [\"code\", \"test\"]\n" +
            "}";

        try (Arena arena = Arena.ofConfined();
             CompiledSchema schema = BlazeWrapper.compileSchema(schemaJson, arena)) {
            
            // Test valid object
            String validJson = "{\n" +
                "  \"code\": \"AB-123\",\n" +
                "  \"tags\": [\"tag1\", \"tag2\"],\n" +
                "  \"test\": { \"name\": \"test-config\", \"value\": 42 }\n" +
                "}";
            boolean validResult = BlazeWrapper.validateInstance(schema, validJson);
            assertTrue(validResult, "Valid object should pass validation");
            
            // Test with invalid pattern match
            String invalidPatternJson = "{\n" +
                "  \"code\": \"AB123\",\n" +
                "  \"test\": { \"name\": \"test-config\", \"value\": 42 }\n" +
                "}";
            boolean invalidPatternResult = BlazeWrapper.validateInstance(schema, invalidPatternJson);
            assertFalse(invalidPatternResult, "Object with invalid pattern should fail validation");
            
            // Test with duplicate items in array (violates uniqueItems)
            String duplicateItemsJson = "{\n" +
                "  \"code\": \"AB-123\",\n" +
                "  \"tags\": [\"tag1\", \"tag1\"],\n" +
                "  \"test\": { \"name\": \"test-config\", \"value\": 42 }\n" +
                "}";
            boolean duplicateItemsResult = BlazeWrapper.validateInstance(schema, duplicateItemsJson);
            assertFalse(duplicateItemsResult, "Object with duplicate tags should fail validation");
        }
    }

    /**
     * Test nested schema references with Draft-07
     */
    @Test
    public void testDraft07Nested() {
        String schemaJson = "{\n" +
            "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" +
            "  \"$ref\": \"classpath://nested-schema.json\"\n" +
            "}";

        try (Arena arena = Arena.ofConfined();
             CompiledSchema schema = BlazeWrapper.compileSchema(schemaJson, arena)) {
            
            // Test valid object with all references
            String validJson = "{\n" +
                "  \"metadata\": { \"name\": \"test\", \"value\": 42 },\n" +
                "  \"items\": [ {\"id\": \"item1\", \"count\": 5}, {\"id\": \"item2\", \"count\": 10} ],\n" +
                "  \"code\": \"valid-ID-123\"\n" +
                "}";
            boolean validResult = BlazeWrapper.validateInstance(schema, validJson);
            assertTrue(validResult, "Valid object with all references should pass validation");
            
            // Test with invalid count (negative value)
            String invalidCountJson = "{\n" +
                "  \"metadata\": { \"name\": \"test\", \"value\": 42 },\n" +
                "  \"items\": [ {\"id\": \"item1\", \"count\": -5} ],\n" +
                "  \"code\": \"valid-ID-123\"\n" +
                "}";
            boolean invalidCountResult = BlazeWrapper.validateInstance(schema, invalidCountJson);
            assertFalse(invalidCountResult, "Object with negative count should fail validation");
            
            // Test with invalid metadata (missing required name)
            String invalidMetadataJson = "{\n" +
                "  \"metadata\": { \"value\": 42 },\n" +
                "  \"items\": [ {\"id\": \"item1\", \"count\": 5} ],\n" +
                "  \"code\": \"valid-ID-123\"\n" +
                "}";
            boolean invalidMetadataResult = BlazeWrapper.validateInstance(schema, invalidMetadataJson);
            assertFalse(invalidMetadataResult, "Object with invalid metadata should fail validation");
        }
    }

    /**
     * Test basic validation with Draft-06
     */
    @Test
    public void testDraft06Basic() {
        String schemaJson = "{\n" +
            "  \"$schema\": \"http://json-schema.org/draft-06/schema#\",\n" +
            "  \"$ref\": \"classpath://test-schema.json\"\n" +
            "}";

        try (Arena arena = Arena.ofConfined();
             CompiledSchema schema = BlazeWrapper.compileSchema(schemaJson, arena)) {
            
            // Test valid object with name and value
            String validJson = "{ \"name\": \"test\", \"value\": 42 }";
            boolean validResult = BlazeWrapper.validateInstance(schema, validJson);
            assertTrue(validResult, "Object with name and value should be valid");
            
            // Test invalid object missing required name field
            String invalidJson = "{ \"value\": 42 }";
            boolean invalidResult = BlazeWrapper.validateInstance(schema, invalidJson);
            assertFalse(invalidResult, "Object without name should be invalid");
            
            // Test object with additional property
            String additionalPropertyJson = "{ \"name\": \"test\", \"value\": 42, \"extra\": true }";
            boolean additionalPropertyResult = BlazeWrapper.validateInstance(schema, additionalPropertyJson);
            assertTrue(additionalPropertyResult, "Object with additional property should still be valid");
        }
    }

    /**
     * Test advanced validation with Draft-06
     */
    @Test
    public void testDraft06Advanced() {
        String schemaJson = "{\n" +
            "  \"$schema\": \"http://json-schema.org/draft-06/schema#\",\n" +
            "  \"type\": \"object\",\n" +
            "  \"properties\": {\n" +
            "    \"name\": { \"type\": \"string\", \"const\": \"config\" },\n" +
            "    \"settings\": {\n" +
            "      \"type\": \"object\",\n" +
            "      \"properties\": {\n" +
            "        \"enabled\": { \"type\": \"boolean\" },\n" +
            "        \"test\": { \"$ref\": \"classpath://test-schema.json\" }\n" +
            "      },\n" +
            "      \"required\": [\"enabled\", \"test\"]\n" +
            "    }\n" +
            "  },\n" +
            "  \"required\": [\"name\", \"settings\"]\n" +
            "}";

        try (Arena arena = Arena.ofConfined();
             CompiledSchema schema = BlazeWrapper.compileSchema(schemaJson, arena)) {
            
            // Test valid object with nested reference
            String validJson = "{\n" +
                "  \"name\": \"config\",\n" +
                "  \"settings\": {\n" +
                "    \"enabled\": true,\n" +
                "    \"test\": { \"name\": \"test-config\", \"value\": 42 }\n" +
                "  }\n" +
                "}";
            boolean validResult = BlazeWrapper.validateInstance(schema, validJson);
            assertTrue(validResult, "Valid object with nested reference should pass validation");
            
            // Test with invalid const value
            String invalidConstJson = "{\n" +
                "  \"name\": \"wrong-name\",\n" +
                "  \"settings\": {\n" +
                "    \"enabled\": true,\n" +
                "    \"test\": { \"name\": \"test-config\", \"value\": 42 }\n" +
                "  }\n" +
                "}";
            boolean invalidConstResult = BlazeWrapper.validateInstance(schema, invalidConstJson);
            assertFalse(invalidConstResult, "Object with invalid const value should fail validation");
            
            // Test with missing required field in nested reference
            String missingNestedFieldJson = "{\n" +
                "  \"name\": \"config\",\n" +
                "  \"settings\": {\n" +
                "    \"enabled\": true,\n" +
                "    \"test\": { \"value\": 42 }\n" +
                "  }\n" +
                "}";
            boolean missingNestedFieldResult = BlazeWrapper.validateInstance(schema, missingNestedFieldJson);
            assertFalse(missingNestedFieldResult, "Object missing required field in nested reference should fail validation");
        }
    }

    /**
     * Test nested schema references with Draft-06
     */
    @Test
    public void testDraft06Nested() {
        String schemaJson = "{\n" +
            "  \"$schema\": \"http://json-schema.org/draft-06/schema#\",\n" +
            "  \"$ref\": \"classpath://nested-schema.json\"\n" +
            "}";

        try (Arena arena = Arena.ofConfined();
             CompiledSchema schema = BlazeWrapper.compileSchema(schemaJson, arena)) {
            
            // Test valid object with all references
            String validJson = "{\n" +
                "  \"metadata\": { \"name\": \"test\", \"value\": 42 },\n" +
                "  \"items\": [ {\"id\": \"item1\", \"count\": 5}, {\"id\": \"item2\", \"count\": 10} ],\n" +
                "  \"code\": \"valid-ID-123\"\n" +
                "}";
            boolean validResult = BlazeWrapper.validateInstance(schema, validJson);
            assertTrue(validResult, "Valid object with all references should pass validation");
            
            // Test with string exceeding max length
            String tooLongStringJson = "{\n" +
                "  \"metadata\": { \"name\": \"test\", \"value\": 42 },\n" +
                "  \"code\": \"this-string-is-way-too-long-and-should-exceed-the-maximum-length-limit-defined-in-the-schema\"\n" +
                "}";
            boolean tooLongStringResult = BlazeWrapper.validateInstance(schema, tooLongStringJson);
            assertFalse(tooLongStringResult, "Object with string too long should fail validation");
            
            // Test with empty array (violates minItems)
            String emptyArrayJson = "{\n" +
                "  \"metadata\": { \"name\": \"test\", \"value\": 42 },\n" +
                "  \"items\": [],\n" +
                "  \"code\": \"valid-ID-123\"\n" +
                "}";
            boolean emptyArrayResult = BlazeWrapper.validateInstance(schema, emptyArrayJson);
            assertFalse(emptyArrayResult, "Object with empty array should fail validation");
        }
    }

    /**
     * Test basic validation with Draft-04
     */
    @Test
    public void testDraft04Basic() {
        String schemaJson = "{\n" +
            "  \"$schema\": \"http://json-schema.org/draft-04/schema#\",\n" +
            "  \"$ref\": \"classpath://test-schema.json\"\n" +
            "}";

        try (Arena arena = Arena.ofConfined();
             CompiledSchema schema = BlazeWrapper.compileSchema(schemaJson, arena)) {
            
            // Test valid object with name and value
            String validJson = "{ \"name\": \"test\", \"value\": 42 }";
            boolean validResult = BlazeWrapper.validateInstance(schema, validJson);
            assertTrue(validResult, "Object with name and value should be valid");
            
            // Test valid object with only required name field
            String minimalJson = "{ \"name\": \"test\" }";
            boolean minimalResult = BlazeWrapper.validateInstance(schema, minimalJson);
            assertTrue(minimalResult, "Object with only name should be valid");
            
            // Test invalid object missing required name field
            String invalidJson = "{ \"value\": 42 }";
            boolean invalidResult = BlazeWrapper.validateInstance(schema, invalidJson);
            assertFalse(invalidResult, "Object without name should be invalid");
        }
    }

    /**
     * Test advanced validation with Draft-04
     */
    @Test
    public void testDraft04Advanced() {
        String schemaJson = "{\n" +
            "  \"$schema\": \"http://json-schema.org/draft-04/schema#\",\n" +
            "  \"type\": \"object\",\n" +
            "  \"properties\": {\n" +
            "    \"id\": { \"type\": \"string\" },\n" +
            "    \"values\": {\n" +
            "      \"type\": \"array\",\n" +
            "      \"items\": { \"type\": \"number\" },\n" +
            "      \"minItems\": 1,\n" +
            "      \"maxItems\": 5\n" +
            "    },\n" +
            "    \"config\": { \"$ref\": \"classpath://test-schema.json\" }\n" +
            "  },\n" +
            "  \"required\": [\"id\", \"config\", \"values\"],\n" +
            "  \"additionalProperties\": false\n" +
            "}";

        try (Arena arena = Arena.ofConfined();
             CompiledSchema schema = BlazeWrapper.compileSchema(schemaJson, arena)) {
            
            // Test valid object
            String validJson = "{\n" +
                "  \"id\": \"test-id\",\n" +
                "  \"values\": [1, 2, 3],\n" +
                "  \"config\": { \"name\": \"test-config\", \"value\": 42 }\n" +
                "}";
            boolean validResult = BlazeWrapper.validateInstance(schema, validJson);
            assertTrue(validResult, "Valid object should pass validation");
            
            // Test with too many items in array
            String tooManyItemsJson = "{\n" +
                "  \"id\": \"test-id\",\n" +
                "  \"values\": [1, 2, 3, 4, 5, 6],\n" +
                "  \"config\": { \"name\": \"test-config\", \"value\": 42 }\n" +
                "}";
            boolean tooManyItemsResult = BlazeWrapper.validateInstance(schema, tooManyItemsJson);
            assertFalse(tooManyItemsResult, "Object with too many items should fail validation");
            
            // Test with additional property (violates additionalProperties: false)
            String additionalPropertyJson = "{\n" +
                "  \"id\": \"test-id\",\n" +
                "  \"values\": [1, 2, 3],\n" +
                "  \"config\": { \"name\": \"test-config\", \"value\": 42 },\n" +
                "  \"extra\": \"not allowed\"\n" +
                "}";
            boolean additionalPropertyResult = BlazeWrapper.validateInstance(schema, additionalPropertyJson);
            assertFalse(additionalPropertyResult, "Object with additional property should fail validation");
        }
    }

    /**
     * Test nested schema references with Draft-04
     */
    @Test
    public void testDraft04Nested() {
        String schemaJson = "{\n" +
            "  \"$schema\": \"http://json-schema.org/draft-04/schema#\",\n" +
            "  \"$ref\": \"classpath://nested-schema.json\"\n" +
            "}";

        try (Arena arena = Arena.ofConfined();
             CompiledSchema schema = BlazeWrapper.compileSchema(schemaJson, arena)) {
            
            // Test valid object with all references
            String validJson = "{\n" +
                "  \"metadata\": { \"name\": \"test\", \"value\": 42 },\n" +
                "  \"items\": [ {\"id\": \"item1\", \"count\": 5}, {\"id\": \"item2\", \"count\": 10} ],\n" +
                "  \"code\": \"valid-ID-123\"\n" +
                "}";
            boolean validResult = BlazeWrapper.validateInstance(schema, validJson);
            assertTrue(validResult, "Valid object with all references should pass validation");
            
            // Test with item missing required id
            String missingIdJson = "{\n" +
                "  \"metadata\": { \"name\": \"test\", \"value\": 42 },\n" +
                "  \"items\": [ {\"count\": 5} ],\n" +
                "  \"code\": \"valid-ID-123\"\n" +
                "}";
            boolean missingIdResult = BlazeWrapper.validateInstance(schema, missingIdJson);
            assertFalse(missingIdResult, "Object with item missing id should fail validation");
            
            // Test with metadata value of wrong type
            String wrongTypeJson = "{\n" +
                "  \"metadata\": { \"name\": \"test\", \"value\": \"not-a-number\" },\n" +
                "  \"items\": [ {\"id\": \"item1\", \"count\": 5} ],\n" +
                "  \"code\": \"valid-ID-123\"\n" +
                "}";
            boolean wrongTypeResult = BlazeWrapper.validateInstance(schema, wrongTypeJson);
            assertFalse(wrongTypeResult, "Object with wrong type in metadata should fail validation");
        }
    }
}
