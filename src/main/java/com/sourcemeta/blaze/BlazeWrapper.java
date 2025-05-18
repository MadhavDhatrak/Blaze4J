package com.sourcemeta.blaze;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

public class BlazeWrapper {
    private static final Linker linker = Linker.nativeLinker();
    private static final SymbolLookup symbolLookup;
    private static final MethodHandle blazeCompileHandle;
    private static final MethodHandle blazeValidateHandle;
    private static final MethodHandle blazeFreeTemplateHandle;
    
    static {
        try {
           
            System.out.println("Attempting to load native library 'blaze_wrapper' using System.loadLibrary");
            System.loadLibrary("blaze_wrapper");
            System.out.println("Successfully loaded native library");
        } catch (UnsatisfiedLinkError e1) {
           
            try {
                String libPath = System.getProperty("user.dir").replace("\\", "/") + "/build/bin/Debug/blaze_wrapper.dll";
                System.out.println("Fallback: Attempting to load native library from: " + libPath);
                System.load(libPath);
                System.out.println("Successfully loaded native library from explicit path");
            } catch (UnsatisfiedLinkError e2) {
                System.err.println("Failed to load native library: " + e2.getMessage());
                throw e2;
            }
        }
        symbolLookup = SymbolLookup.loaderLookup();
        
        FunctionDescriptor compileDesc = FunctionDescriptor.of(
            ValueLayout.JAVA_LONG,  
            ValueLayout.ADDRESS,    
            ValueLayout.ADDRESS,   
            ValueLayout.ADDRESS     
        );
        
        FunctionDescriptor validateDesc = FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN,  
            ValueLayout.JAVA_LONG,     
            ValueLayout.ADDRESS        
        );
        
        FunctionDescriptor freeTemplateDesc = FunctionDescriptor.ofVoid(
            ValueLayout.JAVA_LONG     
        );
        
        try {
            var blazeCompileSymbol = symbolLookup.find("blaze_compile");
            if (!blazeCompileSymbol.isPresent()) {
                throw new UnsatisfiedLinkError("Could not find 'blaze_compile' function in native library");
            }
            blazeCompileHandle = linker.downcallHandle(
                blazeCompileSymbol.get(),
                compileDesc
            );
            
            var blazeValidateSymbol = symbolLookup.find("blaze_validate");
            if (!blazeValidateSymbol.isPresent()) {
                throw new UnsatisfiedLinkError("Could not find 'blaze_validate' function in native library");
            }
            blazeValidateHandle = linker.downcallHandle(
                blazeValidateSymbol.get(),
                validateDesc
            );
            
            var blazeFreeTemplateSymbol = symbolLookup.find("blaze_free_template");
            if (!blazeFreeTemplateSymbol.isPresent()) {
                throw new UnsatisfiedLinkError("Could not find 'blaze_free_template' function in native library");
            }
            blazeFreeTemplateHandle = linker.downcallHandle(
                blazeFreeTemplateSymbol.get(),
                freeTemplateDesc
            );
        } catch (Throwable e) {
            throw new RuntimeException("Failed to initialize native methods", e);
        }
    }

    /**
     * Compiles a JSON schema using the blaze compiler.
     * (Legacy method for backward compatibility)
     *
     * @param schema The JSON schema to compile
     * @param walker The schema walker configuration (ignored in native code)
     * @param resolver The schema resolver configuration (ignored in native code)
     * @return The compiled template as a string
     * @throws RuntimeException if compilation fails
     */
    public static String compile(String schema, String walker, String resolver) {
        try (Arena arena = Arena.ofConfined()) {
            CompiledSchema compiledSchema = compileSchema(schema, arena);
            // For backward compatibility, return a success message
            return "Compilation succeeded";
        } catch (RuntimeException e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Compiles a JSON schema using the blaze compiler
     *
     * @param schema The JSON schema to compile
     * @param arena Memory arena for resource management
     * @return A compiled schema object
     * @throws RuntimeException if compilation fails
     */
    public static CompiledSchema compileSchema(String schema, Arena arena) {
        // Default empty values for walker and resolver
        String walker = "{}";
        String resolver = "{}";
        
        try {
           
            MemorySegment schemaSeg = arena.allocateUtf8String(schema);
            MemorySegment walkerSeg = arena.allocateUtf8String(walker);
            MemorySegment resolverSeg = arena.allocateUtf8String(resolver);
            
            
            long schemaHandle;
            try {
                schemaHandle = (long) blazeCompileHandle.invoke(
                    schemaSeg,
                    walkerSeg,
                    resolverSeg
                );
            } catch (Throwable e) {
                throw new RuntimeException("Failed to invoke native compile function", e);
            }
            
            if (schemaHandle == 0) {
                throw new RuntimeException("Schema compilation failed - null handle returned");
            }
            
            
            return new CompiledSchemaImpl(schemaHandle);
        } catch (RuntimeException e) {
            throw e;  // Rethrow RuntimeExceptions directly
        } catch (Throwable e) {
            throw new RuntimeException("Unexpected error during schema compilation", e);
        }
    }
    
    /**
     * Validates a JSON instance against a compiled schema
     *
     * @param schema The compiled schema
     * @param instance The JSON instance to validate
     * @return true if the instance is valid, false otherwise
     */
    public static boolean validateInstance(CompiledSchema schema, String instance) {
        try (Arena arena = Arena.ofConfined()) {
           
            MemorySegment instanceSeg = arena.allocateUtf8String(instance);
            long schemaHandle = schema.getHandle();
            
            try {
                return (boolean) blazeValidateHandle.invoke(schemaHandle, instanceSeg);
            } catch (Throwable e) {
                throw new RuntimeException("Failed to invoke native validate function", e);
            }
        } catch (RuntimeException e) {
            throw e;  // Rethrow RuntimeExceptions directly
        } catch (Throwable e) {
            throw new RuntimeException("Unexpected error during validation", e);
        }
    }
    
    /**
     * Free a compiled schema's native resources
     *
     * @param schemaHandle The handle to the compiled schema
     */
    public static void freeCompiledSchema(long schemaHandle) {
        try {
            blazeFreeTemplateHandle.invoke(schemaHandle);
        } catch (Throwable e) {
            System.err.println("Warning: Failed to free native template memory: " + e.getMessage());
        }
    }
    
    /**
     * Implementation of the CompiledSchema interface
     */
    private static class CompiledSchemaImpl implements CompiledSchema {
        private final long handle;
        private boolean closed = false;
        
        public CompiledSchemaImpl(long handle) {
            this.handle = handle;
        }
        
        @Override
        public long getHandle() {
            if (closed) {
                throw new IllegalStateException("Schema has been closed");
            }
            return handle;
        }
        
        @Override
        public void close() {
            if (!closed) {
                freeCompiledSchema(handle);
                closed = true;
            }
        }
        
        @Override
        protected void finalize() throws Throwable {
            if (!closed) {
                System.err.println("Warning: CompiledSchema was not closed properly. Attempting cleanup in finalizer.");
                try {
                    close();
                } catch (Exception e) {
                    System.err.println("Failed to close CompiledSchema in finalizer: " + e.getMessage());
                }
            }
            super.finalize();
        }
    }
}