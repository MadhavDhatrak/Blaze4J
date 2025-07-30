# Blaze4J

**Blaze4J** is a high-performance Java wrapper for the Blaze JSON Schema validator, providing seamless integration between Java applications and the native C++ Blaze validator. By leveraging Java's Foreign Function & Memory API (FFM), this library offers excellent performance while maintaining memory safety.

Blaze4J enables Java developers to validate JSON documents against JSON Schema specifications (supporting multiple draft versions) with minimal overhead and maximum efficiency.

---
<p align="center">
  <img src="https://github.com/user-attachments/assets/5b1f7e38-4614-446e-985c-6ad2cc135ff3" width="516" height="346" alt="Image 2" />
</p>


## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Installation](./installation.md) 
- [Usage](#usage) 
- [Supported Drafts](#supported-drafts) 
- [Supported Resolvers](#supported-resolvers) 
- [API Documentation](#api-documentation)
  - [Compiler API](./docs/compiler.md)
  - [Validator API](./docs/validator.md)
- [Best Practices](#best-practices)
- [Contributing](#contributing)
- [Credits](#credits)

---

## Overview

Blaze4J bridges Java and native C++ for **fast, standards-compliant JSON Schema validation**. It supports multiple schema drafts, efficient memory management via the FFM API, and robust error reporting.

---

## Features

- **High-performance**: Native C++ validation with minimal Java overhead.
- **Multi-draft support**: Compatible with JSON Schema Drafts 4, 6, 7, 2019-09, and 2020-12.
- **Memory safety**: Uses Java's Foreign Function & Memory API (FFM) for safe, efficient resource management.
- **Detailed error reporting**: Get precise validation errors and paths.
- **Easy integration**: Simple API for compiling schemas and validating instances.
- **Cross-platform**: Works on Windows, Linux, and macOS.


---

## Installation

**Maven**
- Add the following dependency to your `pom.xml`:
```java
<dependency>
    <groupId>io.github.madhavdhatrak</groupId>
    <artifactId>blaze4j</artifactId>
    <version>0.0.1</version>
</dependency>

```

**Gradle**
- Add this to your `build.gradle`:
```java
implementation group: 'io.github.madhavdhatrak', name: 'blaze4j', version: '0.0.1'
```

 **Make Sure to have pom.xml Like this**
```java
<properties>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>

<dependencies>
    <dependency>
        <groupId>io.github.madhavdhatrak</groupId>
        <artifactId>blaze4j</artifactId>
        <version>0.0.1</version>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.11.0</version>
            <configuration>
                <compilerArgs>
                    <arg>--enable-preview</arg>
                </compilerArgs>
            </configuration>
        </plugin>
        <plugin>
            <groupId>org.codehaus.mojo</groupId>
            <artifactId>exec-maven-plugin</artifactId>
            <version>3.1.0</version>
            <configuration>
                <executable>java</executable>
                <arguments>
                    <argument>--enable-preview</argument>
                    <argument>--enable-native-access=ALL-UNNAMED</argument>
                    <argument>-classpath</argument>
                    <classpath/>
                    <argument>${exec.mainClass}</argument>
                </arguments>
            </configuration>
        </plugin>
    </plugins>
</build>
```

**Testing**
- **Run your test class using**:
```java
mvn clean compile exec:exec -Dexec.mainClass=com.example.Test
```
_**Replace com.example.Test with the path to your test class**_.


## Usage 
- **Simple Boolean Result**

```java
package com.example;

import com.github.madhavdhatrak.blaze4j.Blaze;
import com.github.madhavdhatrak.blaze4j.CompiledSchema;
import com.github.madhavdhatrak.blaze4j.BlazeValidator;

public class Test {
    public static void main(String[] args) {
        String schemaJson = """
            {
                "$schema": "https://json-schema.org/draft/2020-12/schema",
                "type": "string"
            }
            """;

        try (CompiledSchema schema = Blaze.compile(schemaJson)) {
            final BlazeValidator validator = new BlazeValidator();
            
            // Test valid string input
            boolean validStringResult = validator.validate(schema, "\"hello\"");
            System.out.println("Validation result for \"hello\": " + validStringResult);
            
            // Test invalid number input
            boolean invalidNumberResult = validator.validate(schema, "42");
            System.out.println("Validation result for 42: " + invalidNumberResult);
        }
    }
}
```
- **Detailed Error Reporting**
```java
package com.example;

import com.github.madhavdhatrak.blaze4j.Blaze;
import com.github.madhavdhatrak.blaze4j.CompiledSchema;
import com.github.madhavdhatrak.blaze4j.ValidationResult;
import com.github.madhavdhatrak.blaze4j.ValidationError;

public class TestBlaze4j {
    public static void main(String[] args) {
        String enumSchemaJson = """
            {
                "$schema": "https://json-schema.org/draft/2020-12/schema",
                "enum": ["red", "green", "blue"]
            }
            """;

        String invalidEnumInstance = "\"yellow\"";

        try (CompiledSchema compiledSchema = Blaze.compile(enumSchemaJson)) {
            ValidationResult result = Blaze.validateWithDetails(compiledSchema, invalidEnumInstance);

            System.out.println("Is valid: " + result.isValid());

            if (!result.isValid()) {
                System.out.println("Validation errors:");
                result.getErrors().forEach(System.out::println);
            }
        }
    }
}
```

---
## Supported Drafts 

| Draft Version | Status        | Specification                                                                 |
|---------------|--------------|-------------------------------------------------------------------------------|
| Draft 2020-12 | ✅ Supported | [Spec](https://json-schema.org/draft/2020-12/release-notes.html)              |
| Draft 2019-09 | ✅ Supported | [Spec](https://json-schema.org/draft/2019-09/release-notes.html)              |
| Draft 7       | ✅ Supported | [Spec](https://json-schema.org/draft-07/release-notes.html)                   |
| Draft 6       | ✅ Supported | [Spec](https://json-schema.org/draft-06/release-notes.html)                   |
| Draft 4       | ✅ Supported | [Spec](https://json-schema.org/draft-04/release-notes.html)                   |

---
<a name="supported-resolvers"></a>
## Supported `$ref` Resolvers:

| Resolver Type | Status        | Description                                   |
|---------------|---------------|-----------------------------------------------|
| `http`        | ✅ Supported  | Resolves external schemas via HTTP(S) URLs    |
| `classpath`   | ✅ Supported  | Resolves schemas from the Java classpath      |

---

## API Documentation

- [Compiler API](./docs/Compiler.md)- How to compile schemas, manage memory, and use different dialects.
- [Validator API](./docs/Validator.md)-How to validate instances, get boolean or detailed results, and interpret errors.

---

## Best Practices

- Use try-with-resources for all `CompiledSchema` and `Arena` objects to ensure proper cleanup.
- Prefer detailed validation (`validateWithDetails`) for debugging and error reporting.
- Always specify a `$schema` or default dialect for maximum compatibility.

---

## Contributing

Feel free to open an issue for bugs or feature requests, or submit a pull request to improve the project. Whether it's fixing a typo, improving the docs, or adding new features—every contribution counts!

For local installation and development, see [CONTRIBUTING.md](./docs/CONTRIBUTING.md). The project uses Git submodules, so when cloning the repository, use:


## Credits

- [Blaze](https://github.com/sourcemeta/blaze) — Native JSON Schema validator
- [JSON Schema Test Suite](https://github.com/json-schema-org/JSON-Schema-Test-Suite)
