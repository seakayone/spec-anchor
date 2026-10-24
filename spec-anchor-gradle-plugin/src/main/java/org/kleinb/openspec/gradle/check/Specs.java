package org.kleinb.openspec.gradle.check;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Reads capability specs by the discovery and slug rules of {@code ide-spec-navigation}. */
public final class Specs {

    /** A capability spec: its capability path, file and scenario headings. */
    public record SpecFile(String capability, Path path, List<Scenario> scenarios) { }

    /**
     * A {@code #### Scenario:} heading at 1-based [line]. [slug] is null when the heading has no valid slug anchor;
     * [invalidAnchor] tells an invalid anchor apart from a missing one.
     */
    public record Scenario(int line, String name, String slug, boolean invalidAnchor) { }

    // Same rules as OpenSpecModel.kt in the IntelliJ plugin.
    private static final Pattern SCENARIO_PREFIX = Pattern.compile("^ {0,3}####\\s+Scenario:");
    private static final Pattern SLUG_ANCHOR = Pattern.compile("(<a id=\"([a-z0-9]+(?:-[a-z0-9]+)*)\"></a>)\\s*$");
    private static final Pattern ANY_ANCHOR = Pattern.compile("<a\\b[^>]*>(?:</a>)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern FENCE = Pattern.compile("^ {0,3}(`{3,}|~{3,})");

    private Specs() { }

    /** All capability specs below {@code openspecDir/specs/} and {@code openspecDir/changes/<change>/specs/}, archive excluded. */
    public static List<SpecFile> discover(Path openspecDir) {
        List<Path> roots = new ArrayList<>();
        roots.add(openspecDir.resolve("specs"));
        Path changes = openspecDir.resolve("changes");
        if (Files.isDirectory(changes)) {
            try (Stream<Path> dirs = Files.list(changes)) {
                dirs.filter(dir -> !dir.getFileName().toString().equals("archive"))
                    .map(dir -> dir.resolve("specs"))
                    .sorted()
                    .forEach(roots::add);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        List<SpecFile> specs = new ArrayList<>();
        for (Path root : roots) {
            if (!Files.isDirectory(root)) continue;
            try (Stream<Path> files = Files.walk(root)) {
                for (Path file : files.filter(f -> f.getFileName().toString().equals("spec.md")).sorted().toList()) {
                    Path dir = root.relativize(file.getParent());
                    if (dir.toString().isEmpty()) continue;
                    specs.add(new SpecFile(dir.toString().replace('\\', '/'), file, parse(Files.readAllLines(file))));
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return specs;
    }

    /** The scenario headings of a spec's lines, skipping fenced code blocks. */
    public static List<Scenario> parse(List<String> lines) {
        List<Scenario> scenarios = new ArrayList<>();
        String fence = null;
        for (int i = 0; i < lines.size(); i++) {
            String text = lines.get(i);
            Matcher fenceMatch = FENCE.matcher(text);
            if (fenceMatch.find()) {
                String marker = fenceMatch.group(1);
                if (fence == null) fence = marker;
                else if (marker.charAt(0) == fence.charAt(0) && marker.length() >= fence.length()) fence = null;
                continue;
            }
            if (fence != null) continue;
            Matcher prefix = SCENARIO_PREFIX.matcher(text);
            if (prefix.find()) scenarios.add(parseScenario(i + 1, text, prefix.end()));
        }
        return scenarios;
    }

    private static Scenario parseScenario(int line, String text, int nameStart) {
        long anchors = ANY_ANCHOR.matcher(text).results().count();
        Matcher slug = SLUG_ANCHOR.matcher(text);
        if (anchors == 1 && slug.find()) {
            return new Scenario(line, text.substring(nameStart, slug.start(1)).trim(), slug.group(2), false);
        }
        return new Scenario(line, text.substring(nameStart).trim(), null, anchors > 0);
    }
}
