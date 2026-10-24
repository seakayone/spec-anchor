package org.kleinb.openspec.gradle.check;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.kleinb.openspec.gradle.check.Finding.Kind;
import org.kleinb.openspec.gradle.check.Specs.Scenario;
import org.kleinb.openspec.gradle.check.Specs.SpecFile;

/** Checks specs against links. */
public final class Checker {

    private Checker() { }

    /** Findings for [specs] and [links]; spec locations are relative to [base]. */
    public static List<Finding> check(List<SpecFile> specs, List<Link> links, Path base) {
        Map<String, Set<String>> slugs = new HashMap<>();
        for (SpecFile spec : specs) {
            Set<String> capabilitySlugs = slugs.computeIfAbsent(spec.capability(), c -> new HashSet<>());
            spec.scenarios().stream().map(Scenario::slug).filter(Objects::nonNull).forEach(capabilitySlugs::add);
        }
        Set<String> tested = links.stream()
            .filter(link -> !link.scenario().isEmpty())
            .map(link -> link.capability() + "#" + link.scenario())
            .collect(Collectors.toSet());

        List<Finding> findings = new ArrayList<>();
        for (SpecFile spec : specs) {
            String path = base.relativize(spec.path()).toString().replace('\\', '/');
            Map<String, Long> slugCounts = spec.scenarios().stream()
                .map(Scenario::slug).filter(Objects::nonNull)
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));
            for (Scenario scenario : spec.scenarios()) {
                String location = path + ":" + scenario.line();
                if (scenario.invalidAnchor()) {
                    findings.add(new Finding(Kind.INVALID_SLUG, location, "scenario '" + scenario.name() + "' has an invalid slug anchor"));
                } else if (scenario.slug() == null) {
                    findings.add(new Finding(Kind.MISSING_SLUG, location, "scenario '" + scenario.name() + "' has no slug anchor"));
                } else {
                    if (slugCounts.get(scenario.slug()) > 1) {
                        findings.add(new Finding(Kind.DUPLICATE_SLUG, location, "slug '" + scenario.slug() + "' is used by more than one scenario in this file"));
                    }
                    if (!tested.contains(spec.capability() + "#" + scenario.slug())) {
                        findings.add(new Finding(Kind.UNTESTED_SCENARIO, location, "no test linked to scenario '" + scenario.slug() + "'"));
                    }
                }
            }
        }
        for (Link link : links) {
            Set<String> capabilitySlugs = slugs.get(link.capability());
            if (capabilitySlugs == null) {
                findings.add(new Finding(Kind.UNKNOWN_REFERENCE, link.test(), "unknown capability '" + link.capability() + "'"));
            } else if (!link.scenario().isEmpty() && !capabilitySlugs.contains(link.scenario())) {
                findings.add(new Finding(Kind.UNKNOWN_REFERENCE, link.test(), "unknown scenario '" + link.scenario() + "' in capability '" + link.capability() + "'"));
            }
        }
        return findings;
    }
}
