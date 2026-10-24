package org.kleinb.openspec.gradle.check;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.kleinb.openspec.annotation.OpenSpec;

class LinksTest {

    @OpenSpec(capability = "user-auth")
    static class Fixture {

        @OpenSpec(capability = "user-auth", scenario = "a")
        @OpenSpec(capability = "user-auth", scenario = "b")
        void twoScenarios() { }

        void notLinked() { }
    }

    /** Overriding a generic method makes javac add an annotated bridge method {@code Object get()}. */
    static class BridgeFixture implements Supplier<String> {

        @OpenSpec(capability = "user-auth", scenario = "a")
        @Override
        public String get() {
            return "";
        }
    }

    private static File testClassesDir() throws URISyntaxException {
        return new File(LinksTest.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    }

    private static List<File> testClasspath() {
        return Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator)).map(File::new).toList();
    }

    @Test
    @OpenSpec(capability = "build-spec-check", scenario = "reads-repeated-annotations")
    void readsRepeatedAnnotations() throws URISyntaxException {
        List<String> warnings = new ArrayList<>();
        List<Link> links = Links.read(List.of(testClassesDir()), testClasspath(), warnings::add).stream()
            .filter(link -> link.test().startsWith(Fixture.class.getName()))
            .toList();

        String fixture = Fixture.class.getName();
        assertEquals(List.of(
            new Link(fixture, "user-auth", ""),
            new Link(fixture + "#twoScenarios", "user-auth", "a"),
            new Link(fixture + "#twoScenarios", "user-auth", "b")), links);
        assertEquals(List.of(), warnings);
    }

    @Test
    void bridgeMethodsAreNotCountedTwice() throws URISyntaxException {
        List<Link> links = Links.read(List.of(testClassesDir()), testClasspath(), message -> { }).stream()
            .filter(link -> link.test().startsWith(BridgeFixture.class.getName()))
            .toList();

        assertEquals(List.of(new Link(BridgeFixture.class.getName() + "#get", "user-auth", "a")), links);
    }

    @Test
    void skipsClassesThatDoNotLoad() throws URISyntaxException {
        // Only the annotations on the classpath: types used in the signatures of other test classes (TestKit, Link) cannot be resolved.
        List<File> annotationsOnly = List.of(new File(System.getProperty("specAnchor.annotationsJar")));
        List<String> warnings = new ArrayList<>();

        List<Link> links = Links.read(List.of(testClassesDir()), annotationsOnly, warnings::add);

        assertTrue(warnings.stream().anyMatch(w -> w.contains("SpecAnchorPluginTest") && w.contains("NoClassDefFoundError")), warnings.toString());
        assertTrue(links.contains(new Link(Fixture.class.getName(), "user-auth", "")), links.toString());
    }

    @Test
    void noLinksWithoutAnnotationOnClasspath() throws URISyntaxException {
        List<File> withoutAnnotations = testClasspath().stream().filter(file -> !file.getName().startsWith("spec-anchor-annotations")).toList();

        assertEquals(List.of(), Links.read(List.of(testClassesDir()), withoutAnnotations, message -> { }));
    }
}
