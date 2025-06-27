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

// Portable export macro
#if defined(_WIN32) || defined(_WIN64)
  #define BLAZE_EXPORT __declspec(dllexport)
#else
  // Define visibility default without direct attribute syntax
  #define BLAZE_VISIBILITY __attribute__((visibility("default")))
  #define BLAZE_EXPORT BLAZE_VISIBILITY
#endif

// Resolver pointer
thread_local const char* (*current_custom_resolver)(const char*) = nullptr;

extern "C" {

BLAZE_EXPORT char* blaze_alloc_string(size_t size) {
    return static_cast<char*>(malloc(size));
}

BLAZE_EXPORT void blaze_free_string(char* ptr) {
    free(ptr);
}

BLAZE_EXPORT int64_t blaze_compile(const char* schema, const char* walker, const char* (*custom_resolver)(const char*), const char* default_dialect) {
    try {
        if (schema == nullptr) {
            std::cerr << "Error: Schema is null" << std::endl;
            throw std::runtime_error("Schema is null");
        }

        std::cerr << "Received schema: " << schema << std::endl;
        std::string schema_str(schema);
        
        // Process default dialect
        std::optional<std::string> dialect_opt = std::nullopt;
        if (default_dialect != nullptr && strlen(default_dialect) > 0) {
            dialect_opt = std::string(default_dialect);
            std::cerr << "Using default dialect: " << *dialect_opt << std::endl;
        }

        try {
            std::cerr << "Attempting to parse JSON schema" << std::endl;
            auto json_schema = sourcemeta::core::parse_json(schema_str);
            std::cerr << "Successfully parsed JSON schema" << std::endl;

            auto walker_obj = sourcemeta::core::schema_official_walker;
            current_custom_resolver = custom_resolver;

            auto resolver_obj = [](std::string_view uri_sv) -> std::optional<sourcemeta::core::JSON> {
                std::string uri(uri_sv);
                std::cerr << "[DEBUG blaze_wrapper] Resolver: Attempting to resolve URI: " << uri << std::endl;

                auto official_result = sourcemeta::core::schema_official_resolver(uri_sv);
                if (official_result.has_value()) {
                    std::cerr << "[DEBUG blaze_wrapper] Resolver: Found in built-in resolver for URI: " << uri << std::endl;
                    return official_result;
                }

                std::cerr << "[DEBUG blaze_wrapper] Resolver: Not found in built-in resolver for URI: " << uri << std::endl;

                if (current_custom_resolver != nullptr) {
                    std::cerr << "[DEBUG blaze_wrapper] Resolver: Attempting custom resolver for URI: " << uri << std::endl;
                    const char* result_c_str = current_custom_resolver(uri.c_str());

                    if (result_c_str != nullptr) {
                        std::string result_str(result_c_str);
                        try {
                            std::cerr << "[DEBUG blaze_wrapper] Resolver: Retrieved custom schema (first 100 chars): " 
                                      << result_str.substr(0, 100) << (result_str.length() > 100 ? "..." : "") << std::endl;
                            auto parsed_json = sourcemeta::core::parse_json(result_str);
                            return parsed_json;
                        } catch (const std::exception& e) {
                            std::cerr << "[ERROR blaze_wrapper] Error parsing JSON from custom resolver: " << e.what() << std::endl;
                        }
                    } else {
                        std::cerr << "[DEBUG blaze_wrapper] Custom resolver returned NULL for URI: " << uri << std::endl;
                    }
                }

                std::cerr << "[DEBUG blaze_wrapper] Resolver: Returning nullopt for URI: " << uri << std::endl;
                return std::nullopt;
            };

            std::cerr << "Creating compiler instance" << std::endl;
            auto compiler = sourcemeta::blaze::default_schema_compiler;

            std::cerr << "[DEBUG blaze_wrapper] Calling sourcemeta::blaze::compile..." << std::endl;
            auto compiled = sourcemeta::blaze::compile(
                json_schema,
                walker_obj,
                resolver_obj,
                compiler,
                sourcemeta::blaze::Mode::FastValidation,
                dialect_opt
            );
            std::cerr << "[DEBUG blaze_wrapper] Compilation successful." << std::endl;

            current_custom_resolver = nullptr;

            auto* template_ptr = new sourcemeta::blaze::Template(compiled);
            std::cerr << "Template object created at: " << (void*)template_ptr << std::endl;
            return reinterpret_cast<int64_t>(template_ptr);
        } catch (const std::exception& internal_e) {
            current_custom_resolver = nullptr;
            std::cerr << "Internal error during compilation: " << internal_e.what() << std::endl;
            throw;
        }
    } catch (const std::exception& e) {
        current_custom_resolver = nullptr;
        std::cerr << "Compilation error: " << e.what() << std::endl;
        return 0;
    } catch (...) {
        current_custom_resolver = nullptr;
        std::cerr << "Unknown error during compilation" << std::endl;
        return 0;
    }
}

BLAZE_EXPORT bool blaze_validate(int64_t schemaHandle, const char* instance) {
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

        if (!schema_template) {
            std::cerr << "Error: Schema template is null" << std::endl;
            return false;
        }

        std::cerr << "Validating instance..." << std::endl;
        return evaluator.validate(*schema_template, json_instance);
    } catch (const std::exception& e) {
        std::cerr << "Validation error: " << e.what() << std::endl;
        return false;
    } catch (...) {
        std::cerr << "Unknown error during validation" << std::endl;
        return false;
    }
}

BLAZE_EXPORT void blaze_free_template(int64_t schemaHandle) {
    if (schemaHandle != 0) {
        auto* template_ptr = reinterpret_cast<sourcemeta::blaze::Template*>(schemaHandle);
        std::cerr << "Freeing Template at: " << (void*)template_ptr << std::endl;
        delete template_ptr;
    }
}

BLAZE_EXPORT void blaze_free_result(const char* result) {
    if (result) {
        delete[] result;
    }
}

BLAZE_EXPORT char* blaze_validate_with_output(int64_t schemaHandle, const char* instance) {
    try {
        if (instance == nullptr) return nullptr;
        if (schemaHandle == 0) return nullptr;

        std::string instance_str(instance);
        auto json_instance = sourcemeta::core::parse_json(instance_str);
        auto* schema_template = reinterpret_cast<sourcemeta::blaze::Template*>(schemaHandle);
        
        sourcemeta::blaze::Evaluator evaluator;
        sourcemeta::blaze::SimpleOutput output{json_instance};
        bool valid = evaluator.validate(*schema_template, json_instance, std::ref(output));
        
        // Build JSON result
        std::ostringstream json_ss;
        json_ss << "{\"valid\":" << (valid ? "true" : "false");
        if (!valid) {
            json_ss << ",\"errors\":[";
            bool first_error = true;
            for (const auto& entry : output) {
                if (!first_error) {
                    json_ss << ",";
                }
                first_error = false;
                std::string instance_loc_str = sourcemeta::core::to_string(entry.instance_location);
                std::string eval_path_str = sourcemeta::core::to_string(entry.evaluate_path);
                // Basic escaping for quotes and backslashes
                auto escape = [](const std::string& s) {
                    std::string out;
                    for (char c : s) {
                        if (c == '"' || c == '\\') out.push_back('\\');
                        out.push_back(c);
                    }
                    return out;
                };
                json_ss << "{\"message\":\"" << escape(entry.message)
                        << "\",\"instance_location\":\"" << escape(instance_loc_str)
                        << "\",\"evaluate_path\":\"" << escape(eval_path_str) << "\"}";
            }
            json_ss << "]";
        }
        json_ss << "}";
        std::string result_str = json_ss.str();
        char* buffer = new char[result_str.size() + 1];
        std::strcpy(buffer, result_str.c_str());
        return buffer;
    } catch (const std::exception& e) {
        std::cerr << "Detailed validation error: " << e.what() << std::endl;
        return nullptr;
    } catch (...) {
        std::cerr << "Unknown error during detailed validation" << std::endl;
        return nullptr;
    }
}

BLAZE_EXPORT void blaze_free_json(char* json) {
    if (json) delete[] json;
}

} 
