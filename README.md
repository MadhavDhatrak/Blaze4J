# BlazeWrapper

**BlazeWrapper** is a high-performance Java wrapper for the Blaze JSON Schema validator, providing seamless integration between Java applications and the native C++ Blaze validator. By leveraging Java's Foreign Function & Memory API (FFM), this library offers excellent performance while maintaining memory safety.

BlazeWrapper enables Java developers to validate JSON documents against JSON Schema specifications (supporting multiple draft versions) with minimal overhead and maximum efficiency.

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Supported Drafts](#supported-drafts) 
- [Supported Resolvers](#supported-resolvers) 
- [Project Structure](#project-structure)
- [Dependencies](#dependencies)
- [Build Instructions](#build-instructions)
  - [Initialize Blaze Submodule](#1-initialize-blaze-submodule)
  - [Build for Windows](#2-build-for-windows)
  - [Build for Linux](#3-build-for-linux)
  - [Build for macOS](#4-build-for-macos)
- [Testing](#testing)
- [Usage](#usage)
- [API Documentation](#api-documentation)
  - [Compiler API](./docs/compiler.md)
  - [Validator API](./docs/validator.md)
- [Best Practices](#best-practices)
- [Contributing](#contributing)
- [Credits](#credits)

---

## Overview

BlazeWrapper bridges Java and native C++ for **fast, standards-compliant JSON Schema validation**. It supports multiple schema drafts, efficient memory management via the FFM API, and robust error reporting.

---

## Features

- **High-performance**: Native C++ validation with minimal Java overhead.
- **Multi-draft support**: Compatible with JSON Schema Drafts 4, 6, 7, 2019-09, and 2020-12.
- **Memory safety**: Uses Java's Foreign Function & Memory API (FFM) for safe, efficient resource management.
- **Detailed error reporting**: Get precise validation errors and paths.
- **Easy integration**: Simple API for compiling schemas and validating instances.
- **Cross-platform**: Works on Windows, Linux, and macOS.

---

## Supported Drafts 

| Draft Version | Status        | Specification                                                                 |
|---------------|--------------|-------------------------------------------------------------------------------|
| Draft 2020-12 | ✅ Supported | [Spec](https://json-schema.org/draft/2020-12/release-notes.html)              |
| Draft 2019-09 | ✅ Supported | [Spec](https://json-schema.org/draft/2019-09/release-notes.html)              |
| Draft 7       | ✅ Supported | [Spec](https://json-schema.org/draft-07/release-notes.html)                   |
| Draft 6       | ✅ Supported | [Spec](https://json-schema.org/draft-06/release-notes.html)                   |
| Draft 4       | ✅ Supported | [Spec](https://json-schema.org/draft-04/release-notes.html)                   |


---
<a name="supported-resolvers"></a>
## Supported `$ref` Resolvers:

| Resolver Type | Status        | Description                                   |
|---------------|---------------|-----------------------------------------------|
| `http`        | ✅ Supported  | Resolves external schemas via HTTP(S) URLs    |
| `classpath`   | ✅ Supported  | Resolves schemas from the Java classpath      |

---

## Project Structure
```
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/sourcemeta/blaze/
│   │   │       ├── CompiledSchema.java
│   │   │       ├── BlazeWrapper.java
│   │   │       ├── BlazeValidator.java
│   │   │       └── Blaze.java
│   │   └── cpp/
│   │       └── blaze_wrapper.cpp
│   └── test/
│       └── java/
│           └── com/sourcemeta/blaze/wrapper/
│               └── BlazeWrapperTest.java
├── deps/
│   └── blaze/          # Git submodule for Blaze library
├── build/              # Build output directory (contains platform-specific shared libraries)
├── pom.xml            # Maven build configuration
└── CMakeLists.txt     # CMake configuration

```

---

## Dependencies

- Java Development Kit (JDK) 11 or higher
- CMake 3.10 or higher
- C++ compiler with C++11 support
- Blaze library (included as a Git submodule)

---

## Build Instructions

### 1. Initialize Blaze Submodule
```
git submodule add https://github.com/sourcemeta/blaze.git deps/blaze
```

### 2. Build for Windows
```java
mkdir build
cmake -Bbuild -G "Visual Studio 17 2022" -DCMAKE_BUILD_TYPE=Release
cmake --build build --config Release
```
### 3. Build for Linux
```java
mkdir -p build-linux
cd build-linux
cmake .. -DCMAKE_BUILD_TYPE=Release
make
```
### 4. Build for macOS
```
mkdir -p build-mac
cd build-mac
cmake .. -DCMAKE_BUILD_TYPE=Release
cmake --build . --config Release
```
This will generate platform-specific shared libraries (`.dll` on Windows, `.so` on Linux, `.dylib` for macOS) in the build directory.

---

## Testing

First, add the JSON Schema Test Suite as a submodule:
```
git submodule add https://github.com/json-schema-org/JSON-Schema-Test-Suite.git src/test/resources/JSON-Schema-Test-Suite
```

**Run the test suite for each draft:**
```bash
mvn test -Dtest=Draft2020Runner 
```
```bash
mvn test -Dtest=Draft2019Runner 
```
```bash
mvn test -Dtest=Draft7Runner 
```
```bash
mvn test -Dtest=Draft6Runner 
```
```bash
mvn test -Dtest=Draft4Runner 
```
---

## Usage

```java

String schemaJson = "{"
    + "\"$schema\": \"https://json-schema.org/draft/2020-12/schema\","
    + "\"type\": \"string\""
    + "}";

String instance = "\"hello\"";

try (CompiledSchema compiledSchema = Blaze.compile(schemaJson)) {
    ValidationResult result = Blaze.validateWithDetails(compiledSchema, instance);

    System.out.println("Is valid: " + result.isValid());

    if (!result.isValid()) {
        result.getErrors().forEach(error -> {
            System.out.println("- message      : " + error.getMessage());
            System.out.println("  instancePath : " + error.getInstancePath());
            System.out.println("  schemaPath   : " + error.getSchemaPath());
        });
    }
}

```
---

## API Documentation

- [Compiler API](./docs/Compiler.md)- How to compile schemas, manage memory, and use different dialects.
- [Validator API](./docs/Validator.md)-How to validate instances, get boolean or detailed results, and interpret errors.

---

## Best Practices

- Use try-with-resources for all `CompiledSchema` and `Arena` objects to ensure proper cleanup.
- Prefer detailed validation (`validateWithDetails`) for debugging and error reporting.
- Always specify a `$schema` or default dialect for maximum compatibility.

---

## Contributing

Feel free to open an issue for bugs or feature requests, or submit a pull request to improve the project. Whether it’s fixing a typo, improving docs, or adding new features—every contribution counts!

## Credits

- [Blaze](https://github.com/sourcemeta/blaze) — Native JSON Schema validator
- [JSON Schema Test Suite](https://github.com/json-schema-org/JSON-Schema-Test-Suite)

