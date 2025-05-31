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
std::optional<sourcemeta::core::JSON> custom_resolver_wrapper(const std::string &uri) {
    if (current_custom_resolver != nullptr) {
        const char* result = current_custom_resolver(uri.c_str());
        if (result != nullptr) {
            std::string result_str(result);
            blaze_free_string((char*)result); 
            try {
                return sourcemeta::core::parse_json(result_str);
            } catch (...) {
                
            }
        }
    }
    return sourcemeta::core::schema_official_resolver(uri);
}

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
                if (current_custom_resolver != nullptr) {
                    std::string uri_for_java(uri_sv); 
                    std::cerr << "Attempting to resolve URI (via Java): " << uri_for_java << std::endl;
                    
                   
                    const char* java_result_c_str = current_custom_resolver(uri_for_java.c_str());
                    
                    if (java_result_c_str != nullptr) {
                        std::string java_result_str(java_result_c_str); 
                        std::cerr << "Got result from Java resolver (first 100 chars): " << java_result_str.substr(0, 100) << std::endl;
                        blaze_free_string((char*)java_result_c_str); 
                        
                        try {
                            return sourcemeta::core::parse_json(java_result_str);
                        } catch (const std::exception& e) {
                            std::cerr << "Error parsing JSON from Java resolver: " << e.what() << std::endl;
                            
                        }
                    } else {
                        std::cerr << "Java resolver returned NULL for URI: " << uri_for_java << std::endl;
                        // Fall through to standard resolver
                    }
                } else {
                    std::cerr << "No custom resolver set, using standard resolver for URI: " << std::string(uri_sv) << std::endl;
                }
                
                // Fall back to the standard resolver if Java fails or returns NULL
                return sourcemeta::core::schema_official_resolver(uri_sv);
            };

            std::cerr << "Creating compiler instance" << std::endl;
            auto compiler = sourcemeta::blaze::default_schema_compiler;

            std::cerr << "Compiling schema" << std::endl;
            auto compiled = sourcemeta::blaze::compile(
                json_schema,
                walker_obj,
                resolver_obj,
                compiler,
                sourcemeta::blaze::Mode::FastValidation
            );
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