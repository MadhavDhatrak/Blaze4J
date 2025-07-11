cmake_minimum_required(VERSION 3.15)
project(blaze4j)

# Set C++ standard
set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# Set platform-specific output directories
if(WIN32)
    # Windows: put DLL in build/bin/Release
    set(CMAKE_RUNTIME_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/bin")
    set(CMAKE_RUNTIME_OUTPUT_DIRECTORY_DEBUG "${CMAKE_BINARY_DIR}/bin/Debug")
    set(CMAKE_RUNTIME_OUTPUT_DIRECTORY_RELEASE "${CMAKE_BINARY_DIR}/bin/Release")
    set(CMAKE_LIBRARY_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/bin")
    set(CMAKE_LIBRARY_OUTPUT_DIRECTORY_DEBUG "${CMAKE_BINARY_DIR}/bin/Debug")
    set(CMAKE_LIBRARY_OUTPUT_DIRECTORY_RELEASE "${CMAKE_BINARY_DIR}/bin/Release")
elseif(APPLE)
    # Mac: put dylib in build-mac/bin
    set(CMAKE_LIBRARY_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/bin")
else()
    # Linux: put .so in build-linux/lib
    set(CMAKE_LIBRARY_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/lib")
endif()

# Add blaze as a submodule
add_subdirectory("${PROJECT_SOURCE_DIR}/deps/blaze")

# Create the shared library
add_library(blaze4j SHARED
    src/main/cpp/blaze_wrapper.cpp)

# No need to redefine BLAZE_EXPORT here; the macro is set in the source file with proper platform guards.

# Disable strcpy deprecation warning for MSVC
if(MSVC)
    target_compile_definitions(blaze4j PRIVATE _CRT_SECURE_NO_WARNINGS)
endif()

# Link against blaze libraries
target_link_libraries(blaze4j
    PUBLIC
    sourcemeta::blaze::compiler
    sourcemeta::blaze::evaluator)

# Include directories
target_include_directories(blaze4j
    PUBLIC
    ${PROJECT_SOURCE_DIR}/src/main/cpp)
