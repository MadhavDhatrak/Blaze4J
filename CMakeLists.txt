cmake_minimum_required(VERSION 3.15)
project(blaze_wrapper)

# Set C++ standard
set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# Add blaze as a submodule
add_subdirectory("${PROJECT_SOURCE_DIR}/deps/blaze")

# Create the shared library
add_library(blaze_wrapper SHARED
    src/main/cpp/blaze_wrapper.cpp)

# Disable strcpy deprecation warnings
target_compile_definitions(blaze_wrapper PRIVATE _CRT_SECURE_NO_WARNINGS)

# Link against blaze libraries
target_link_libraries(blaze_wrapper
    PUBLIC
    sourcemeta::blaze::compiler
    sourcemeta::blaze::evaluator)

# Include directories
target_include_directories(blaze_wrapper
    PUBLIC
    ${PROJECT_SOURCE_DIR}/src/main/cpp)

# Set output directory to match Java's native library path
set_target_properties(blaze_wrapper
    PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY "${PROJECT_SOURCE_DIR}/src/main/resources")