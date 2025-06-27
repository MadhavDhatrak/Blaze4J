package com.sourcemeta.blaze;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

/**
 * Represents the detailed result of a JSON schema validation.
 */
public class ValidationResult {
    private final boolean valid;
    private final List<ValidationError> errors;

    /**
     * Creates a new ValidationResult instance.
     * 
     * @param valid Whether the instance is valid against the schema
     * @param errors List of validation errors, if any
     */
    public ValidationResult(boolean valid, List<ValidationError> errors) {
        this.valid = valid;
        this.errors = errors != null ? Collections.unmodifiableList(new ArrayList<>(errors)) : Collections.emptyList();
    }

    /**
     * Creates a ValidationResult from a JSON string.
     * 
     * @param jsonString JSON string containing validation results
     * @return A ValidationResult instance
     */
    public static ValidationResult fromJson(String jsonString) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonString);
            boolean valid = root.path("valid").asBoolean();
            List<ValidationError> errors = new ArrayList<>();

            if (!valid && root.has("errors")) {
                for (JsonNode errNode : root.get("errors")) {
                    String message = errNode.path("message").asText();
                    String instanceLocation = errNode.path("instance_location").asText();
                    String evaluatePath = errNode.path("evaluate_path").asText();
                    errors.add(new ValidationError(message, instanceLocation, evaluatePath));
                }
            }

            return new ValidationResult(valid, errors);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse validation JSON", e);
        }
    }

    /**
     * Checks if the instance is valid against the schema.
     * 
     * @return true if the instance is valid, false otherwise
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Gets the list of validation errors.
     * 
     * @return An unmodifiable list of validation errors
     */
    public List<ValidationError> getErrors() {
        return errors;
    }
} 