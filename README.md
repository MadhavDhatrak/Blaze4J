# BlazeWrapper

BlazeWrapper is a high-performance Java wrapper for the Blaze JSON Schema validator, providing seamless integration between Java applications and the native C++ Blaze validator. By leveraging Java's Foreign Function & Memory API (FFM), this library offers excellent performance while maintaining memory safety.

The library allows Java developers to validate JSON documents against JSON Schema specifications (supporting multiple draft versions) without the overhead typically associated with cross-language bindings.

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

## Dependencies

- Java Development Kit (JDK) 11 or higher
- CMake 3.10 or higher
- C++ compiler with C++11 support
- Blaze library (included as a Git submodule)

## Build Instructions

### 1. Initialize Blaze Submodule

First, initialize the Blaze library as a Git submodule:

```bash
git submodule add https://github.com/sourcemeta/blaze.git deps/blaze
```

### 2. Build the Project for Windows

Create a build directory and compile the project using CMake:

```bash
mkdir build
cmake -Bbuild -G "Visual Studio 17 2022" -DCMAKE_BUILD_TYPE=Release
cmake --build build --config Release
```

### 3. Build the Project for Linux

Create a build directory and compile the project using CMake:

```bash
mkdir -p build-linux
cd build-linux
cmake .. -DCMAKE_BUILD_TYPE=Release
make 
```

### 4. Build the Project for macOS

Create a build directory and compile the project using CMake:

```bash
mkdir -p build-mac
cd build-mac
cmake .. -DCMAKE_BUILD_TYPE=Release
cmake --build . --config Release
```

This will generate platform-specific shared libraries (.dll on Windows, .so on Linux and .dylib for macOS) in the build directory.

### Testing 

First you need to have the JSON Schema Test Suite as a submodule:
```bash
git submodule add https://github.com/json-schema-org/JSON-Schema-Test-Suite.git src/test/resources/JSON-Schema-Test-Suite
```

For running the Test Suite Drafts, follow these commands:

1. Draft2020-12
```bash
mvn test -Dtest=Draft2020Runner
```

2. Draft2019
```bash
mvn test -Dtest=Draft2019Runner
```

3. Draft7
```bash
mvn test -Dtest=Draft7Runner
```

4. Draft6 
```bash
mvn test -Dtest=Draft6Runner
```

5. Draft4 
```bash
mvn test -Dtest=Draft4Runner
```

## Build Configuration

- The project uses Maven for Java build management
- CMake is used for building the native C++ components
- Native libraries are built into the `build/` directory
