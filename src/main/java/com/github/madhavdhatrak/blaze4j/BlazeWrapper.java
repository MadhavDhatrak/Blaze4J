package com.github.madhavdhatrak.blaze4j;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.lang.ref.Cleaner;
import java.util.logging.Logger;

class BlazeWrapper {
    private static final Logger LOGGER = Logger.getLogger(BlazeWrapper.class.getName());
    private static final Linker linker = Linker.nativeLinker();
    private static final SymbolLookup symbolLookup;
    private static final MethodHandle blazeCompileHandle;
    private static final MethodHandle blazeValidateHandle;
    private static final MethodHandle blazeFreeTemplateHandle;
    private static final MethodHandle blazeAllocStringHandle;
    private static final MethodHandle blazeFreeStringHandle;
    private static final MethodHandle blazeValidateWithOutputHandle;
    private static final MethodHandle blazeFreeJsonHandle;
    private static final MemorySegment resolverUpcallStub;
    private static final Cleaner cleaner = Cleaner.create();

    static {
        try {
            System.loadLibrary("blaze4j");
            } catch (UnsatisfiedLinkError e) {
                try {
                    NativeLoader.loadLibrary("blaze4j");
                    LOGGER.fine("Loaded native library from JAR");
                } catch (Exception ex) {
                    LOGGER.severe("FATAL: Failed to load native library: " + ex.getMessage());
                    ex.printStackTrace();
                    throw new RuntimeException("Failed to load native library: " + ex.getMessage(), ex);
            }
        }

        symbolLookup = SymbolLookup.loaderLookup();
        
        // Get blaze string allocation functions from the native library
        try {
            MemorySegment allocStringSymbol = symbolLookup.find("blaze_alloc_string").orElseThrow();
            blazeAllocStringHandle = linker.downcallHandle(
                allocStringSymbol,
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
            );
            
            MemorySegment freeStringSymbol = symbolLookup.find("blaze_free_string").orElseThrow();
            blazeFreeStringHandle = linker.downcallHandle(
                freeStringSymbol,
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
            );
        } catch (Throwable e) {
            throw new RuntimeException("Failed to initialize blaze string allocation handles: " + e.getMessage());
        }

        // Setup blaze_compile handle
        FunctionDescriptor compileDesc = FunctionDescriptor.of(
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS
        );
        try {
            blazeCompileHandle = linker.downcallHandle(
                symbolLookup.find("blaze_compile").orElseThrow(),
                compileDesc
            );
        } catch (Throwable e) {
            throw new RuntimeException("Failed to initialize blaze_compile handle", e);
        }

        // Setup blaze_validate handle
        FunctionDescriptor validateDesc = FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS
        );
        try {
            blazeValidateHandle = linker.downcallHandle(
                symbolLookup.find("blaze_validate").orElseThrow(),
                validateDesc
            );
        } catch (Throwable e) {
            throw new RuntimeException("Failed to initialize blaze_validate handle", e);
        }

        // Setup blaze_validate_with_output handle
        FunctionDescriptor validateWithOutputDesc = FunctionDescriptor.of(
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS
        );
        try {
            blazeValidateWithOutputHandle = linker.downcallHandle(
                symbolLookup.find("blaze_validate_with_output").orElseThrow(),
                validateWithOutputDesc
            );
        } catch (Throwable e) {
            throw new RuntimeException("Failed to initialize blaze_validate_with_output handle", e);
        }
        
        // Setup blaze_free_json handle
        FunctionDescriptor freeJsonDesc = FunctionDescriptor.ofVoid(ValueLayout.ADDRESS);
        try {
            blazeFreeJsonHandle = linker.downcallHandle(
                symbolLookup.find("blaze_free_json").orElseThrow(), 
                freeJsonDesc
            );
        } catch (Throwable e) {
            throw new RuntimeException("Failed to initialize blaze_free_json handle", e);
        }

        // Setup blaze_free_template handle
        FunctionDescriptor freeTemplateDesc = FunctionDescriptor.ofVoid(ValueLayout.JAVA_LONG);
        try {
            blazeFreeTemplateHandle = linker.downcallHandle(
                symbolLookup.find("blaze_free_template").orElseThrow(),
                freeTemplateDesc
            );
        } catch (Throwable e) {
            throw new RuntimeException("Failed to initialize blaze_free_template handle", e);
        }

        // Create upcall stub for custom resolver
        try {
            FunctionDescriptor resolverDesc = FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS);
            MethodHandle resolverMethod = MethodHandles.lookup().findStatic(
                BlazeWrapper.class,
                "customResolver",
                MethodType.methodType(MemorySegment.class, MemorySegment.class)
            );
            resolverUpcallStub = linker.upcallStub(
                resolverMethod,
                resolverDesc,
                Arena.global()
            );
        } catch (Throwable e) {
            throw new RuntimeException("Failed to create resolver upcall stub", e);
        }
    }
        
    /**
     * Helper to read a null-terminated UTF-8 string from a MemorySegment.
     */
    private static String getNullTerminatedUtf8String(MemorySegment seg) {
        if (seg == null || seg.equals(MemorySegment.NULL)) {
            return null;
        }
        long len = 0;
        while (seg.get(ValueLayout.JAVA_BYTE, len) != 0) {
            len++;
        }
        byte[] buf = new byte[(int) len];
        for (int i = 0; i < len; i++) {
            buf[i] = seg.get(ValueLayout.JAVA_BYTE, i);
        }
        return new String(buf, StandardCharsets.UTF_8);
    }

    private static String readClasspathResource(String resourcePath) {
        // Remove leading slashes for classloader compatibility
        String normalizedPath = resourcePath.replaceFirst("^/+", "");
        
        try (InputStream inputStream = BlazeWrapper.class.getClassLoader()
                .getResourceAsStream(normalizedPath)) {
            
            if (inputStream == null) {
                LOGGER.severe("Classpath resource not found: " + normalizedPath);
                return null;
            }
            
            String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            return content;
        } catch (IOException e) {
            LOGGER.severe("Error reading classpath resource: " + e.getMessage());
            return null;
        }
    }

    // Schema registry for the current resolution context
    private static final ThreadLocal<SchemaRegistry> CURRENT_REGISTRY = new ThreadLocal<>();

    // Set the current registry for schema resolution
    static void setCurrentRegistry(SchemaRegistry registry) {
        CURRENT_REGISTRY.set(registry);
    }
    
    // Remove the current registry from the thread local
    static void clearCurrentRegistry() {
        CURRENT_REGISTRY.remove();
    }
    
    private static MemorySegment customResolver(MemorySegment uriPtrSegment) {
        try {
            // Check if segment is null or NULL
            if (uriPtrSegment == null || uriPtrSegment.equals(MemorySegment.NULL)) {
                LOGGER.warning("Null URI segment received in customResolver");
                return MemorySegment.NULL;
            }
            
            LOGGER.fine("Received URI segment address: " + uriPtrSegment.address());
            
            // Create a confined arena for our temporary allocations
            try (Arena arena = Arena.ofConfined()) {
                // Safely read the null-terminated string from the native memory
                String uri = null;
                try {
                    // Create a safe view of the native memory
                    MemorySegment safeView = uriPtrSegment.reinterpret(Long.MAX_VALUE, arena, null);
                    
                    // Find the null terminator
                    long length = 0;
                    while (safeView.get(ValueLayout.JAVA_BYTE, length) != 0) {
                        length++;
                        if (length > 2048) { // Reasonable limit for URI length
                            break;
                        }
                    }
                    
                    if (length == 0) {
                        LOGGER.warning("Empty string at " + uriPtrSegment.address());
                        return MemorySegment.NULL;
                    }
                    
                    // Copy the bytes to a Java array
                    byte[] bytes = new byte[(int)length];
                    for (int i = 0; i < length; i++) {
                        bytes[i] = safeView.get(ValueLayout.JAVA_BYTE, i);
                    }
                    
                    // Convert to a Java string
                    uri = new String(bytes, StandardCharsets.UTF_8);
                    LOGGER.fine("Resolved URI string: " + uri);
                } catch (Exception e) {
                    LOGGER.severe("Error reading URI string: " + e.getMessage());
                    e.printStackTrace();
                    return MemorySegment.NULL;
                }
                
                // Get the registry from the thread local storage
                SchemaRegistry registry = CURRENT_REGISTRY.get();
                if (uri != null && registry != null && registry.contains(uri)) {
                    LOGGER.fine("Found schema in registry for URI: " + uri);
                    String schemaJson = registry.resolve(uri);
                    return processSchemaJson(schemaJson);
                }

                // Handle different URI schemes
                if (uri != null) {
                    if (uri.startsWith("http://") || uri.startsWith("https://")) {
                        // Existing HTTP handling
                        String schemaJson = fetchRemoteSchema(uri);
                        return processSchemaJson(schemaJson);
                    }
                    else if (uri.startsWith("classpath://")) {
                        // Existing classpath handling
                        String resourcePath = uri.substring("classpath://".length());
                        LOGGER.fine("Resolving classpath resource: " + resourcePath);
                        String schemaJson = readClasspathResource(resourcePath);
                        // Normalize JSON before returning
                        if (schemaJson != null) {
                            schemaJson = schemaJson.trim().replaceFirst("^\\{\\s+", "{");
                        }
                        return processSchemaJson(schemaJson);
                    }
                    else {
                        LOGGER.warning("Unsupported URI scheme or unregistered URI: " + uri);
                    }
                }
            }
            
            return MemorySegment.NULL;
        } catch (Throwable t) {
            LOGGER.severe("Unhandled exception in customResolver: " + t.getMessage());
            t.printStackTrace();
            return MemorySegment.NULL;
        }
    }

    private static MemorySegment processSchemaJson(String schemaJson) {
        if (schemaJson == null) return MemorySegment.NULL;
        
        try {
           
            long size = schemaJson.length() + 1;
            MemorySegment cStringPtr = (MemorySegment) blazeAllocStringHandle.invokeExact(size);
            
            if (cStringPtr == null || cStringPtr.equals(MemorySegment.NULL) || cStringPtr.address() == 0) {
                LOGGER.severe("Memory allocation failed for schema string");
                return MemorySegment.NULL;
            }
            
            MemorySegment cString = cStringPtr.reinterpret(size);
            byte[] schemaBytes = schemaJson.getBytes(StandardCharsets.UTF_8);
            cString.copyFrom(MemorySegment.ofArray(schemaBytes));
            cString.set(ValueLayout.JAVA_BYTE, schemaBytes.length, (byte) 0);
            
            return cStringPtr;
        } catch (Throwable e) {
            LOGGER.severe("Error processing schema: " + e.getMessage());
            e.printStackTrace();
            return MemorySegment.NULL;
        }
    }

    private static String fetchRemoteSchema(String uri) {
        try {
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
                
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .timeout(Duration.ofSeconds(3))
                .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return response.body();
            } else {
                LOGGER.warning("Failed to fetch schema from " + uri + ": HTTP " + response.statusCode());
                return null;
            }
        } catch (Exception e) {
            LOGGER.severe("Error fetching schema from " + uri + ": " + e.getMessage());
            return null;
        }
    }

    static String compile(String schema, String walker, String resolver) {
        try (Arena arena = Arena.ofConfined()) {
            CompiledSchema compiledSchema = compileSchema(schema, arena);
            return "Compilation succeeded";
        } catch (RuntimeException e) {
            return "Error: " + e.getMessage();
        }
    }

    static CompiledSchema compileSchema(String schema, Arena arena) {
        return compileSchema(schema, null, arena, null);
    }

    static CompiledSchema compileSchema(String schema, Arena arena, String defaultDialect) {
        return compileSchema(schema, null, arena, defaultDialect);
    }
    
    static CompiledSchema compileSchema(String schema, SchemaRegistry registry, Arena arena) {
        return compileSchema(schema, registry, arena, null);
    }

    static CompiledSchema compileSchema(String schema, SchemaRegistry registry, Arena arena, String defaultDialect) {
        String walker = "{}";
        
        if (registry != null) {
            setCurrentRegistry(registry);
        }
        
        try {
            MemorySegment schemaSeg = arena.allocateFrom(schema);
            MemorySegment walkerSeg = arena.allocateFrom(walker);
            
            // Use the provided default dialect or null
            MemorySegment dialectSeg = defaultDialect != null ? 
                arena.allocateFrom(defaultDialect) : 
                MemorySegment.NULL;

            long schemaHandle;
            try {
                schemaHandle = (long) blazeCompileHandle.invoke(
                    schemaSeg,
                    walkerSeg,
                    resolverUpcallStub,
                    dialectSeg
                );
            } catch (Throwable e) {
                throw new RuntimeException("Failed to invoke native compile function", e);
            }

            if (schemaHandle == 0) {
                throw new RuntimeException("Schema compilation failed");
            }

            return new CompiledSchemaImpl(schemaHandle);
        } catch (Throwable e) {
            throw new RuntimeException("Unexpected error during schema compilation", e);
        } finally {
            if (registry != null) {
                clearCurrentRegistry();
            }
        }
    }

    static boolean validateInstance(CompiledSchema schema, String instance) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment instanceSeg = arena.allocateFrom(instance);
            long schemaHandle = schema.getHandle();

            try {
                return (boolean) blazeValidateHandle.invoke(schemaHandle, instanceSeg);
            } catch (Throwable e) {
                throw new RuntimeException("Failed to invoke native validate function", e);
            }
        }
    }

    static ValidationResult validateInstanceWithDetails(CompiledSchema schema, String instance) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment instanceSeg = arena.allocateFrom(instance);
            long schemaHandle = schema.getHandle();

            try {
                MemorySegment resultSeg = (MemorySegment) blazeValidateWithOutputHandle.invoke(schemaHandle, instanceSeg);
                if (resultSeg.equals(MemorySegment.NULL)) {
                    throw new RuntimeException("Failed to get validation details");
                }
                
                // Convert the C string to Java string
                String jsonResult = getNullTerminatedUtf8String(resultSeg.reinterpret(Long.MAX_VALUE));
                
                // Free the memory allocated in C++
                blazeFreeJsonHandle.invoke(resultSeg);
                
                return ValidationResult.fromJson(jsonResult);
            } catch (Throwable e) {
                throw new RuntimeException("Failed to invoke detailed validation function", e);
            }
        }
    }

    static void freeCompiledSchema(long schemaHandle) {
        try {
            blazeFreeTemplateHandle.invoke(schemaHandle);
        } catch (Throwable e) {
            LOGGER.warning("Failed to free native template memory: " + e.getMessage());
        }
    }

    private static class CompiledSchemaImpl implements CompiledSchema {
        private final long handle;
        private boolean closed = false;
        private final Cleaner.Cleanable cleanable;

        // State class to hold the resources that need cleanup
        private static class State implements Runnable {
            private final long handle;
            private boolean cleaned = false;

            State(long handle) {
                this.handle = handle;
            }

            @Override
            public void run() {
                if (!cleaned) {
                    LOGGER.fine("Cleaning up schema resources via Cleaner for handle: " + handle);
                    freeCompiledSchema(handle);
                    cleaned = true;
                }
            }
        }

        public CompiledSchemaImpl(long handle) {
            this.handle = handle;
            this.cleanable = cleaner.register(this, new State(handle));
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
                cleanable.clean();
                closed = true;
            }
        }
    }
}