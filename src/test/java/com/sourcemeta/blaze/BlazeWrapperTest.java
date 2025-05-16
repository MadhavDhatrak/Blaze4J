package com.sourcemeta.blaze;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.foreign.Arena;

public class BlazeWrapperTest {

    @Test
    public void testSchemaValidation() {
        String schemaJson = "{"
            + "\"$schema\": \"https://json-schema.org/draft/2020-12/schema\","
            + "\"type\": \"string\""
            + "}";

        try (Arena arena = Arena.ofConfined();
             CompiledSchema schema = Blaze.compile(schemaJson, arena)) {
            
            final BlazeValidator validator = new BlazeValidator();
            
            // Test valid string input
            boolean validStringResult = validator.validate(schema, "\"hello\"");
            System.out.println("Validation result for \"hello\": " + validStringResult);
            assertTrue(validStringResult);
            
            // Test invalid non-string input
            boolean invalidNumberResult = validator.validate(schema, "42");
            System.out.println("Validation result for 42: " + invalidNumberResult);
            assertFalse(invalidNumberResult);
            
            boolean invalidNullResult = validator.validate(schema, "null");
            System.out.println("Validation result for null: " + invalidNullResult);
            assertFalse(invalidNullResult);
            
            boolean invalidBoolResult = validator.validate(schema, "true");
            System.out.println("Validation result for true: " + invalidBoolResult);
            assertFalse(invalidBoolResult);
        }
    }

    @Test
    public void testObjectSchemaValidation() {
        String schemaJson = "{"
            + "\"$schema\": \"https://json-schema.org/draft/2020-12/schema\","
            + "\"type\": \"object\","
            + "\"properties\": {"
            + "    \"name\": { \"type\": \"string\" },"
            + "    \"age\": { \"type\": \"integer\" }"
            + "},"
            + "\"required\": [\"name\"]"
            + "}";

        try (Arena arena = Arena.ofConfined();
             CompiledSchema schema = Blaze.compile(schemaJson, arena)) {
            
            final BlazeValidator validator = new BlazeValidator();
            
            // Test valid object
            boolean validObjectResult = validator.validate(schema, "{\"name\":\"John\",\"age\":30}");
            System.out.println("Validation result for {\"name\":\"John\",\"age\":30}: " + validObjectResult);
            assertTrue(validObjectResult);
            
            // Test invalid objects
            boolean missingNameResult = validator.validate(schema, "{\"age\":30}");
            System.out.println("Validation result for {\"age\":30}: " + missingNameResult);
            assertFalse(missingNameResult);
            
            boolean notObjectResult = validator.validate(schema, "\"not an object\"");
            System.out.println("Validation result for \"not an object\": " + notObjectResult);
            assertFalse(notObjectResult);
        }
    }
}