# Installation and Local Development

This guide explains how to **build Blaze4J from source** and run the official JSON Schema Test Suite for all supported drafts.

---

## Table of Contents

- [Prerequisites](#prerequisites)
- [1. Initialize Blaze Submodule](#1-initialize-blaze-submodule)
- [2. Build Native Libraries](#2-build-native-libraries)
  - [Windows](#windows)
  - [Linux](#linux)
  - [macOS](#macos)
- [3. JSON Schema Test Suite (Drafts)](#3-json-schema-test-suite-drafts)
  - [A. Add Test Suite Submodule](#a-add-test-suite-submodule)
  - [B. Run Draft Test Runners](#b-run-draft-test-runners)
- [Notes](#notes)

---

## Prerequisites

Make sure you have the following tools installed:

- **Java Development Kit (JDK) 11** or higher
- **CMake 3.10** or higher
- **C++ compiler** with C++11 support
- **Git** (for submodules)

---

## 1. Initialize Blaze Submodule

Clone the repository
```
git clone https://github.com/MadhavDhatrak/Blaze4J.git
```

 then run:
```bash
git submodule add https://github.com/sourcemeta/blaze.git deps/blaze
```

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

Add the official JSON Schema Test Suite:
```
git submodule add https://github.com/json-schema-org/JSON-Schema-Test-Suite.git src/test/resources/JSON-Schema-Test-Suite
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

## Notes

- For full usage instructions and API documentation, see [README.md](./README.md).
- For troubleshooting or contributions, open an issue or pull request on GitHub.
