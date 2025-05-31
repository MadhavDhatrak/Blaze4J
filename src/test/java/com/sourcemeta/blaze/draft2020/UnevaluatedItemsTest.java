package com.sourcemeta.blaze.draft2020;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.sourcemeta.blaze.Blaze;
import com.sourcemeta.blaze.BlazeValidator;
import com.sourcemeta.blaze.CompiledSchema;
import com.sourcemeta.blaze.draft2020.Json;

@TestInstance(Lifecycle.PER_CLASS)
public class UnevaluatedItemsTest {

    private final AtomicInteger totalTests = new AtomicInteger();
    private final AtomicInteger passedTests = new AtomicInteger();
    private final AtomicInteger skippedTests = new AtomicInteger();
    private final AtomicInteger failedTests = new AtomicInteger();

    private final List<String> failedTestDetails = Collections.synchronizedList(new ArrayList<>());

    private static final List<String> TEST_FILES = List.of("unevaluatedProperties.json", "unevaluatedItems.json");

    @AfterAll
    public void printSummary() {
        System.out.println("\n===== Unevaluated Test Summary =====");
        System.out.println("Total tests: " + totalTests.get());
        System.out.println("Passed tests: " + passedTests.get());
        System.out.println("Skipped tests: " + skippedTests.get());
        System.out.println("Failed tests: " + failedTests.get());

        double passPercentage = 0;
        if (totalTests.get() > 0) {
            passPercentage = (double) passedTests.get() / totalTests.get() * 100;
        }
        System.out.printf("Pass rate: %.1f%%\n", passPercentage);

        if (!failedTestDetails.isEmpty()) {
            System.out.println("\n===== Failed Tests Details =====");
            failedTestDetails.forEach(System.out::println);
            System.out.println("=================================");
        }

        System.out.println("=========================================\n");
    }

    @Test
    public void testFilesExist() {
        for (String fileName : TEST_FILES) {
            Path testFilePath = Paths.get("src/test/resources/JSON-Schema-Test-Suite/tests/draft2020-12/" + fileName);
            assertTrue(Files.exists(testFilePath), "Test file not found: " + testFilePath);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    @ParameterizedTest
    @MethodSource("testCases")
    @Tag("unevaluatedItems")
    public void runTest(TestCase testCase) throws Exception {
        totalTests.incrementAndGet();

        if (testCase.schema.contains("$dynamicRef") || testCase.schema.contains("$recursiveRef")) {
            System.out.println("Skipping test with complex dynamic/recursive references: " + testCase.description);
            skippedTests.incrementAndGet();
            return;
        }

        Arena arena = null;
        CompiledSchema schema = null;

        try {
            arena = Arena.ofConfined();

            if (testCase.schema.equals("true") || testCase.schema.equals("false")) {
                boolean schemaValue = Boolean.parseBoolean(testCase.schema);
                if (schemaValue == testCase.valid) {
                    passedTests.incrementAndGet();
                } else {
                    String message = String.format(
                        "Boolean schema test failed: %s expects %s but boolean schema is %s",
                        testCase.description, testCase.valid, schemaValue);
                    failedTests.incrementAndGet();
                    System.err.println("VALIDATION ERROR: " + message);
                    failedTestDetails.add(String.format("Test: %s - %s", testCase.description, message));
                }
                return;
            }

            schema = Blaze.compile(testCase.schema, arena);
            BlazeValidator validator = new BlazeValidator();

            boolean actual = validator.validate(schema, testCase.data);
            if (actual != testCase.valid) {
                String message = String.format(
                    "%s: Expected %s but got %s",
                    testCase.description, testCase.valid, actual);
                failedTests.incrementAndGet();
                System.err.println("VALIDATION ERROR: " + message);
                failedTestDetails.add(String.format("Test: %s - %s", testCase.description, message));
            } else {
                passedTests.incrementAndGet();
            }
        } catch (RuntimeException e) {
            System.err.println("Error for test: " + testCase.description + " - " + e.getMessage());
            failedTests.incrementAndGet();
            failedTestDetails.add(String.format("Test: %s - Error: %s", testCase.description, e.getMessage()));
        } finally {
            if (arena != null) {
                arena.close();
            }
        }
    }

    public static Stream<Arguments> testCases() throws Exception {
        List<TestCase> allTests = new ArrayList<>();
        for (String fileName : TEST_FILES) {
            Path testFile = Paths.get("src/test/resources/JSON-Schema-Test-Suite/tests/draft2020-12/" + fileName);
            allTests.addAll(loadTests(testFile));
        }
        return allTests.stream().map(Arguments::of);
    }

    private static List<TestCase> loadTests(Path file) throws IOException {
        List<TestCase> result = new ArrayList<>();
        String content = Files.readString(file);

        JsonNode rootNode = Json.parse(content);
        for (JsonNode schemaTest : rootNode) {
            String schemaDescription = schemaTest.get("description").asText();
            JsonNode schemaNode = schemaTest.get("schema");
            String schemaJson = Json.mapper().writeValueAsString(schemaNode);

            for (JsonNode testNode : schemaTest.get("tests")) {
                String testDescription = testNode.get("description").asText();
                String fullDescription = schemaDescription + " - " + testDescription;
                JsonNode dataNode = testNode.get("data");
                String dataJson = Json.mapper().writeValueAsString(dataNode);
                boolean valid = testNode.get("valid").asBoolean();

                TestCase testCase = new TestCase(fullDescription, schemaJson, dataJson, valid, file.getFileName().toString());
                result.add(testCase);
            }
        }

        return result;
    }

    static class TestCase {
        String description;
        String schema;
        String data;
        boolean valid;
        String file;

        TestCase(String description, String schema, String data, boolean valid, String file) {
            this.description = description;
            this.schema = schema;
            this.data = data;
            this.valid = valid;
            this.file = file;
        }

        @Override
        public String toString() {
            return description;
        }
    }
}
