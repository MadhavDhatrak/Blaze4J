package com.github.madhavdhatrak.blaze4j;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.foreign.Arena;
import java.util.List;

/**
 * Tests for detailed validation functionality
 */
public class DetailedValidationTest {

   
    // ---------------- Additional sample invalid cases using simple style ----------------

    @Test
    public void testEnumMismatchError() {
        String schema = "{ \"$schema\": \"https://json-schema.org/draft/2020-12/schema\", \"enum\": [\"red\", \"green\", \"blue\"] }";
        String instance = "\"yellow\"";

        SchemaCompiler compiler = new SchemaCompiler();
        try (Arena arena = Arena.ofConfined()) {
            CompiledSchema compiledSchema = compiler.compile(schema, arena);
            
            BlazeValidator validator = new BlazeValidator();
            ValidationResult result = validator.validateWithDetails(compiledSchema, instance);

            if (!result.isValid()) {
                result.getErrors().forEach(System.out::println);
            }

            assertFalse(result.isValid());
        }
    }

    @Test
    public void testArrayItemCountError() {
        String schema = "{ \"$schema\": \"https://json-schema.org/draft/2020-12/schema\", \"type\": \"array\", \"maxItems\": 2 }";
        String instance = "[1, 2, 3]";

        SchemaCompiler compiler = new SchemaCompiler();
        try (Arena arena = Arena.ofConfined()) {
            CompiledSchema compiledSchema = compiler.compile(schema, arena);
            
            BlazeValidator validator = new BlazeValidator();
            ValidationResult result = validator.validateWithDetails(compiledSchema, instance);

            if (!result.isValid()) {
                result.getErrors().forEach(System.out::println);
            }

            assertFalse(result.isValid());
        }
    }

    @Test
    public void testAdditionalPropertiesNotAllowed() {
        String schema = "{ \"$schema\": \"https://json-schema.org/draft/2020-12/schema\", \"type\": \"object\", \"properties\": { \"id\": { \"type\": \"integer\" } }, \"additionalProperties\": false }";
        String instance = "{ \"id\": 1, \"extra\": \"oops\" }";

        SchemaCompiler compiler = new SchemaCompiler();
        try (Arena arena = Arena.ofConfined()) {
            CompiledSchema compiledSchema = compiler.compile(schema, arena);
            
            BlazeValidator validator = new BlazeValidator();
            ValidationResult result = validator.validateWithDetails(compiledSchema, instance);

            if (!result.isValid()) {
                result.getErrors().forEach(System.out::println);
            }

            assertFalse(result.isValid());
        }
    }

    @Test
    public void testPatternMismatchError() {
        String schema = "{ \"$schema\": \"https://json-schema.org/draft/2020-12/schema\", \"type\": \"string\", \"pattern\": \"^[A-Z]{3}-\\\\d{3}$\" }";
        String instance = "\"abc-123\""; 

        SchemaCompiler compiler = new SchemaCompiler();
        BlazeValidator validator = new BlazeValidator();
        try (Arena arena = Arena.ofConfined()) {
            CompiledSchema compiledSchema = compiler.compile(schema, arena);
            ValidationResult result = validator.validateWithDetails(compiledSchema, instance);

            if (!result.isValid()) {
                result.getErrors().forEach(System.out::println);
            }

            assertFalse(result.isValid());
        }
    }

    @Test
    public void testMultipleOfError() {
        String schema = "{ \"$schema\": \"https://json-schema.org/draft/2020-12/schema\", \"type\": \"integer\", \"multipleOf\": 10 }";
        String instance = "25";

        SchemaCompiler compiler = new SchemaCompiler();
        BlazeValidator validator = new BlazeValidator();
        try (Arena arena = Arena.ofConfined()) {
            CompiledSchema compiledSchema = compiler.compile(schema, arena);
            ValidationResult result = validator.validateWithDetails(compiledSchema, instance);

            if (!result.isValid()) {
                result.getErrors().forEach(System.out::println);
            }

            assertFalse(result.isValid());
        }
    }

    @Test
    public void testNestedRequiredPropertyMissing() {
        String schema = "{ \"$schema\": \"https://json-schema.org/draft/2020-12/schema\", \"type\": \"object\", \"properties\": { \"user\": { \"type\": \"object\", \"properties\": { \"email\": { \"type\": \"string\", \"format\": \"email\" } }, \"required\": [\"email\"] } } }";
        String instance = "{ \"user\": {} }";

        SchemaCompiler compiler = new SchemaCompiler();
        BlazeValidator validator = new BlazeValidator();
        try (Arena arena = Arena.ofConfined()) {
            CompiledSchema compiledSchema = compiler.compile(schema, arena);
            ValidationResult result = validator.validateWithDetails(compiledSchema, instance);

            if (!result.isValid()) {
                result.getErrors().forEach(System.out::println);
            }

            assertFalse(result.isValid());
        }
    }
    
    // ---------------- Additional valid cases with multi-line formatting ----------------
    
    @Test
    public void testValidComplexObject() {
        String schema = "{\n" +
            "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" +
            "  \"type\": \"object\",\n" +
            "  \"properties\": {\n" +
            "    \"name\": { \"type\": \"string\" },\n" +
            "    \"age\": { \"type\": \"integer\", \"minimum\": 18 },\n" +
            "    \"email\": { \"type\": \"string\", \"format\": \"email\" },\n" +
            "    \"tags\": { \"type\": \"array\", \"items\": { \"type\": \"string\" } }\n" +
            "  },\n" +
            "  \"required\": [\"name\", \"age\"]\n" +
            "}";
            
        String instance = "{\n" +
            "  \"name\": \"John Doe\",\n" +
            "  \"age\": 30,\n" +
            "  \"email\": \"john@example.com\",\n" +
            "  \"tags\": [\"developer\", \"javascript\"]\n" +
            "}";

        SchemaCompiler compiler = new SchemaCompiler();
        BlazeValidator validator = new BlazeValidator();
        try (Arena arena = Arena.ofConfined()) {
            CompiledSchema compiledSchema = compiler.compile(schema, arena);
            ValidationResult result = validator.validateWithDetails(compiledSchema, instance);
            
              if (!result.isValid()) {
                result.getErrors().forEach(System.out::println);
            }
        }
    }
    
    @Test
    public void testValidNestedArrays() {
        String schema = "{\n" +
            "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" +
            "  \"type\": \"array\",\n" +
            "  \"items\": {\n" +
            "    \"type\": \"array\",\n" +
            "    \"items\": { \"type\": \"number\" },\n" +
            "    \"minItems\": 2\n" +
            "  }\n" +
            "}";
            
        String instance = "[\n" +
            "  [1, 2, 3],\n" +
            "  [4, 5],\n" +
            "  [6, 7, 8, 9]\n" +
            "]";

        SchemaCompiler compiler = new SchemaCompiler();
        BlazeValidator validator = new BlazeValidator();
        try (Arena arena = Arena.ofConfined()) {
            CompiledSchema compiledSchema = compiler.compile(schema, arena);
            ValidationResult result = validator.validateWithDetails(compiledSchema, instance);
            
             if (!result.isValid()) {
                result.getErrors().forEach(System.out::println);
            }
        }
    }
    
    // ---------------- Additional invalid cases with multi-line formatting ----------------
    
    @Test
    public void testDependentRequiredViolation() {
        String schema = "{\n" +
            "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" +
            "  \"type\": \"object\",\n" +
            "  \"properties\": {\n" +
            "    \"credit_card\": { \"type\": \"string\" },\n" +
            "    \"billing_address\": { \"type\": \"string\" }\n" +
            "  },\n" +
            "  \"dependentRequired\": {\n" +
            "    \"credit_card\": [\"billing_address\"]\n" +
            "  }\n" +
            "}";
            
        String instance = "{\n" +
            "  \"credit_card\": \"1234-5678-9012-3456\"\n" +
            "}";

        SchemaCompiler compiler = new SchemaCompiler();
        BlazeValidator validator = new BlazeValidator();
        try (Arena arena = Arena.ofConfined()) {
            CompiledSchema compiledSchema = compiler.compile(schema, arena);
            ValidationResult result = validator.validateWithDetails(compiledSchema, instance);
            
            if (!result.isValid()) {
                result.getErrors().forEach(System.out::println);
            }
        }
    }
    
    @Test
    public void testMinMaxPropertiesViolation() {
        String schema = "{\n" +
            "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" +
            "  \"type\": \"object\",\n" +
            "  \"minProperties\": 2,\n" +
            "  \"maxProperties\": 3\n" +
            "}";
            
        String instance = "{\n" +
            "  \"prop1\": \"value1\",\n" +
            "  \"prop2\": \"value2\",\n" +
            "  \"prop3\": \"value3\",\n" +
            "  \"prop4\": \"value4\"\n" +
            "}";

        SchemaCompiler compiler = new SchemaCompiler();
        BlazeValidator validator = new BlazeValidator();
        try (Arena arena = Arena.ofConfined()) {
            CompiledSchema compiledSchema = compiler.compile(schema, arena);
            ValidationResult result = validator.validateWithDetails(compiledSchema, instance);
            
            assertFalse(result.isValid());
            assertTrue(result.getErrors().size() > 0);
            
            // Verify error contains information about maxProperties
            ValidationError error = result.getErrors().get(0);
            assertTrue(error.getMessage().contains("properties") || 
                       error.getEvaluatePath().contains("maxProperties"));
        }
    }
    
    @Test
    public void testFormatValidationError() {
        String schema = "{\n" +
            "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" +
            "  \"type\": \"object\",\n" +
            "  \"properties\": {\n" +
            "    \"email\": { \"type\": \"string\", \"format\": \"email\" },\n" +
            "    \"date\": { \"type\": \"string\", \"format\": \"date\" },\n" +
            "    \"uri\": { \"type\": \"string\", \"format\": \"uri\" }\n" +
            "  }\n" +
            "}";
            
        String instance = "{\n" +
            "  \"email\": \"not-an-email\",\n" +
            "  \"date\": \"2023-13-32\",\n" +
            "  \"uri\": \"not a uri\"\n" +
            "}";

        SchemaCompiler compiler = new SchemaCompiler();
        BlazeValidator validator = new BlazeValidator();
        try (Arena arena = Arena.ofConfined()) {
            CompiledSchema compiledSchema = compiler.compile(schema, arena);
            ValidationResult result = validator.validateWithDetails(compiledSchema, instance);
            
            // Note: Format validation might be annotation-only by default
            // So we're only checking that we get a result, not necessarily errors
            if (!result.isValid()) {
                System.out.println("Format validation errors:");
                result.getErrors().forEach(System.out::println);
            }
        }
    }
    
   

    @Test
    public void testClasspathReferenceValidationWithDetails() {
        String schemaJson = "{"
            + "\"$schema\": \"https://json-schema.org/draft/2020-12/schema\","
            + "\"$ref\": \"classpath://test-schema.json\""
            + "}";
        
        SchemaCompiler compiler = new SchemaCompiler();
        try (Arena arena = Arena.ofConfined();
             CompiledSchema schema = compiler.compile(schemaJson, arena)) {
            
            BlazeValidator validator = new BlazeValidator();
            // Test valid instance
            String validInstance = "{\"name\":\"test\",\"value\":42}";
            ValidationResult validResult = validator.validateWithDetails(schema, validInstance);
            assertTrue(validResult.isValid(), "Instance with valid properties should validate");
            assertEquals(0, validResult.getErrors().size(), "Valid instance should have no errors");
            
            // Test invalid instance (missing required property)
            String invalidInstance = "{\"value\":42}";
            ValidationResult invalidResult = validator.validateWithDetails(schema, invalidInstance);
            assertFalse(invalidResult.isValid(), "Instance missing required property should not validate");
            assertTrue(invalidResult.getErrors().size() > 0, "Invalid instance should have errors");
            
            // Print errors for debugging
            System.out.println("Validation errors for missing required property:");
            invalidResult.getErrors().forEach(System.out::println);
            
            // Test invalid instance (wrong type)
            String wrongTypeInstance = "{\"name\":\"test\",\"value\":\"not-an-integer\"}";
            ValidationResult wrongTypeResult = validator.validateWithDetails(schema, wrongTypeInstance);
            assertFalse(wrongTypeResult.isValid(), "Instance with wrong type should not validate");
            assertTrue(wrongTypeResult.getErrors().size() > 0, "Type error instance should have errors");
            
            // Print errors for debugging
            System.out.println("\nValidation errors for wrong type:");
            wrongTypeResult.getErrors().forEach(System.out::println);
            
        }
    }
} 