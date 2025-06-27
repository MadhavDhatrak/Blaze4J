package com.sourcemeta.blaze;

/**
 * Validator for JSON Schema validation
 */
public class BlazeValidator {
    /**
     * Validates a JSON instance against a compiled schema
     * 
     * @param schema The compiled schema
     * @param instance The JSON instance to validate
     * @return true if the instance is valid, false otherwise
     */
    public boolean validate(CompiledSchema schema, String instance) {
        return BlazeWrapper.validateInstance(schema, instance);
    }
    
    /**
     * Validates a JSON instance against a compiled schema with detailed results
     * 
     * @param schema The compiled schema
     * @param instance The JSON instance to validate
     * @return A ValidationResult containing detailed validation information
     */
    public ValidationResult validateWithDetails(CompiledSchema schema, String instance) {
        return BlazeWrapper.validateInstanceWithDetails(schema, instance);
    }
}