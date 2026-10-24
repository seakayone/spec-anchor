package org.kleinb.openspec.gradle.check;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.kleinb.openspec.annotation.OpenSpec;

class CheckerTest {

    @TempDir
    Path dir;

    private void spec(String relativePath, String content) throws IOException {
        SpecsTest.write(dir.resolve(relativePath), content);
    }

    private List<String> findings(Link... links) {
        return Checker.check(Specs.discover(dir.resolve("openspec")), List.of(links), dir).stream().map(Finding::toString).toList();
    }

    private static final String USER_AUTH = "openspec/specs/user-auth/spec.md";
    private static final String VALID = "# user-auth\n\n#### Scenario: Valid credentials <a id=\"valid-credentials\"></a>\n";

    @Test
    @OpenSpec(capability = "build-spec-check", scenario = "reports-untested-scenario")
    void reportsUntestedScenario() throws IOException {
        spec(USER_AUTH, VALID);

        assertEquals(List.of("untested-scenario openspec/specs/user-auth/spec.md:3: no test linked to scenario 'valid-credentials'"), findings());
    }

    @Test
    @OpenSpec(capability = "build-spec-check", scenario = "capability-link-does-not-cover")
    void capabilityLinkDoesNotCover() throws IOException {
        spec(USER_AUTH, VALID + "#### Scenario: Wrong password <a id=\"wrong-password\"></a>\n");

        assertEquals(List.of(
            "untested-scenario openspec/specs/user-auth/spec.md:3: no test linked to scenario 'valid-credentials'",
            "untested-scenario openspec/specs/user-auth/spec.md:4: no test linked to scenario 'wrong-password'"),
            findings(new Link("LoginTest", "user-auth", "")));
    }

    @Test
    @OpenSpec(capability = "build-spec-check", scenario = "tested-scenario-not-reported")
    void testedScenarioNotReported() throws IOException {
        spec(USER_AUTH, VALID);

        assertEquals(List.of(), findings(new Link("LoginTest#valid", "user-auth", "valid-credentials")));
    }

    @Test
    @OpenSpec(capability = "build-spec-check", scenario = "reports-missing-slug")
    void reportsMissingSlug() throws IOException {
        spec(USER_AUTH, "#### Scenario: Valid credentials\n");

        assertEquals(List.of("missing-slug openspec/specs/user-auth/spec.md:1: scenario 'Valid credentials' has no slug anchor"), findings());
    }

    @Test
    @OpenSpec(capability = "build-spec-check", scenario = "reports-invalid-slug")
    void reportsInvalidSlug() throws IOException {
        spec(USER_AUTH, "#### Scenario: Valid credentials <a id=\"Valid-Credentials\"></a>\n");

        assertEquals(List.of("invalid-slug openspec/specs/user-auth/spec.md:1: scenario 'Valid credentials <a id=\"Valid-Credentials\"></a>' has an invalid slug anchor"),
            findings());
    }

    @Test
    @OpenSpec(capability = "build-spec-check", scenario = "reports-duplicate-slug")
    void reportsDuplicateSlug() throws IOException {
        spec(USER_AUTH, "#### Scenario: A <a id=\"valid-input\"></a>\n#### Scenario: B <a id=\"valid-input\"></a>\n");

        assertEquals(List.of(
            "duplicate-slug openspec/specs/user-auth/spec.md:1: slug 'valid-input' is used by more than one scenario in this file",
            "duplicate-slug openspec/specs/user-auth/spec.md:2: slug 'valid-input' is used by more than one scenario in this file"),
            findings(new Link("T", "user-auth", "valid-input")));
    }

    @Test
    @OpenSpec(capability = "build-spec-check", scenario = "slug-in-two-files-not-duplicate")
    void slugInTwoFilesNotDuplicate() throws IOException {
        spec(USER_AUTH, "#### Scenario: A <a id=\"valid-input\"></a>\n");
        spec("openspec/changes/add-login/specs/user-auth/spec.md", "#### Scenario: A <a id=\"valid-input\"></a>\n");

        assertEquals(List.of(), findings(new Link("T", "user-auth", "valid-input")));
    }

    @Test
    @OpenSpec(capability = "build-spec-check", scenario = "reports-unknown-capability")
    void reportsUnknownCapability() throws IOException {
        spec(USER_AUTH, "# user-auth\n");

        assertEquals(List.of("unknown-reference com.example.LoginTest: unknown capability 'no-such-capability'"),
            findings(new Link("com.example.LoginTest", "no-such-capability", "")));
    }

    @Test
    @OpenSpec(capability = "build-spec-check", scenario = "reports-unknown-scenario")
    void reportsUnknownScenario() throws IOException {
        spec(USER_AUTH, VALID);
        spec("openspec/changes/add-login/specs/user-auth/spec.md", "#### Scenario: B <a id=\"in-change\"></a>\n");

        assertEquals(List.of("unknown-reference com.example.LoginTest#valid: unknown scenario 'no-such-slug' in capability 'user-auth'"),
            findings(
                new Link("com.example.LoginTest#valid", "user-auth", "valid-credentials"),
                new Link("com.example.LoginTest#change", "user-auth", "in-change"),
                new Link("com.example.LoginTest#valid", "user-auth", "no-such-slug")));
    }

    @Test
    @OpenSpec(capability = "build-spec-check", scenario = "valid-anchor-not-missing")
    @OpenSpec(capability = "build-spec-check", scenario = "valid-anchor-not-invalid")
    void validAnchorIsNeitherMissingNorInvalid() throws IOException {
        spec(USER_AUTH, VALID);

        List<String> findings = findings();

        assertEquals(List.of(), findings.stream().filter(f -> f.startsWith("missing-slug") || f.startsWith("invalid-slug")).toList());
        assertEquals(1, findings.size(), findings.toString()); // only the untested scenario
    }

    @Test
    @OpenSpec(capability = "build-spec-check", scenario = "known-references-not-reported")
    void knownReferencesNotReported() throws IOException {
        spec(USER_AUTH, VALID);
        spec("openspec/changes/add-login/specs/user-auth/spec.md", "#### Scenario: B <a id=\"in-change\"></a>\n");

        assertEquals(List.of(), findings(
            new Link("LoginTest", "user-auth", ""),
            new Link("LoginTest#valid", "user-auth", "valid-credentials"),
            new Link("LoginTest#change", "user-auth", "in-change")));
    }
}
