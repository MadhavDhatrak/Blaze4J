#include <sourcemeta/blaze/compiler.h>
#include <sourcemeta/blaze/evaluator.h>
#include <sourcemeta/core/json.h>
#include <sourcemeta/core/jsonschema.h>
#include <string>
#include <cstring>
#include <sstream>
#include <iostream>
#include <cstdint>
#include <cstdlib>
#include <optional>
#include <mutex>
#include <unordered_map>
#include <memory>


extern "C" {
#ifdef _WIN32
__declspec(dllexport)
#endif
void blaze_free_string(char* ptr);
}


thread_local const char* (*current_custom_resolver)(const char*) = nullptr;

extern "C" {

#ifdef _WIN32
__declspec(dllexport)
#endif
char* blaze_alloc_string(size_t size) {
    return static_cast<char*>(malloc(size));
}

#ifdef _WIN32
__declspec(dllexport)
#endif
void blaze_free_string(char* ptr) {
    free(ptr);
}

#ifdef _WIN32
__declspec(dllexport)
#endif
int64_t blaze_compile(const char* schema, const char* walker, const char* (*custom_resolver)(const char*)) {
    try {
        if (schema == nullptr) {
            std::cerr << "Error: Schema is null" << std::endl;
            throw std::runtime_error("Schema is null");
        }

        std::cerr << "Received schema: " << schema << std::endl;
        
        std::string schema_str(schema);

        try {
            // Parse JSON schema
            std::cerr << "Attempting to parse JSON schema" << std::endl;
            auto json_schema = sourcemeta::core::parse_json(schema_str);
            std::cerr << "Successfully parsed JSON schema" << std::endl;

            std::cerr << "Setting up walker and resolver" << std::endl;
            auto walker_obj = sourcemeta::core::schema_official_walker;
            
            current_custom_resolver = custom_resolver;
            
           
            auto resolver_obj = [](std::string_view uri_sv) -> std::optional<sourcemeta::core::JSON> {
                std::string uri(uri_sv);
                std::cerr << "[DEBUG blaze_wrapper] Resolver: Attempting to resolve URI: " << uri << std::endl;
                
                // First try built-in official resolver
                auto official_result = sourcemeta::core::schema_official_resolver(uri_sv);
                if (official_result.has_value()) {
                    std::cerr << "[DEBUG blaze_wrapper] Resolver: Found in built-in resolver for URI: " << uri << std::endl;
                    
                    return official_result;
                }
                
                std::cerr << "[DEBUG blaze_wrapper] Resolver: Not found in built-in resolver for URI: " << uri << std::endl;
                
                // Then try custom resolver (Java) if available
                if (current_custom_resolver != nullptr) {
                    std::cerr << "[DEBUG blaze_wrapper] Resolver: Attempting custom resolver for URI: " << uri << std::endl;
                    const char* result_c_str = current_custom_resolver(uri.c_str());
                    
                    if (result_c_str != nullptr) {
                        std::string result_str(result_c_str);
                        // DO NOT free result_c_str here; keeping the memory alive until after compilation to avoid invalid memory access
                        // blaze_free_string((char*)result_c_str);
                        
                        try {
                            std::cerr << "[DEBUG blaze_wrapper] Resolver: Successfully retrieved schema string from custom resolver for URI: " << uri << ". Schema (first 100 chars): " << result_str.substr(0, 100) << (result_str.length() > 100 ? "..." : "") << std::endl;
                            auto parsed_json = sourcemeta::core::parse_json(result_str);
                            std::cerr << "[DEBUG blaze_wrapper] Resolver: Returning successfully parsed custom schema for URI: " << uri << std::endl;
                            return parsed_json;
                        } catch (const std::exception& e) {
                            std::cerr << "[ERROR blaze_wrapper] Resolver: Error parsing JSON from custom resolver for URI: " << uri << " - " << e.what() << std::endl;
                            // DO NOT free result_c_str here; keeping the memory alive until after compilation to avoid invalid memory access
                            // blaze_free_string((char*)result_c_str);
                        }
                    } else {
                        std::cerr << "[DEBUG blaze_wrapper] Resolver: Custom resolver returned NULL for URI: " << uri << std::endl;
                    }
                }
                
                std::cerr << "[DEBUG blaze_wrapper] Resolver: Returning nullopt (no schema found) for URI: " << uri << std::endl;
                return std::nullopt;
            };

            std::cerr << "Creating compiler instance" << std::endl;
            auto compiler = sourcemeta::blaze::default_schema_compiler;

            std::cerr << "[DEBUG blaze_wrapper] Attempting to compile main schema. Schema (first 200 chars): " << schema_str.substr(0, 200) << (schema_str.length() > 200 ? "..." : "") << std::endl;
            // Potentially log info about json_schema if API allows, e.g., json_schema.type_name()
            std::cerr << "[DEBUG blaze_wrapper] Calling sourcemeta::blaze::compile..." << std::endl;
            auto compiled = sourcemeta::blaze::compile(
                json_schema,
                walker_obj,
                resolver_obj,
                compiler,
                sourcemeta::blaze::Mode::FastValidation
            );
            std::cerr << "[DEBUG blaze_wrapper] sourcemeta::blaze::compile finished successfully." << std::endl;
            std::cerr << "Schema compiled successfully" << std::endl;

            // Reset the custom resolver
            current_custom_resolver = nullptr;

            auto* template_ptr = new sourcemeta::blaze::Template(compiled);
            std::cerr << "Created persistent Template object at address: " << (void*)template_ptr << std::endl;
            
            // Return the pointer as a long integer
            return reinterpret_cast<int64_t>(template_ptr);
        } catch (const std::exception& internal_e) {
            current_custom_resolver = nullptr; // Clean up on error
            std::cerr << "Internal error during compilation: " << internal_e.what() << std::endl;
            throw; 
        }
    } catch (const std::exception& e) {
        current_custom_resolver = nullptr; // Clean up on error
        std::string error_msg = std::string("Error: ") + e.what();
        std::cerr << "Compilation error: " << error_msg << std::endl;
        return 0; 
    } catch (...) {
        current_custom_resolver = nullptr; // Clean up on error
        std::cerr << "Unknown error during compilation" << std::endl;
        return 0; 
    }
}

#ifdef _WIN32
__declspec(dllexport)
#endif
bool blaze_validate(int64_t schemaHandle, const char* instance) {
    try {
        if (instance == nullptr) {
            std::cerr << "Error: Instance is null" << std::endl;
            return false;
        }

        if (schemaHandle == 0) {
            std::cerr << "Error: Invalid schema handle" << std::endl;
            return false;
        }

        std::string instance_str(instance);
        auto json_instance = sourcemeta::core::parse_json(instance_str);

        sourcemeta::blaze::Evaluator evaluator;

        auto* schema_template = reinterpret_cast<sourcemeta::blaze::Template*>(schemaHandle);
        if (schema_template == nullptr) {
            std::cerr << "Error: Schema template is null" << std::endl;
            return false;
        }

        std::cerr << "Validating instance against schema template at address: " << (void*)schema_template << std::endl;
        return evaluator.validate(*schema_template, json_instance);
    } catch (const std::exception& e) {
        std::cerr << "Error during validation: " << e.what() << std::endl;
        return false;
    } catch (...) {
        std::cerr << "Unknown error during validation" << std::endl;
        return false;
    }
}


#ifdef _WIN32
__declspec(dllexport)
#endif
void blaze_free_template(int64_t schemaHandle) {
    if (schemaHandle != 0) {
        auto* template_ptr = reinterpret_cast<sourcemeta::blaze::Template*>(schemaHandle);
        std::cerr << "Freeing Template object at address: " << (void*)template_ptr << std::endl;
        delete template_ptr;
    }
}


#ifdef _WIN32
__declspec(dllexport)
#endif
void blaze_free_result(const char* result) {
    if (result) {
        delete[] result;
    }
}

}