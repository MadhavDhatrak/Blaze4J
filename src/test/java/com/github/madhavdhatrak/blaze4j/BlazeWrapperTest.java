package com.github.madhavdhatrak.blaze4j;

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

        SchemaCompiler compiler = new SchemaCompiler();
        try (CompiledSchema schema = compiler.compile(schemaJson)) {
            
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

        SchemaCompiler compiler = new SchemaCompiler();
        try (CompiledSchema schema = compiler.compile(schemaJson)) {
            
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

    @Test
    public void testArraySchemaValidation() {
        String schemaJson = "{"
            + "\"$schema\": \"https://json-schema.org/draft/2020-12/schema\","
            + "\"type\": \"array\","
            + "\"items\": { \"type\": \"number\" },"
            + "\"minItems\": 2,"
            + "\"maxItems\": 4"
            + "}";

        SchemaCompiler compiler = new SchemaCompiler();
        try (CompiledSchema schema = compiler.compile(schemaJson)) {
            
            final BlazeValidator validator = new BlazeValidator();
            
            // Test valid array
            boolean validArrayResult = validator.validate(schema, "[1, 2, 3]");
            System.out.println("Validation result for [1, 2, 3]: " + validArrayResult);
            assertTrue(validArrayResult);
            
            // Test too short array
            boolean tooShortResult = validator.validate(schema, "[1]");
            System.out.println("Validation result for [1]: " + tooShortResult);
            assertFalse(tooShortResult);
            
            // Test too long array
            boolean tooLongResult = validator.validate(schema, "[1, 2, 3, 4, 5]");
            System.out.println("Validation result for [1, 2, 3, 4, 5]: " + tooLongResult);
            assertFalse(tooLongResult);
            
            // Test wrong item type
            boolean wrongTypeResult = validator.validate(schema, "[1, \"string\", 3]");
            System.out.println("Validation result for [1, \"string\", 3]: " + wrongTypeResult);
            assertFalse(wrongTypeResult);
        }
    }

    @Test
    public void testNumberSchemaValidation() {
        String schemaJson = "{"
            + "\"$schema\": \"https://json-schema.org/draft/2020-12/schema\","
            + "\"type\": \"number\","
            + "\"minimum\": 0,"
            + "\"maximum\": 100,"
            + "\"multipleOf\": 5"
            + "}";

        SchemaCompiler compiler = new SchemaCompiler();
        try (CompiledSchema schema = compiler.compile(schemaJson)) {
            
            final BlazeValidator validator = new BlazeValidator();
            
            // Test valid number
            boolean validNumberResult = validator.validate(schema, "50");
            System.out.println("Validation result for 50: " + validNumberResult);
            assertTrue(validNumberResult);
            
            // Test number too small
            boolean tooSmallResult = validator.validate(schema, "-10");
            System.out.println("Validation result for -10: " + tooSmallResult);
            assertFalse(tooSmallResult);
            
            // Test number too large
            boolean tooLargeResult = validator.validate(schema, "150");
            System.out.println("Validation result for 150: " + tooLargeResult);
            assertFalse(tooLargeResult);
            
            // Test not multiple of 5
            boolean notMultipleResult = validator.validate(schema, "27");
            System.out.println("Validation result for 27: " + notMultipleResult);
            assertFalse(notMultipleResult);
        }
    }

    @Test
    public void testPatternValidation() {
        String schemaJson = "{"
            + "\"$schema\": \"https://json-schema.org/draft/2020-12/schema\","
            + "\"type\": \"string\","
            + "\"pattern\": \"^[A-Z][a-z]+$\""
            + "}";

        SchemaCompiler compiler = new SchemaCompiler();
        try (CompiledSchema schema = compiler.compile(schemaJson)) {
            
            final BlazeValidator validator = new BlazeValidator();
            
            // Test valid pattern
            boolean validPatternResult = validator.validate(schema, "\"Hello\"");
            System.out.println("Validation result for \"Hello\": " + validPatternResult);
            assertTrue(validPatternResult);
            
            // Test invalid pattern - all lowercase
            boolean allLowerResult = validator.validate(schema, "\"hello\"");
            System.out.println("Validation result for \"hello\": " + allLowerResult);
            assertFalse(allLowerResult);
            
            // Test invalid pattern - contains number
            boolean containsNumberResult = validator.validate(schema, "\"Hello123\"");
            System.out.println("Validation result for \"Hello123\": " + containsNumberResult);
            assertFalse(containsNumberResult);
        }
    }

    @Test
    public void testCombinedSchemaValidation() {
        String schemaJson = "{"
            + "\"$schema\": \"https://json-schema.org/draft/2020-12/schema\","
            + "\"oneOf\": ["
            + "    { \"type\": \"string\" },"
            + "    { \"type\": \"number\", \"minimum\": 10 }"
            + "]"
            + "}";

        SchemaCompiler compiler = new SchemaCompiler();
        try (CompiledSchema schema = compiler.compile(schemaJson)) {
            
            final BlazeValidator validator = new BlazeValidator();
            
            // Test valid string
            boolean validStringResult = validator.validate(schema, "\"test\"");
            System.out.println("Validation result for \"test\": " + validStringResult);
            assertTrue(validStringResult);
            
            // Test valid number
            boolean validNumberResult = validator.validate(schema, "15");
            System.out.println("Validation result for 15: " + validNumberResult);
            assertTrue(validNumberResult);
            
            // Test invalid number (too small)
            boolean invalidNumberResult = validator.validate(schema, "5");
            System.out.println("Validation result for 5: " + invalidNumberResult);
            assertFalse(invalidNumberResult);
            
            // Test invalid type
            boolean invalidTypeResult = validator.validate(schema, "true");
            System.out.println("Validation result for true: " + invalidTypeResult);
            assertFalse(invalidTypeResult);
        }
    }
}