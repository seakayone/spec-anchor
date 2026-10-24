package org.kleinb.openspec.gradle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.kleinb.openspec.annotation.OpenSpec;

class SpecAnchorPluginTest {

    @TempDir
    Path dir;

    static final String ANNOTATIONS_JAR = System.getProperty("specAnchor.annotationsJar").replace('\\', '/');

    static final String VALID = "#### Scenario: Valid credentials <a id=\"valid-credentials\"></a>\n";

    void write(String path, String content) throws IOException {
        Path file = dir.resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    /** A Java project applying the plugin, with [specAnchor] as the body of its {@code specAnchor { }} block. */
    void javaProject(String specAnchor) throws IOException {
        write("settings.gradle.kts", "rootProject.name = \"sample\"\n");
        write("build.gradle.kts", """
            plugins {
                java
                id("org.kleinb.openspec")
            }
            dependencies {
                testImplementation(files("%s"))
            }
            // the sample test classes are annotated stubs, not runnable tests
            tasks.test { failOnNoDiscoveredTests = false }
            specAnchor {
            %s
            }
            """.formatted(ANNOTATIONS_JAR, specAnchor));
    }

    void test(String className, String annotations) throws IOException {
        write("src/test/java/sample/" + className + ".java", """
            package sample;
            import org.kleinb.openspec.annotation.OpenSpec;
            class %s {
                %s
                void test() { }
            }
            """.formatted(className, annotations));
    }

    GradleRunner runner(String... args) {
        List<String> arguments = new java.util.ArrayList<>(List.of(args));
        arguments.add("--configuration-cache");
        arguments.add("--stacktrace");
        return GradleRunner.create().withProjectDir(dir.toFile()).withPluginClasspath().withArguments(arguments);
    }

    String report() throws IOException {
        return Files.readString(dir.resolve("build/reports/spec-anchor/findings.txt"));
    }

    @Test
    @OpenSpec(capability = "build-spec-check", scenario = "runs-with-check")
    void runsWithCheck() throws IOException {
        javaProject("");
        write("openspec/specs/user-auth/spec.md", VALID);
        test("LoginTest", "@OpenSpec(capability = \"user-auth\", scenario = \"valid-credentials\")");

        BuildResult result = runner("check").build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":compileTestJava").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, result.task(":specAnchorCheck").getOutcome());
        assertTrue(result.getOutput().contains("spec-anchor: no findings"), result.getOutput());
    }

    @Test
    @OpenSpec(capability = "build-spec-check", scenario = "reruns-on-spec-change")
    void rerunsOnSpecChange() throws IOException {
        javaProject("");
        write("openspec/specs/user-auth/spec.md", VALID);

        assertEquals(TaskOutcome.SUCCESS, runner("specAnchorCheck").build().task(":specAnchorCheck").getOutcome());
        assertEquals(TaskOutcome.UP_TO_DATE, runner("specAnchorCheck").build().task(":specAnchorCheck").getOutcome());

        write("openspec/specs/user-auth/spec.md", VALID + "#### Scenario: Wrong password <a id=\"wrong-password\"></a>\n");
        BuildResult result = runner("specAnchorCheck").build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":specAnchorCheck").getOutcome());
        assertTrue(result.getOutput().contains("no test linked to scenario 'wrong-password'"), result.getOutput());
    }

    @Test
    @OpenSpec(capability = "build-spec-check", scenario = "custom-openspec-dir")
    void customOpenspecDir() throws IOException {
        javaProject("openspecDir = file(\"docs/openspec\")");
        write("docs/openspec/specs/user-auth/spec.md", VALID);
        write("openspec/specs/ignored/spec.md", "#### Scenario: Ignored <a id=\"ignored\"></a>\n");

        String output = runner("specAnchorCheck").build().getOutput();

        assertTrue(output.contains("untested-scenario docs/openspec/specs/user-auth/spec.md:1"), output);
        assertFalse(output.contains("ignored"), output);
    }

    @Test
    @OpenSpec(capability = "build-spec-check", scenario = "ignores-archived-specs")
    void ignoresArchivedSpecs() throws IOException {
        javaProject("");
        write("openspec/changes/archive/2026-01-01-add-login/specs/user-auth/spec.md", VALID);
        test("LoginTest", "@OpenSpec(capability = \"user-auth\", scenario = \"valid-credentials\")");

        String output = runner("specAnchorCheck").build().getOutput();

        assertTrue(output.contains("unknown-reference sample.LoginTest#test: unknown capability 'user-auth'"), output);
        assertFalse(output.contains("untested-scenario"), output);
    }

    @Test
    @OpenSpec(capability = "build-spec-check", scenario = "prints-and-writes-findings")
    void printsAndWritesFindings() throws IOException {
        javaProject("");
        write("openspec/specs/user-auth/spec.md", VALID + "#### Scenario: No slug\n");

        String output = runner("specAnchorCheck").build().getOutput();

        List<String> expected = List.of(
            "untested-scenario openspec/specs/user-auth/spec.md:1: no test linked to scenario 'valid-credentials'",
            "missing-slug openspec/specs/user-auth/spec.md:2: scenario 'No slug' has no slug anchor",
            "spec-anchor: 1 untested-scenario, 1 missing-slug");
        expected.forEach(line -> assertTrue(output.contains(line), output));
        assertEquals(expected, report().lines().toList());
    }

    @Test
    @OpenSpec(capability = "build-spec-check", scenario = "reports-no-findings")
    void reportsNoFindings() throws IOException {
        javaProject("");
        write("openspec/specs/user-auth/spec.md", VALID);
        test("LoginTest", "@OpenSpec(capability = \"user-auth\", scenario = \"valid-credentials\")");

        BuildResult result = runner("specAnchorCheck").build();

        assertTrue(result.getOutput().contains("spec-anchor: no findings"), result.getOutput());
        assertEquals("spec-anchor: no findings\n", report());
    }

    @Test
    @OpenSpec(capability = "build-spec-check", scenario = "reports-only-by-default")
    void reportsOnlyByDefault() throws IOException {
        javaProject("");
        write("openspec/specs/user-auth/spec.md", VALID);

        BuildResult result = runner("specAnchorCheck").build();

        assertTrue(result.getOutput().contains("spec-anchor: 1 untested-scenario"), result.getOutput());
    }

    @Test
    @OpenSpec(capability = "build-spec-check", scenario = "fails-on-enabled-kind")
    void failsOnEnabledKind() throws IOException {
        javaProject("failOnUntestedScenario = true");
        write("openspec/specs/user-auth/spec.md", VALID + "#### Scenario: Wrong password <a id=\"wrong-password\"></a>\n#### Scenario: No slug\n");

        BuildResult result = runner("specAnchorCheck").buildAndFail();

        assertEquals(TaskOutcome.FAILED, result.task(":specAnchorCheck").getOutcome());
        assertTrue(result.getOutput().contains("missing-slug openspec/specs/user-auth/spec.md:3"), result.getOutput());
        assertTrue(result.getOutput().contains("spec-anchor check failed: 2 untested-scenario ("), result.getOutput());
    }

    @Test
    @OpenSpec(capability = "build-spec-check", scenario = "reads-kotlin-tests")
    @OpenSpec(capability = "build-spec-check", scenario = "reads-repeated-annotations")
    void readsKotlinTests() throws IOException {
        write("settings.gradle.kts", """
            pluginManagement { repositories { gradlePluginPortal() } }
            rootProject.name = "sample"
            """);
        write("build.gradle.kts", """
            plugins {
                kotlin("jvm") version "%s"
                id("org.kleinb.openspec")
            }
            repositories { mavenCentral() }
            dependencies {
                testImplementation(files("%s"))
            }
            tasks.test { failOnNoDiscoveredTests = false }
            """.formatted(System.getProperty("specAnchor.kotlinVersion"), ANNOTATIONS_JAR));
        write("openspec/specs/user-auth/spec.md", VALID + "#### Scenario: Wrong password <a id=\"wrong-password\"></a>\n");
        write("src/test/kotlin/sample/LoginTest.kt", """
            package sample
            import org.kleinb.openspec.annotation.OpenSpec
            class LoginTest {
                @OpenSpec(capability = "user-auth", scenario = "valid-credentials")
                @OpenSpec(capability = "user-auth", scenario = "wrong-password")
                fun login() { }
            }
            """);

        BuildResult result = runner("specAnchorCheck").build();

        assertTrue(result.getOutput().contains("spec-anchor: no findings"), result.getOutput());
    }

    @Test
    @OpenSpec(capability = "build-spec-check", scenario = "ignores-disabled-kinds")
    void ignoresDisabledKinds() throws IOException {
        javaProject("failOnUntestedScenario = true");
        write("openspec/specs/user-auth/spec.md", "#### Scenario: No slug\n");

        BuildResult result = runner("specAnchorCheck").build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":specAnchorCheck").getOutcome());
        assertTrue(result.getOutput().contains("spec-anchor: 1 missing-slug"), result.getOutput());
    }
}
