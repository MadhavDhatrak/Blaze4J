# Contributing to Blaze4J

This guide explains how to contribute to Blaze4J, including how to set up your development environment, build the project from source, and run tests.

---

## Table of Contents

- [Prerequisites](#prerequisites)
- [1. Clone and Initialize Submodules](#1-clone-and-initialize-submodules)
- [2. Build Native Libraries](#2-build-native-libraries)
  - [Windows](#windows)
  - [Linux](#linux)
  - [macOS](#macos)
- [3. JSON Schema Test Suite (Drafts)](#3-json-schema-test-suite-drafts)
  - [A. Add Test Suite Submodule](#a-add-test-suite-submodule)
  - [B. Run Draft Test Runners](#b-run-draft-test-runners)
- [Pull Request Guidelines](#pull-request-guidelines)
- [Notes](#notes)

---

## Prerequisites

Make sure you have the following tools installed:

- **Java Development Kit (JDK) 21** or higher
- **CMake 3.10** or higher
- **C++ compiler** with C++11 support
- **Git** (for submodules)

---

## 1. Clone and Initialize Submodules

Blaze4J uses Git submodules to include the Blaze library. You can clone the repository and all its submodules in a single command:

```bash
git clone https://github.com/MadhavDhatrak/Blaze4J.git
cd Blaze4J
```

Add blaze as a Submodule:
Run this command:

```bash
git submodule add https://github.com/sourcemeta/blaze.git deps/blaze
```

This will ensure you have the exact version of the Blaze library that Blaze4J is designed to work with.

## 2. Build Native Libraries

> The resulting shared libraries (\*.dll / \*.so / \*.dylib) will be in your build directory.

### Windows
```java
mkdir build
cmake -Bbuild -G "Visual Studio 17 2022" -DCMAKE_BUILD_TYPE=Release
cmake --build build --config Release
```
### Linux
```java
mkdir -p build-linux
cd build-linux
cmake .. -DCMAKE_BUILD_TYPE=Release
make
```

### macOS
```java
mkdir -p build-mac
cd build-mac
cmake .. -DCMAKE_BUILD_TYPE=Release
cmake --build . --config Release
```

## 3. JSON Schema Test Suite (Drafts)

### A. Add Test Suite Submodule

If the JSON Schema Test Suite submodule is not already initialized (it should be if you used `--recurse-submodules` when cloning), you can add it with:

```bash
git submodule add https://github.com/json-schema-org/JSON-Schema-Test-Suite.git src/test/resources/JSON-Schema-Test-Suite
```

Or update it if it already exists:

```bash
git submodule update --init --recursive src/test/resources/JSON-Schema-Test-Suite
```

### B. Run Draft Test Runners

Run your Maven test runner for each draft:
```java
mvn test -Dtest=Draft2020Runner
mvn test -Dtest=Draft2019Runner
mvn test -Dtest=Draft7Runner
mvn test -Dtest=Draft6Runner
mvn test -Dtest=Draft4Runner
```

## Pull Request Guidelines

- Ensure your changes are well-documented and follow the existing code style.
- Run the test suite to ensure your changes do not break existing functionality.
- Update the documentation if your changes affect the user-facing API.

## Notes

- For full usage instructions and API documentation, see [README.md](./README.md).
- For troubleshooting or contributions, open an issue or pull request on GitHub.
