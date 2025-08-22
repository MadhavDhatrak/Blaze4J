package com.github.madhavdhatrak.blaze4j;

import java.lang.foreign.Arena;

/**
 * Class responsible for compiling JSON schemas.
 * This class allows for schema compilation with various configuration options.
 */
public class SchemaCompiler {
    private final SchemaRegistry registry;
    
    /**
     * Creates a SchemaCompiler with no pre-registered schemas.
     */
    public SchemaCompiler() {
        this(new SchemaRegistry());
    }
    
    /**
     * Creates a SchemaCompiler with the specified schema registry.
     * 
     * @param registry Schema registry containing pre-registered schemas
     */
    public SchemaCompiler(SchemaRegistry registry) {
        if (registry == null) {
            throw new IllegalArgumentException("Schema registry cannot be null");
        }
        this.registry = registry;
    }
    
    /**
     * Compiles a JSON schema
     * 
     * @param schema JSON schema to compile
     * @param arena Memory arena for resource management
     * @return A compiled schema
     */
    public CompiledSchema compile(String schema, Arena arena) {
        return BlazeWrapper.compileSchema(schema, this.registry, arena);
    }
    
    /**
     * Compiles a JSON schema with an explicit default dialect
     * 
     * @param schema JSON schema to compile
     * @param arena Memory arena for resource management
     * @param defaultDialect Default dialect to use if the schema doesn't specify one
     * @return A compiled schema
     */
    public CompiledSchema compile(String schema, Arena arena, String defaultDialect) {
        return BlazeWrapper.compileSchema(schema, this.registry, arena, defaultDialect);
    }
    
    /**
     * Compiles a JSON schema, creating and managing an Arena internally
     * 
     * @param schema JSON schema to compile
     * @return A compiled schema
     */
    public CompiledSchema compile(String schema) {
        Arena arena = Arena.ofConfined();
        return compile(schema, arena);
    }
    
    /**
     * Compiles a JSON schema with an explicit default dialect, creating and managing an Arena internally
     * 
     * @param schema JSON schema to compile
     * @param defaultDialect Default dialect to use if the schema doesn't specify one
     * @return A compiled schema
     */
    public CompiledSchema compile(String schema, String defaultDialect) {
        Arena arena = Arena.ofConfined();
        return compile(schema, arena, defaultDialect);
    }
    
    /**
     * Gets the schema registry associated with this compiler
     * 
     * @return The schema registry
     */
    public SchemaRegistry getRegistry() {
        return registry;
    }
}
