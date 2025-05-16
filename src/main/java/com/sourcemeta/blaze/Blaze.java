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
}