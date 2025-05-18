#include <sourcemeta/blaze/compiler.h>
#include <sourcemeta/blaze/evaluator.h>
#include <sourcemeta/core/json.h>
#include <sourcemeta/core/jsonschema.h>
#include <string>
#include <cstring>
#include <sstream>
#include <iostream>
#include <cstdint>

extern "C" {

#ifdef _WIN32
__declspec(dllexport)
#endif
int64_t blaze_compile(const char* schema, const char* walker, const char* resolver) {
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
            auto resolver_obj = sourcemeta::core::schema_official_resolver;

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

            auto* template_ptr = new sourcemeta::blaze::Template(compiled);
            std::cerr << "Created persistent Template object at address: " << (void*)template_ptr << std::endl;
            
            // Return the pointer as a long integer
            return reinterpret_cast<int64_t>(template_ptr);
        } catch (const std::exception& internal_e) {
            std::cerr << "Internal error during compilation: " << internal_e.what() << std::endl;
            throw; 
        }
    } catch (const std::exception& e) {
        
        std::string error_msg = std::string("Error: ") + e.what();
        std::cerr << "Compilation error: " << error_msg << std::endl;
        return 0; 
    } catch (...) {
        // Handle unknown exceptions
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