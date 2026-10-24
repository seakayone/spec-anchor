package org.kleinb.openspec.intellij

import com.intellij.psi.PsiNamedElement
import org.kleinb.openspec.annotation.OpenSpec
import org.kleinb.openspec.intellij.SpecLineMarkerProvider.Companion.TOOLTIP

class SpecLineMarkerTest : OpenSpecTestCase() {

    // Lines (0-based) of USER_AUTH's first heading and its two scenario headings.
    private val capabilityLine = 0
    private val validLine = 6
    private val wrongPasswordLine = 10

    private fun names(targets: List<*>?) = targets.orEmpty().map { (it as PsiNamedElement).name }.toSet()

    @OpenSpec(capability = "ide-spec-navigation", scenario = "scenario-one-test")
    fun testScenarioWithOneLinkedTest() {
        val spec = addSpec("user-auth", USER_AUTH)
        addTestClass("""class LoginTest { @OpenSpec(capability = "user-auth", scenario = "valid-credentials") void valid() {} }""")
        assertEquals(setOf("valid"), names(gutterTargets(spec, TOOLTIP)[validLine]))
    }

    @OpenSpec(capability = "ide-spec-navigation", scenario = "scenario-several-tests")
    fun testScenarioWithSeveralLinkedTests() {
        val spec = addSpec("user-auth", USER_AUTH)
        addTestClass(
            """
            class LoginTest {
                @OpenSpec(capability = "user-auth", scenario = "valid-credentials") void one() {}
                @OpenSpec(capability = "user-auth", scenario = "valid-credentials") void two() {}
            }
            """
        )
        assertEquals(setOf("one", "two"), names(gutterTargets(spec, TOOLTIP)[validLine]))
    }

    @OpenSpec(capability = "ide-spec-navigation", scenario = "scenario-no-tests")
    fun testScenarioWithoutTestsHasNoMarker() {
        val spec = addSpec("user-auth", USER_AUTH)
        addTestClass("""class LoginTest { @OpenSpec(capability = "user-auth", scenario = "valid-credentials") void valid() {} }""")
        val gutters = gutterTargets(spec, TOOLTIP)
        assertFalse(gutters.containsKey(wrongPasswordLine))
        assertEquals(setOf(capabilityLine, validLine), gutters.keys)
    }

    @OpenSpec(capability = "ide-spec-navigation", scenario = "capability-all-tests")
    fun testCapabilityMarkerListsAllLinkedTests() {
        val spec = addSpec("user-auth", USER_AUTH)
        addTestClass(
            """
            @OpenSpec(capability = "user-auth")
            class LoginTest { @OpenSpec(capability = "user-auth", scenario = "valid-credentials") void valid() {} }
            """
        )
        assertEquals(setOf("LoginTest", "valid"), names(gutterTargets(spec, TOOLTIP)[capabilityLine]))
    }

    @OpenSpec(capability = "ide-spec-navigation", scenario = "change-spec-included")
    fun testMarkersInActiveChangeSpec() {
        val spec = addChangeSpec("add-login", "user-auth", USER_AUTH)
        addTestClass("""class LoginTest { @OpenSpec(capability = "user-auth", scenario = "valid-credentials") void valid() {} }""")
        assertEquals(setOf("valid"), names(gutterTargets(spec, TOOLTIP)[validLine]))
    }

    @OpenSpec(capability = "ide-spec-navigation", scenario = "archived-change-ignored")
    fun testNoMarkersInArchivedChangeSpec() {
        val spec = myFixture.addFileToProject("openspec/changes/archive/2026-01-01-add-login/specs/user-auth/spec.md", USER_AUTH.trimIndent())
        addTestClass("""class LoginTest { @OpenSpec(capability = "user-auth", scenario = "valid-credentials") void valid() {} }""")
        assertEmpty(gutterTargets(spec, TOOLTIP).keys)
    }
}
