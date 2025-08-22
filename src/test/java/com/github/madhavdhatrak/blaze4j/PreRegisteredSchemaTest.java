package com.github.madhavdhatrak.blaze4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(Lifecycle.PER_CLASS)
public class PreRegisteredSchemaTest {

    @Test
    public void testSchemaCompilerWithRegistry() {
        // Define a simple schema for integers
        String integerSchema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "integer"
            }""";
        
        String schemaUri = "my-integer-schema";
        
        SchemaRegistry registry = new SchemaRegistry();
        registry.register(schemaUri, integerSchema);
        
        SchemaCompiler compiler = new SchemaCompiler(registry);
        
        String mainSchema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$ref": "my-integer-schema"
            }""";
        
        try (CompiledSchema schema = compiler.compile(mainSchema)) {
            // Test valid integer
            BlazeValidator validator = new BlazeValidator();
            boolean validResult = validator.validate(schema, "42");
            System.out.println("validResult: " + validResult);
            assertTrue(validResult, "Integer should be valid against pre-registered integer schema");
            
            // Test invalid string
            boolean invalidResult = validator.validate(schema, "\"not an integer\"");
            System.out.println("invalidResult: " + invalidResult);
            assertFalse(invalidResult, "String should be invalid against pre-registered integer schema");
        }
    }
    

}
