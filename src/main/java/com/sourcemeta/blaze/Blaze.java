package com.sourcemeta.blaze;

import java.lang.foreign.Arena;

/**
 * Main entry point for Blaze schema operations
 */
public class Blaze {
    /**
     * Compiles a JSON schema
     * 
     * @param schema JSON schema to compile
     * @param arena Memory arena for resource management
     * @return A compiled schema
     */
    public static CompiledSchema compile(String schema, Arena arena) {
        return BlazeWrapper.compileSchema(schema, arena);
    }
    
    /**
     * Compiles a JSON schema with an explicit default dialect
     * 
     * @param schema JSON schema to compile
     * @param arena Memory arena for resource management
     * @param defaultDialect Default dialect to use if the schema doesn't specify one
     * @return A compiled schema
     */
    public static CompiledSchema compile(String schema, Arena arena, String defaultDialect) {
        return BlazeWrapper.compileSchema(schema, arena, defaultDialect);
    }
    
    /**
     * Compiles a JSON schema, creating and managing an Arena internally
     * 
     * @param schema JSON schema to compile
     * @return A compiled schema
     */
    public static CompiledSchema compile(String schema) {
        Arena arena = Arena.ofConfined();
        return BlazeWrapper.compileSchema(schema, arena);
    }
    
    /**
     * Compiles a JSON schema with an explicit default dialect, creating and managing an Arena internally
     * 
     * @param schema JSON schema to compile
     * @param defaultDialect Default dialect to use if the schema doesn't specify one
     * @return A compiled schema
     */
    public static CompiledSchema compile(String schema, String defaultDialect) {
        Arena arena = Arena.ofConfined();
        return BlazeWrapper.compileSchema(schema, arena, defaultDialect);
    }
    
    /**
     * Validates a JSON instance against a schema
     * 
     * @param schema The compiled schema
     * @param instance The JSON instance to validate
     * @return true if the instance is valid, false otherwise
     */
    public static boolean validate(CompiledSchema schema, String instance) {
        BlazeValidator validator = new BlazeValidator();
        return validator.validate(schema, instance);
    }
    
    /**
     * Validates a JSON instance against a schema with detailed results
     * 
     * @param schema The compiled schema
     * @param instance The JSON instance to validate
     * @return A ValidationResult containing validation details
     */
    public static ValidationResult validateWithDetails(CompiledSchema schema, String instance) {
        BlazeValidator validator = new BlazeValidator();
        return validator.validateWithDetails(schema, instance);
    }
}