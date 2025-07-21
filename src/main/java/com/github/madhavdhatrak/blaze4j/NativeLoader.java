package com.github.madhavdhatrak.blaze4j;

import java.io.*;
import java.nio.file.*;

public class NativeLoader {
    public static void loadLibrary(String libName) throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();
        String platform;

        if (os.contains("win")) {
            platform = "windows-amd64";
            libName += ".dll";
        } else if (os.contains("mac")) {
            platform = "macos-amd64";
            if (libName.startsWith("lib")) {
                libName += ".dylib";
            } else {
                libName = "lib" + libName + ".dylib";
            }
        } else if (os.contains("wsl") || os.contains("linux")) {
            platform = "linux-amd64";
            if (libName.startsWith("lib")) {
                libName += ".so";
            } else {
                libName = "lib" + libName + ".so";
            }
        } else {
            throw new UnsupportedOperationException("Unsupported OS: " + os);
        }

        String resourcePath = "native/" + platform + "/" + libName;
         
        try (InputStream in = NativeLoader.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new FileNotFoundException("Native library not found: " + resourcePath);
            }

            File tempFile = File.createTempFile("native-", libName);
            tempFile.deleteOnExit();
            Files.copy(in, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.load(tempFile.getAbsolutePath());
        }
    }
} 