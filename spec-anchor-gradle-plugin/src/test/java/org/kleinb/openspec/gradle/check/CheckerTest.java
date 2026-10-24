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

    // --- active changes supersede living requirements --------------------------------------------------------------

    private static final String LIVING = """
        ## Requirements

        ### Requirement: Login
        #### Scenario: A <a id="a"></a>
        #### Scenario: B <a id="b"></a>

        ### Requirement: Legacy  login
        #### Scenario: Legacy <a id="legacy"></a>
        """;

    private static final String CHANGE = "openspec/changes/drop-legacy/specs/user-auth/spec.md";

    private static final String REMOVE_LEGACY = """
        ## REMOVED Requirements

        ### Requirement: Legacy login
        **Reason**: replaced
        **Migration**: use Login
        """;

    @Test
    @OpenSpec(capability = "build-spec-check", scenario = "removed-scenarios-not-reported")
    @OpenSpec(capability = "build-spec-check", scenario = "requirement-names-match-loosely")
    void removedScenariosNotReported() throws IOException {
        spec(USER_AUTH, LIVING);
        spec(CHANGE, REMOVE_LEGACY);

        assertEquals(List.of(), findings(new Link("T#a", "user-auth", "a"), new Link("T#b", "user-auth", "b")));
    }

    @Test
    @OpenSpec(capability = "build-spec-check", scenario = "unaffected-requirements-checked")
    void unaffectedRequirementsChecked() throws IOException {
        spec(USER_AUTH, LIVING);
        spec(CHANGE, REMOVE_LEGACY);

        assertEquals(List.of("untested-scenario openspec/specs/user-auth/spec.md:5: no test linked to scenario 'b'"),
            findings(new Link("T#a", "user-auth", "a")));
    }

    @Test
    @OpenSpec(capability = "build-spec-check", scenario = "modified-requirement-replaces-living")
    void modifiedRequirementReplacesLiving() throws IOException {
        spec(USER_AUTH, "### Requirement: Login\n#### Scenario: Valid <a id=\"valid-credentials\"></a>\n");
        spec(CHANGE, "## MODIFIED Requirements\n\n### Requirement: Login\n#### Scenario: Valid <a id=\"valid-credentials\"></a>\n");

        assertEquals(List.of("untested-scenario openspec/changes/drop-legacy/specs/user-auth/spec.md:4: no test linked to scenario 'valid-credentials'"),
            findings());
    }

    @Test
    @OpenSpec(capability = "build-spec-check", scenario = "reports-removed-reference")
    void reportsRemovedReference() throws IOException {
        spec(USER_AUTH, LIVING);
        spec(CHANGE, REMOVE_LEGACY);

        assertEquals(List.of("removed-reference T#legacy: scenario 'legacy' in capability 'user-auth' is removed by an active change"),
            findings(new Link("T#a", "user-auth", "a"), new Link("T#b", "user-auth", "b"), new Link("T#legacy", "user-auth", "legacy")));
    }

    @Test
    @OpenSpec(capability = "build-spec-check", scenario = "modified-drops-scenario")
    @OpenSpec(capability = "build-spec-check", scenario = "kept-scenario-not-removed")
    void modifiedDropsScenario() throws IOException {
        spec(USER_AUTH, LIVING);
        spec(CHANGE, "## MODIFIED Requirements\n\n### Requirement: Login\n#### Scenario: A <a id=\"a\"></a>\n");

        assertEquals(List.of("removed-reference T#b: scenario 'b' in capability 'user-auth' is removed by an active change"),
            findings(new Link("T#a", "user-auth", "a"), new Link("T#b", "user-auth", "b"), new Link("T#legacy", "user-auth", "legacy")));
    }

    @Test
    void scenariosInRemovedSectionAreIgnored() throws IOException {
        spec(USER_AUTH, LIVING);
        spec(CHANGE, REMOVE_LEGACY + "#### Scenario: Leftover\n");

        assertEquals(List.of(), findings(new Link("T#a", "user-auth", "a"), new Link("T#b", "user-auth", "b")));
    }
}
