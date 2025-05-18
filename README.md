# BlazeWrapper

A Java wrapper for the Blaze library using FFM 

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

### 2. Build the Project

Create a build directory and compile the project using CMake:

```bash
mkdir build
cd build
cmake ..
cmake --build .
```

This will generate platform-specific shared libraries (.dll on Windows, .so on Linux) in the build directory.

## Build Configuration

- The project uses Maven for Java build management
- CMake is used for building the native C++ components
- Native libraries are built into the `build/` directory