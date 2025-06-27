package com.sourcemeta.blaze;

/**
 * Represents a single validation error from JSON schema validation.
 */
public class ValidationError {
    private final String message;
    private final String instanceLocation;
    private final String evaluatePath;

    /**
     * Creates a new ValidationError instance.
     * 
     * @param message The error message
     * @param instanceLocation The JSON pointer to the location in the instance where the error occurred
     * @param evaluatePath The evaluation path in the schema
     */
    public ValidationError(String message, String instanceLocation, String evaluatePath) {
        this.message = message;
        this.instanceLocation = instanceLocation;
        this.evaluatePath = evaluatePath;
    }

    /**
     * Gets the error message.
     * 
     * @return The error message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Gets the instance location where the error occurred.
     * 
     * @return The JSON pointer to the location in the instance
     */
    public String getInstanceLocation() {
        return instanceLocation;
    }

    /**
     * Gets the evaluation path in the schema.
     * 
     * @return The evaluation path
     */
    public String getEvaluatePath() {
        return evaluatePath;
    }

    @Override
    public String toString() {
        return "- message        : " + message + System.lineSeparator() +
               "  instance path : " + (instanceLocation.isEmpty() ? "<root>" : instanceLocation) + System.lineSeparator() +
               "  schema path   : " + evaluatePath;
    }
} 