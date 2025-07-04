# BlazeWrapper Validator

The BlazeWrapper Validator provides an API for validating JSON instances against compiled JSON Schemas. This document covers the usage of the validator, its API overloads, key terms, and practical examples.

## Table of Contents

- [Overview](#overview)
- [Key Terms](#key-terms)
- [API Reference](#api-reference)
  - [validate(CompiledSchema schema, String instance)](#validatecompiledschema-schema-string-instance)
  - [validateWithDetails(CompiledSchema schema, String instance)](#validatewithdetailscompiledschema-schema-string-instance)
- [Usage Examples](#usage-examples)
  - [Basic Boolean Validation](#basic-boolean-validation)
  - [Detailed Validation with Error Reporting](#detailed-validation-with-error-reporting)
- [ValidationResult Error Structure](#validationresult-error-structure)
- [Best Practices](#best-practices)
- [See Also](#see-also)

## Overview

The BlazeWrapper Validator enables fast and robust validation of JSON data against compiled schemas. It offers two main validation APIs: a simple boolean check and a detailed result for error reporting and debugging. The validator is designed for high performance and integrates seamlessly with the BlazeWrapper Compiler.

## Key Terms

- **CompiledSchema**: An optimized, pre-processed version of your JSON Schema, ready for validation.
- **Instance**: The JSON data (as a string) to be validated against the schema.
- **ValidationResult**: An object representing the outcome of a validation, including validity status and detailed errors (if any).

## API Reference

### `validate(CompiledSchema schema, String instance)`

**Validates a JSON instance against a schema. Returns a simple boolean result.**
```java
public static boolean validate(CompiledSchema schema, String instance)
```
- **Parameters:**
  - `schema`: The compiled schema.
  - `instance`: The JSON instance as a string.
- **Returns:** `true` if the instance is valid, `false` otherwise.
- **Behavior:** Performs a fast validation and returns only the validity status.

---

### `validateWithDetails(CompiledSchema schema, String instance`

**Validates a JSON instance against a schema and returns detailed validation results.**
```java
public static ValidationResult validateWithDetails(CompiledSchema schema, String instance)
```

- **Parameters:**
  - `schema`: The compiled schema.
  - `instance`: The JSON instance as a string.
- **Returns:** `ValidationResult` containing validity and error details.
- **Behavior:** Performs validation and, on failure, provides a list of error objects with messages and paths.

---

## Usage Examples

### Basic Boolean Validation
```java
String schemaJson = "{"
+ ""$schema": "https://json-schema.org/draft/2020-12/schema\","
+ ""type": "string""
+ "}";

try (CompiledSchema schema = Blaze.compile(schemaJson)) {

boolean validStringResult = Blaze.validate(schema, ""hello"");

System.out.println("Validation result for "hello": " + validStringResult);
assertTrue(validStringResult);
}

```

---

### Detailed Validation with Error Reporting
```java
String schemaJson = "{"
+ "\"$schema\": \"https://json-schema.org/draft/2020-12/schema\","
+ "\"enum\": [\"red\", \"green\", \"blue\"]"
+ "}";

String instance = "\"yellow\"";

try (CompiledSchema compiledSchema = Blaze.compile(schemaJson)) {
    ValidationResult result = Blaze.validateWithDetails(compiledSchema, instance);

    System.out.println(result.isValid());

    if (!result.isValid()) {
        result.getErrors().forEach(System.out::println);
    }

    assertFalse(result.isValid());
}


```

**Sample error output:**
- message : The string value "yellow" was expected to equal one of the given declared values
- instancePath : root
- schemaPath : /enum

---

## ValidationResult Error Structure

When validation fails, the `ValidationResult` contains a list of errors. Each error includes:
- **message**: A human-readable description of the validation failure.
- **instancePath**: JSON Pointer path to the location in the instance where the error occurred.
- **schemaPath**: JSON Pointer path to the schema keyword or location that caused the error.

---

## Best Practices

- **Use try-with-resources**: Always close `CompiledSchema` to ensure proper resource management.
- **Use detailed validation for debugging**: Prefer `validateWithDetails` in development or when you need to report specific validation errors.
- **Validate input types**: Ensure the JSON instance matches the expected schema type for accurate results.

## See Also

- [BlazeWrapper Compiler Documentation](./Compiler.md)
- [JSON Schema Specification](https://json-schema.org/specification.html)

---
