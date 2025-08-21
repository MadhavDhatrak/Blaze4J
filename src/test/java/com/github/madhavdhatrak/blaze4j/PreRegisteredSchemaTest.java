package com.github.madhavdhatrak.blaze4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(Lifecycle.PER_CLASS)
public class PreRegisteredSchemaTest {

   
    /**
     * Test basic schema pre-registration and validation with custom URI using SchemaRegistry
     */
    @Test
    public void testBasicPreRegisteredSchema() {
        // Define a simple schema for integers
        String integerSchema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "integer"
            }""";
        
        String schemaUri = "my-integer-schema";
        
        // Create a registry and register the schema
        SchemaRegistry registry = Blaze.newRegistry();
        registry.register(schemaUri, integerSchema);
        
        String mainSchema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$ref": "my-integer-schema"
            }""";

        try (CompiledSchema schema = Blaze.compile(mainSchema, registry)) {
            
            // Test valid integer
            boolean validResult = Blaze.validate(schema, "42");
            System.out.println("Validation result for valid integer: " + validResult);
            assertTrue(validResult, "Integer should be valid against pre-registered integer schema");
            
            // Test invalid string
            boolean invalidResult = Blaze.validate(schema, "\"not an integer\"");
            System.out.println("Validation result for invalid string: " + invalidResult);
            assertFalse(invalidResult, "String should be invalid against pre-registered integer schema");
        }
    }

   

    /**
     * Test with multiple pre-registered schemas that reference each other using custom URIs with SchemaRegistry
     */
    @Test
    public void testMultiplePreRegisteredSchemas() {
        // Define a schema for address
        String addressSchema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "street": { "type": "string" },
                "city": { "type": "string" }
              },
              "required": ["street", "city"]
            }""";
        
        // Define a schema for person that references address
        String personSchema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "name": { "type": "string" },
                "age": { "type": "integer", "minimum": 0 },
                "address": { "$ref": "address-schema" }
              },
              "required": ["name"]
            }""";
        
        // Create a registry and register both schemas
        SchemaRegistry registry = Blaze.newRegistry();
        registry.register("address-schema", addressSchema);
        registry.register("person-schema", personSchema);
        
        // Create a schema that references the person schema
        String mainSchema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$ref": "person-schema"
            }""";

        try (CompiledSchema schema = Blaze.compile(mainSchema, registry)) {
            
            String validJson = """
                {
                  "name": "John Doe",
                  "age": 30,
                  "address": {
                    "street": "123 Main St",
                    "city": "Anytown"
                  }
                }""";
            boolean validResult = Blaze.validate(schema, validJson);
            System.out.println("Validation result for valid nested object: " + validResult);
            assertTrue(validResult, "Valid nested object should be valid");
            
            String missingCityJson = """
                {
                  "name": "John Doe",
                  "age": 30,
                  "address": {
                    "street": "123 Main St"
                  }
                }""";
            boolean missingCityResult = Blaze.validate(schema, missingCityJson);
            System.out.println("Validation result for missing city: " + missingCityResult);
            assertFalse(missingCityResult, "Object missing required city should be invalid");
        }
    }
}
