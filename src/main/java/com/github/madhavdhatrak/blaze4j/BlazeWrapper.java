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

class BlazeWrapper {
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

    static {
        try {
            // Try to load the library using its base name first
            System.loadLibrary("blaze4j");
        } catch (UnsatisfiedLinkError e) {
            // If that fails, try platform-specific paths
            String osName = System.getProperty("os.name").toLowerCase();
            String userDir = System.getProperty("user.dir").replace("\\", "/");
            String libPath;
            
            if (osName.contains("win")) {
                // Windows path
                libPath = userDir + "/build/bin/Release/blaze4j.dll";
            } else if (osName.contains("mac")) {
                // Mac path
                libPath = userDir + "/build-mac/bin/blaze4j.dylib";
            } else {
                // Linux/WSL path
                libPath = userDir + "/build-linux/lib/libblaze4j.so";
                
                // Check if file exists, if not try alternative location
                if (!new File(libPath).exists()) {
                    libPath = userDir + "/build/libblaze4j.so";
                }
            }
            
            System.out.println("Attempting to load library from: " + libPath);
            System.load(libPath);
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
    
    // Dummy malloc for testing purposes
    private static MemorySegment dummyMalloc(long size) {
        // For testing, just use an arena to allocate memory
        MemorySegment segment = Arena.global().allocate(size);
        System.out.println("Using dummy malloc: " + size + " bytes");
        return segment;
    }
    
    // Dummy free for testing purposes
    private static void dummyFree(MemorySegment segment) {
        // Nothing to do for dummy free
        System.out.println("Using dummy free");
    }

    private static String readClasspathResource(String resourcePath) {
        // Remove leading slashes for classloader compatibility
        String normalizedPath = resourcePath.replaceFirst("^/+", "");
        
        try (InputStream inputStream = BlazeWrapper.class.getClassLoader()
                .getResourceAsStream(normalizedPath)) {
            
            if (inputStream == null) {
                System.err.println("Classpath resource not found: " + normalizedPath);
                return null;
            }
            
            String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            return content;
        } catch (IOException e) {
            System.err.println("Error reading classpath resource: " + e.getMessage());
            return null;
        }
    }

    private static MemorySegment customResolver(MemorySegment uriPtrSegment) {
        try {
            // Check if segment is null or NULL
            if (uriPtrSegment == null || uriPtrSegment.equals(MemorySegment.NULL)) {
                System.err.println("Warning: Null URI segment received in customResolver");
                return MemorySegment.NULL;
            }
            
            System.out.println("Received URI segment address: " + uriPtrSegment.address());
            
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
                        System.err.println("Warning: Empty string at " + uriPtrSegment.address());
                        return MemorySegment.NULL;
                    }
                    
                    // Copy the bytes to a Java array
                    byte[] bytes = new byte[(int)length];
                    for (int i = 0; i < length; i++) {
                        bytes[i] = safeView.get(ValueLayout.JAVA_BYTE, i);
                    }
                    
                    // Convert to a Java string
                    uri = new String(bytes, StandardCharsets.UTF_8);
                    System.out.println("Resolved URI string: " + uri);
                } catch (Exception e) {
                    System.err.println("Error reading URI string: " + e.getMessage());
                    e.printStackTrace();
                    return MemorySegment.NULL;
                }
                
                // Handle different URI schemes
                if (uri != null) {
                    if (uri.startsWith("http://") || uri.startsWith("https://")) {
                        // Existing HTTP handling
                        String schemaJson = fetchRemoteSchema(uri);
                        return processSchemaJson(schemaJson);
                    }
                    else if (uri.startsWith("classpath://")) {
                        // New classpath handling
                        String resourcePath = uri.substring("classpath://".length());
                        System.out.println("Resolving classpath resource: " + resourcePath);
                        String schemaJson = readClasspathResource(resourcePath);
                        // Normalize JSON before returning
                        schemaJson = schemaJson.trim().replaceFirst("^\\{\\s+", "{");
                        return processSchemaJson(schemaJson);
                    }
                    else {
                        System.err.println("Unsupported URI scheme: " + uri);
                    }
                }
            }
            
            return MemorySegment.NULL;
        } catch (Throwable t) {
            System.err.println("Unhandled exception in customResolver: " + t.getMessage());
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
                System.err.println("Memory allocation failed for schema string");
                return MemorySegment.NULL;
            }
            
            MemorySegment cString = cStringPtr.reinterpret(size);
            byte[] schemaBytes = schemaJson.getBytes(StandardCharsets.UTF_8);
            cString.copyFrom(MemorySegment.ofArray(schemaBytes));
            cString.set(ValueLayout.JAVA_BYTE, schemaBytes.length, (byte) 0);
            
            return cStringPtr;
        } catch (Throwable e) {
            System.err.println("Error processing schema: " + e.getMessage());
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
                System.err.println("Failed to fetch schema from " + uri + ": HTTP " + response.statusCode());
                return null;
            }
        } catch (Exception e) {
            System.err.println("Error fetching schema from " + uri + ": " + e.getMessage());
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
        return compileSchema(schema, arena, null);
    }

    static CompiledSchema compileSchema(String schema, Arena arena, String defaultDialect) {
        String walker = "{}";
        try {
            MemorySegment schemaSeg = arena.allocateUtf8String(schema);
            MemorySegment walkerSeg = arena.allocateUtf8String(walker);
            
            // Use the provided default dialect or null
            MemorySegment dialectSeg = defaultDialect != null ? 
                arena.allocateUtf8String(defaultDialect) : 
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
        }
    }

    static boolean validateInstance(CompiledSchema schema, String instance) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment instanceSeg = arena.allocateUtf8String(instance);
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
            MemorySegment instanceSeg = arena.allocateUtf8String(instance);
            long schemaHandle = schema.getHandle();

            try {
                MemorySegment resultSeg = (MemorySegment) blazeValidateWithOutputHandle.invoke(schemaHandle, instanceSeg);
                if (resultSeg.equals(MemorySegment.NULL)) {
                    throw new RuntimeException("Failed to get validation details");
                }
                
                // Convert the C string to Java string
                String jsonResult = resultSeg.reinterpret(Long.MAX_VALUE).getUtf8String(0);
                
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
            System.err.println("Warning: Failed to free native template memory: " + e.getMessage());
        }
    }

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
                System.err.println("Warning: CompiledSchema was not closed properly.");
                close();
            }
            super.finalize();
        }
    }
}