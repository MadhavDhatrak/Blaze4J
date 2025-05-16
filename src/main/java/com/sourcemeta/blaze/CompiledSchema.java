package com.sourcemeta.blaze;

import java.lang.AutoCloseable;

/**
 * Represents a compiled JSON schema that can be used for validation.
 */
public interface CompiledSchema extends AutoCloseable {
    /**
     * Get the native handle to the compiled schema
     * @return The native handle (64-bit pointer value)
     */
    long getHandle();
    
    /**
     * Close the schema and free native resources
     */
    @Override
    void close();
}