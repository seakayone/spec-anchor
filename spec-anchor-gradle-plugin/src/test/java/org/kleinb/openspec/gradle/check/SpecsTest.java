package org.kleinb.openspec.gradle.check;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.kleinb.openspec.annotation.OpenSpec;
import org.kleinb.openspec.gradle.check.Specs.Scenario;
import org.kleinb.openspec.gradle.check.Specs.SpecFile;

class SpecsTest {

    @TempDir
    Path dir;

    static void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    private List<String> capabilities(Path openspecDir) {
        return Specs.discover(openspecDir).stream().map(SpecFile::capability).toList();
    }

    @Test
    @OpenSpec(capability = "build-spec-check", scenario = "discovers-main-and-change-specs")
    void discoversMainAndChangeSpecs() throws IOException {
        write(dir.resolve("openspec/specs/user-auth/spec.md"), "# a\n");
        write(dir.resolve("openspec/specs/identity/user-auth/spec.md"), "# b\n");
        write(dir.resolve("openspec/changes/add-login/specs/billing/spec.md"), "# c\n");

        assertEquals(List.of("identity/user-auth", "user-auth", "billing"), capabilities(dir.resolve("openspec")));
    }

    @Test
    @OpenSpec(capability = "build-spec-check", scenario = "ignores-archived-specs")
    void ignoresArchivedSpecs() throws IOException {
        write(dir.resolve("openspec/changes/archive/2026-01-01-add-login/specs/user-auth/spec.md"), "#### Scenario: A\n");

        assertEquals(List.of(), capabilities(dir.resolve("openspec")));
    }

    @Test
    @OpenSpec(capability = "build-spec-check", scenario = "custom-openspec-dir")
    void customOpenspecDir() throws IOException {
        write(dir.resolve("docs/openspec/specs/user-auth/spec.md"), "# a\n");
        write(dir.resolve("docs/openspec/changes/add-login/specs/billing/spec.md"), "# b\n");

        assertEquals(List.of("user-auth", "billing"), capabilities(dir.resolve("docs/openspec")));
    }

    @Test
    @OpenSpec(capability = "build-spec-check", scenario = "ignores-code-blocks")
    void ignoresCodeBlocks() {
        List<Scenario> scenarios = Specs.parse(List.of(
            "```markdown",
            "#### Scenario: In a fence <a id=\"fenced\"></a>",
            "```",
            "~~~",
            "#### Scenario: In a tilde fence",
            "~~~",
            "#### Scenario: Real <a id=\"real\"></a>"));

        assertEquals(1, scenarios.size());
        assertEquals("real", scenarios.get(0).slug());
        assertEquals(7, scenarios.get(0).line());
    }

    @Test
    void parsesSlugMissingAndInvalidAnchors() {
        List<Scenario> scenarios = Specs.parse(List.of(
            "#### Scenario: Valid credentials <a id=\"valid-credentials\"></a>",
            "#### Scenario: No slug",
            "#### Scenario: Upper <a id=\"Valid-Credentials\"></a>",
            "#### Scenario: <a id=\"valid-credentials\"></a> Not at end",
            "#### Scenario: Single quotes <a id='x'></a>",
            "##### Scenario: Too deep <a id=\"deep\"></a>"));

        assertEquals(5, scenarios.size());
        assertEquals("Valid credentials", scenarios.get(0).name());
        assertEquals("valid-credentials", scenarios.get(0).slug());
        assertNull(scenarios.get(1).slug());
        assertFalse(scenarios.get(1).invalidAnchor());
        for (Scenario invalid : scenarios.subList(2, 5)) {
            assertNull(invalid.slug());
            assertTrue(invalid.invalidAnchor());
        }
    }
}
