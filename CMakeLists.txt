cmake_minimum_required(VERSION 3.15)
project(blaze4j)

# Set C++ standard
set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# Set Java resources native directory paths
set(JAVA_RESOURCES_DIR "${CMAKE_SOURCE_DIR}/src/main/resources/native")

# Set platform-specific output directories
if(WIN32)
    # Windows: put DLL in Java resources directory for windows-amd64
    set(PLATFORM_NATIVE_DIR "${JAVA_RESOURCES_DIR}/windows-amd64")
    set(CMAKE_RUNTIME_OUTPUT_DIRECTORY "${PLATFORM_NATIVE_DIR}")
    set(CMAKE_RUNTIME_OUTPUT_DIRECTORY_DEBUG "${PLATFORM_NATIVE_DIR}")
    set(CMAKE_RUNTIME_OUTPUT_DIRECTORY_RELEASE "${PLATFORM_NATIVE_DIR}")
    set(CMAKE_LIBRARY_OUTPUT_DIRECTORY "${PLATFORM_NATIVE_DIR}")
    set(CMAKE_LIBRARY_OUTPUT_DIRECTORY_DEBUG "${PLATFORM_NATIVE_DIR}")
    set(CMAKE_LIBRARY_OUTPUT_DIRECTORY_RELEASE "${PLATFORM_NATIVE_DIR}")
elseif(APPLE)
    # Mac: put dylib in Java resources directory for macos-amd64
    set(PLATFORM_NATIVE_DIR "${JAVA_RESOURCES_DIR}/macos-amd64")
    set(CMAKE_LIBRARY_OUTPUT_DIRECTORY "${PLATFORM_NATIVE_DIR}")
else()
    # Linux: put .so in Java resources directory for linux-amd64
    set(PLATFORM_NATIVE_DIR "${JAVA_RESOURCES_DIR}/linux-amd64")
    set(CMAKE_LIBRARY_OUTPUT_DIRECTORY "${PLATFORM_NATIVE_DIR}")
endif()

# Create necessary directories
file(MAKE_DIRECTORY ${PLATFORM_NATIVE_DIR})

# Add blaze as a submodule
add_subdirectory("${PROJECT_SOURCE_DIR}/deps/blaze")

# Create the shared library
add_library(blaze4j SHARED
    src/main/cpp/blaze_wrapper.cpp)

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
