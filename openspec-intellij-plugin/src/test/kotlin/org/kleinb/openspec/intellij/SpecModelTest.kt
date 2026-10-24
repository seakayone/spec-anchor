package org.kleinb.openspec.intellij

import org.kleinb.openspec.annotation.OpenSpec

class SpecModelTest : OpenSpecTestCase() {

    @OpenSpec(capability = "ide-spec-navigation", scenario = "flat-capability")
    fun testFlatCapability() {
        val spec = addSpec("user-auth", USER_AUTH)
        assertEquals(mapOf("user-auth" to listOf(spec)), specFiles(project))
    }

    @OpenSpec(capability = "ide-spec-navigation", scenario = "nested-capability")
    fun testNestedCapability() {
        addSpec("identity/user-auth", USER_AUTH)
        assertEquals(setOf("identity/user-auth"), specFiles(project).keys)
    }

    @OpenSpec(capability = "ide-spec-navigation", scenario = "change-spec-included")
    fun testCapabilityInActiveChange() {
        val spec = addChangeSpec("add-login", "user-auth", USER_AUTH)
        assertEquals(mapOf("user-auth" to listOf(spec)), specFiles(project))
    }

    @OpenSpec(capability = "ide-spec-navigation", scenario = "archived-change-ignored")
    fun testArchivedChangeIsIgnored() {
        myFixture.addFileToProject("openspec/changes/archive/2026-01-01-add-login/specs/user-auth/spec.md", USER_AUTH)
        myFixture.addFileToProject("openspec/changes/add-login/proposal.md", "# Proposal")
        assertEmpty(specFiles(project).keys)
    }

    @OpenSpec(capability = "ide-spec-navigation")
    fun testCapabilityInSeveralRoots() {
        addSpec("user-auth", USER_AUTH)
        myFixture.addFileToProject("module-a/openspec/specs/user-auth/spec.md", USER_AUTH)
        assertEquals(2, specFiles(project).getValue("user-auth").size)
    }

    @OpenSpec(capability = "ide-spec-navigation", scenario = "heading-with-slug")
    fun testHeadingWithSlug() {
        val scenario = scenarios(addSpec("user-auth", USER_AUTH)).first()
        assertEquals("Valid credentials", scenario.name)
        assertEquals("valid-credentials", scenario.slug)
        assertEquals("<a id=\"valid-credentials\"></a>", scenario.anchorRange!!.substring(scenario.header.text))
    }

    @OpenSpec(capability = "ide-spec-navigation", scenario = "heading-without-slug")
    fun testHeadingWithoutSlug() {
        val scenario = scenarios(addSpec("user-auth", "# user-auth\n\n#### Scenario: Valid credentials\n")).single()
        assertEquals("Valid credentials", scenario.name)
        assertNull(scenario.slug)
        assertNull(scenario.anchorRange)
    }

    @OpenSpec(capability = "ide-spec-navigation", scenario = "invalid-slug-uppercase")
    @OpenSpec(capability = "ide-spec-navigation", scenario = "invalid-slug-non-ascii")
    @OpenSpec(capability = "ide-spec-navigation", scenario = "invalid-slug-form")
    @OpenSpec(capability = "ide-spec-navigation", scenario = "invalid-slug-position")
    fun testInvalidAnchorsHaveNoSlug() {
        val headings = listOf(
            """Valid credentials <a id="Valid-Credentials"></a>""",
            """Größe <a id="größe"></a>""",
            """Valid credentials <a name="valid-credentials"></a>""",
            """Valid credentials <a id='valid-credentials'></a>""",
            """<a id="valid-credentials"></a> Valid credentials""",
            """Valid credentials <a id="other"></a> <a id="valid-credentials"></a>""",
        )
        val spec = addSpec("user-auth", "# user-auth\n\n" + headings.joinToString("\n\n") { "#### Scenario: $it" })
        val scenarios = scenarios(spec)
        assertEquals(headings.size, scenarios.size)
        scenarios.forEach {
            assertNull(it.header.text, it.slug)
            assertTrue(it.header.text, it.anchorRange!!.substring(it.header.text).startsWith("<a "))
        }
    }

    @OpenSpec(capability = "ide-spec-navigation")
    fun testOnlyLevelFourScenarioHeadings() {
        val spec = addSpec("user-auth", "# user-auth\n\n### Scenario: Not a scenario <a id=\"x\"></a>\n\n##### Scenario: Neither <a id=\"y\"></a>\n")
        assertEmpty(scenarios(spec))
    }
}
