package com.github.madhavdhatrak.blaze4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.BeforeAll;

import java.lang.foreign.Arena;
import java.util.logging.Logger;
import java.util.logging.Level;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(Lifecycle.PER_CLASS)
public class ClasspathResolverTest {
    
    private static final Logger LOGGER = Logger.getLogger(ClasspathResolverTest.class.getName());
    
    @BeforeAll
    public void setupLogger() {
        Blaze4jLogger.configureLogging(Level.FINE);
    }

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
            System.out.println("Minimal JSON test result: " + minimalResult);
            // Test invalid object missing required name field
            String invalidJson = "{ \"value\": 42 }";
            
            boolean invalidResult = BlazeWrapper.validateInstance(schema, invalidJson);
            assertFalse(invalidResult, "Object without name should be invalid");
            System.out.println("Invalid JSON test result: " + invalidResult);
            // Test invalid object with wrong type for value
            String wrongTypeJson = "{ \"name\": \"test\", \"value\": \"not-a-number\" }";
            
            boolean wrongTypeResult = BlazeWrapper.validateInstance(schema, wrongTypeJson);
            assertFalse(wrongTypeResult, "Object with non-integer value should be invalid");
            System.out.println("Wrong type JSON test result: " + wrongTypeResult);
        }
    }
   
    
}
