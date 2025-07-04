# BlazeWrapper Compiler

The BlazeWrapper Compiler provides an API for compiling JSON Schemas into a low-level instruction language for efficient validation. This document covers the usage of the compiler, its API overloads, key terms, and practical examples.

## Table of Contents

- [Overview](#overview)
- [Key Terms](#key-terms)
- [API Reference](#api-reference)
  - [compile(String schema)](#compilestring-schema)
  - [compile(String schema, String defaultDialect)](#compilestring-schema-string-defaultdialect)
  - [compile(String schema, Arena arena)](#compilestring-schema-arena) *(optional)*
  - [compile(String schema, Arena arena, String defaultDialect)](#compilestring-schema-arena-string-defaultdialect) *(optional)*
- [Usage Examples](#usage-examples)
- [Best Practices](#best-practices) 
- [See Also](#see-also)

## Overview

The BlazeWrapper Schema Compiler allows you to compile JSON Schema documents into ```CompiledSchema``` objects. These objects enable fast and repeated validation of JSON data. The compiler internally manages resources using an ```Arena``` to ensure efficient memory usage and lifecycle management.

## Key Terms

- **JSON Schema**: A declarative language for validating the structure of JSON data.
- **CompiledSchema**: An optimized, pre-processed version of your JSON Schema, ready for validation.
- **Arena**: A memory/resource management context used internally to manage the lifecycle of compiled schemas.
- **Dialect**: The specific version or variant of the JSON Schema specification (e.g., Draft 2020-12).

## API Reference

### `compile(String schema)`

**Compiles a JSON schema string using the default dialect specified within the schema (if present)**.
```java
public static CompiledSchema compile(String schema)

```
- **Parameters:**
  - `schema`: The JSON schema as a string.
- **Returns:** `CompiledSchema` — the compiled schema instance.
- **Behavior:** Internally creates and manages an `Arena` for resource management. Uses the `$schema` property in the schema to determine the dialect.

---

### `compile(String schema, String defaultDialect)`

**Compiles a JSON schema string, allowing you to specify a default dialect if the schema does not declare one**
```java
public static CompiledSchema compile(String schema, String defaultDialect)
```
- **Parameters:**
  - `schema`: The JSON schema as a string.
  - `defaultDialect`: The default dialect URI to use if the schema does not specify one.
- **Returns:** `CompiledSchema` — the compiled schema instance.
- **Behavior:** Internally creates and manages an `Arena`. If the schema does not specify a `$schema` property, `defaultDialect` is used.

---
<a name="compilestring-schema-arena"></a>
### `compile(String schema, Arena arena)`

**Compiles a JSON schema string using the provided Arena for resource management.**
```java
public static CompiledSchema compile(String schema, Arena arena)
```
- **Parameters:**
  - `schema`: The JSON schema as a string.
  - `arena`: The memory arena for resource management.
- **Returns:** `CompiledSchema` — the compiled schema instance.
- **Behavior:** Uses the provided `Arena` to manage the lifecycle of the compiled schema, allowing for explicit control over memory/resource management.

---
<a name="compilestring-schema-arena-string-defaultdialect"></a>
### `compile(String schema, Arena arena, String defaultDialect)`

**Compiles a JSON schema string with an explicit default dialect, using the provided Arena for resource management.**
```java
public static CompiledSchema compile(String schema, Arena arena, String defaultDialect)
```
- **Parameters:**
  - `schema`: The JSON schema as a string.
  - `arena`: The memory arena for resource management.
  - `defaultDialect`: The default dialect URI to use if the schema does not specify one.
- **Returns:** `CompiledSchema` — the compiled schema instance.
- **Behavior:** Uses the provided `Arena` for resource management. If the schema does not specify a `$schema` property, `defaultDialect` is used.

---
## Usage Examples

### Example 1: Compiling with Schema-Declared Dialect
```java
String schemaJson = "{"
+ ""$schema": "https://json-schema.org/draft/2020-12/schema\","
+ ""type": "string""
+ "}";

try (CompiledSchema schema = Blaze.compile(schemaJson)) {
BlazeValidator validator = new BlazeValidator();

boolean validStringResult = validator.validate(schema, "\"hello\"");
System.out.println("Validation result for \"hello\": " + validStringResult);
assertTrue(validStringResult);
}

```
### Example 2: Compiling with Explicit Default Dialect
```java
String schemaJson = "{"
+ ""type": "string""
+ "}";

String defaultDialect = "https://json-schema.org/draft/2020-12/schema";

try (CompiledSchema schema = Blaze.compile(schemaJson, defaultDialect)) {
BlazeValidator validator = new BlazeValidator();

boolean validStringResult = validator.validate(schema, "\"world\"");
System.out.println("Validation result for \"world\": " + validStringResult);
assertTrue(validStringResult);
}

```

### Example 3: Compiling with Arena (Resource Management Control)
```java
import java.lang.foreign.Arena;

String schemaJson = "{"
+ ""$schema": "https://json-schema.org/draft/2020-12/schema\","
+ ""type": "string""
+ "}";

try (Arena arena = Arena.ofConfined();
CompiledSchema compiledSchema = Blaze.compile(schemaJson, arena)) {
BlazeValidator validator = new BlazeValidator();

boolean validStringResult = validator.validate(compiledSchema, "\"hello\"");
System.out.println("Validation result for \"hello\": " + validStringResult);
assertTrue(validStringResult);
}

```


### Example 4: Compiling with Arena and Explicit Default Dialect
```java
import java.lang.foreign.Arena;

String schemaJson = "{"
+ ""type": "string""
+ "}";

String defaultDialect = "https://json-schema.org/draft/2020-12/schema";

try (Arena arena = Arena.ofConfined();
CompiledSchema compiledSchema = Blaze.compile(schemaJson, arena, defaultDialect)) {
BlazeValidator validator = new BlazeValidator();

boolean validStringResult = validator.validate(compiledSchema, "\"world\"");
System.out.println("Validation result for \"world\": " + validStringResult);
assertTrue(validStringResult);
}
```

## Best Practices

- **Always close `CompiledSchema`**: Use try-with-resources to ensure proper cleanup.
- **Specify dialect when in doubt**: If your schema does not declare a `$schema` property, always provide a `defaultDialect` to avoid ambiguity.
- **Validate input types**: Ensure the JSON data you validate matches the schema's expectations.
- **If your schema uses `$ref` always use the API —** [`compile(String schema, String defaultDialect)`](#compilestring-schema-string-defaultdialect).  
  The BlazeWrapper Compiler supports two custom `$ref` resolvers: **`http`** and **`classpath`**.  
  Even if your schema includes a `$schema` declaration, it is **highly recommended** to use this API to ensure proper resolution and avoid runtime errors.

## See Also

- [Evaluator Documentation](./Validator.md)
- [JSON Schema Specification](https://json-schema.org/specification.html)

---

