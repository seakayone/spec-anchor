package org.kleinb.openspec.gradle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.kleinb.openspec.annotation.OpenSpec;
import org.kleinb.openspec.gradle.check.Finding.Kind;

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
        List<String> arguments = new ArrayList<>(List.of(args));
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
    @OpenSpec(capability = "build-spec-check", scenario = "missing-openspec-dir")
    void missingOpenspecDir() throws IOException {
        javaProject("openspecDir = file(\"no-such-dir\")");
        test("LoginTest", "@OpenSpec(capability = \"user-auth\")");

        BuildResult result = runner("specAnchorCheck").build();

        assertTrue(result.getOutput().contains("unknown-reference sample.LoginTest#test: unknown capability 'user-auth'"), result.getOutput());
        assertTrue(result.getOutput().contains("spec-anchor: 1 unknown-reference\n"), result.getOutput());
    }

    // --- fail toggles -------------------------------------------------------------------------------------------

    /** The extension property for [kind], e.g. {@code failOnUntestedScenario} for {@code untested-scenario}. */
    static String toggle(Kind kind) {
        return "failOn" + Arrays.stream(kind.id.split("-")).map(w -> Character.toUpperCase(w.charAt(0)) + w.substring(1)).collect(Collectors.joining());
    }

    /** How many findings of each kind the fixture of {@link #project} produces. */
    static long count(Kind kind) {
        return kind == Kind.DUPLICATE_SLUG ? 2 : 1;
    }

    static String describe(Set<Kind> kinds) {
        return kinds.stream().sorted().map(kind -> count(kind) + " " + kind.id).collect(Collectors.joining(", "));
    }

    /** A project whose findings are exactly [found] (see {@link #count}), with the toggles of [failOn] set to true. */
    void project(Set<Kind> found, Set<Kind> failOn) throws IOException {
        javaProject(failOn.stream().map(kind -> toggle(kind) + " = true").collect(Collectors.joining("\n")));
        StringBuilder spec = new StringBuilder("# user-auth\n\n#### Scenario: Covered <a id=\"covered\"></a>\n");
        List<String> links = new ArrayList<>(List.of("@OpenSpec(capability = \"user-auth\", scenario = \"covered\")"));
        if (found.contains(Kind.UNTESTED_SCENARIO)) spec.append("#### Scenario: Untested <a id=\"untested\"></a>\n");
        if (found.contains(Kind.MISSING_SLUG)) spec.append("#### Scenario: No slug\n");
        if (found.contains(Kind.INVALID_SLUG)) spec.append("#### Scenario: Upper <a id=\"Upper\"></a>\n");
        if (found.contains(Kind.DUPLICATE_SLUG)) {
            spec.append("#### Scenario: Dup A <a id=\"dup\"></a>\n#### Scenario: Dup B <a id=\"dup\"></a>\n");
            links.add("@OpenSpec(capability = \"user-auth\", scenario = \"dup\")");
        }
        if (found.contains(Kind.UNKNOWN_REFERENCE)) links.add("@OpenSpec(capability = \"no-such-capability\")");
        write("openspec/specs/user-auth/spec.md", spec.toString());
        test("LoginTest", String.join("\n    ", links));
    }

    /** Runs the check on {@link #project}([found], [failOn]) and asserts it fails exactly on the enabled kinds found. */
    void assertCheck(Set<Kind> found, Set<Kind> failOn) throws IOException {
        project(found, failOn);
        Set<Kind> failing = EnumSet.noneOf(Kind.class);
        failing.addAll(found);
        failing.retainAll(failOn);

        BuildResult result = failing.isEmpty() ? runner("specAnchorCheck").build() : runner("specAnchorCheck").buildAndFail();
        String output = result.getOutput();

        assertEquals(failing.isEmpty() ? TaskOutcome.SUCCESS : TaskOutcome.FAILED, result.task(":specAnchorCheck").getOutcome(), output);
        assertTrue(output.contains(found.isEmpty() ? "spec-anchor: no findings" : "spec-anchor: " + describe(found) + "\n"), output);
        if (!failing.isEmpty()) {
            assertTrue(output.contains("spec-anchor check failed: " + describe(failing) + " ("), output);
        }
    }

    static Set<Kind> kinds(int mask) {
        Set<Kind> kinds = EnumSet.noneOf(Kind.class);
        for (Kind kind : Kind.values()) if ((mask & (1 << kind.ordinal())) != 0) kinds.add(kind);
        return kinds;
    }

    @Test
    @OpenSpec(capability = "build-spec-check", scenario = "reports-only-by-default")
    void reportsOnlyByDefault() throws IOException {
        assertCheck(EnumSet.allOf(Kind.class), EnumSet.noneOf(Kind.class));
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    @OpenSpec(capability = "build-spec-check", scenario = "fails-on-enabled-kind")
    void failsOnEnabledKind(Kind kind) throws IOException {
        assertCheck(EnumSet.of(kind), EnumSet.of(kind));
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    @OpenSpec(capability = "build-spec-check", scenario = "ignores-disabled-kinds")
    void ignoresDisabledKinds(Kind kind) throws IOException {
        assertCheck(EnumSet.of(kind), EnumSet.complementOf(EnumSet.of(kind)));
    }

    @Test
    @OpenSpec(capability = "build-spec-check", scenario = "passes-without-findings")
    void passesWithoutFindings() throws IOException {
        assertCheck(EnumSet.noneOf(Kind.class), EnumSet.allOf(Kind.class));
    }

    @Test
    @OpenSpec(capability = "build-spec-check", scenario = "names-all-failing-kinds")
    void namesAllFailingKinds() throws IOException {
        javaProject("failOnUntestedScenario = true\nfailOnUnknownReference = true");
        write("openspec/specs/user-auth/spec.md", VALID + "#### Scenario: Wrong password <a id=\"wrong-password\"></a>\n#### Scenario: No slug\n");
        test("LoginTest", "@OpenSpec(capability = \"no-such-capability\")");

        BuildResult result = runner("specAnchorCheck").buildAndFail();

        assertTrue(result.getOutput().contains("missing-slug openspec/specs/user-auth/spec.md:3"), result.getOutput());
        assertTrue(result.getOutput().contains("spec-anchor check failed: 2 untested-scenario, 1 unknown-reference ("), result.getOutput());
    }

    /**
     * All 32 x 32 combinations of toggles and found kinds, i.e. 1024 builds. Too slow for every build, so it only runs
     * with {@code -PspecAnchor.shard=k/n}, which runs the combinations whose index modulo n is k ({@code 0/1} for all).
     */
    static Stream<Arguments> combinations() {
        String shard = System.getProperty("specAnchor.shard", "");
        if (!shard.matches("\\d+/\\d+")) return Stream.empty();
        int k = Integer.parseInt(shard.split("/")[0]);
        int n = Integer.parseInt(shard.split("/")[1]);
        return IntStream.range(0, 32 * 32).filter(i -> i % n == k).mapToObj(i -> Arguments.of(kinds(i / 32), kinds(i % 32)));
    }

    @ParameterizedTest(name = "failOn={0} found={1}")
    @MethodSource("combinations")
    @EnabledIfSystemProperty(named = "specAnchor.shard", matches = "\\d+/\\d+")
    @OpenSpec(capability = "build-spec-check", scenario = "fails-only-on-enabled-found-kinds")
    void failsOnlyOnEnabledFoundKinds(Set<Kind> failOn, Set<Kind> found) throws IOException {
        assertCheck(found, failOn);
    }
}
