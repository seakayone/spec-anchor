package org.kleinb.openspec.intellij

import com.intellij.psi.PsiFile
import org.kleinb.openspec.annotation.OpenSpec

class UntestedScenarioInspectionTest : OpenSpecTestCase() {

    private val valid = """#### Scenario: Valid credentials <a id="valid-credentials"></a>"""
    private val wrongPassword = """#### Scenario: Wrong password <a id="wrong-password"></a>"""

    private fun untested(heading: String, slug: String) = """<warning descr="No test linked to scenario '$slug'">$heading</warning>"""

    private fun spec(vararg headings: String) = "# user-auth\n\n" + headings.joinToString("\n\n") { "$it\n- **WHEN** x\n- **THEN** y" } + "\n"

    private fun check(spec: PsiFile) {
        myFixture.enableInspections(UntestedScenarioInspection())
        myFixture.configureFromExistingVirtualFile(spec.virtualFile)
        myFixture.checkHighlighting()
    }

    @OpenSpec(capability = "ide-spec-navigation", scenario = "untested-scenario-flagged")
    @OpenSpec(capability = "ide-spec-navigation", scenario = "tested-scenario-not-flagged")
    fun testOnlyUntestedScenarioIsFlagged() {
        addTestClass("""class LoginTest { @OpenSpec(capability = "user-auth", scenario = "valid-credentials") void valid() {} }""")
        check(addSpec("user-auth", spec(valid, untested(wrongPassword, "wrong-password"))))
    }

    @OpenSpec(capability = "ide-spec-navigation", scenario = "capability-test-does-not-cover")
    fun testCapabilityLevelTestDoesNotCoverScenarios() {
        addTestClass("""@OpenSpec(capability = "user-auth") class LoginTest {}""")
        check(addSpec("user-auth", spec(untested(valid, "valid-credentials"), untested(wrongPassword, "wrong-password"))))
    }

    @OpenSpec(capability = "ide-spec-navigation", scenario = "unslugged-scenario-not-flagged")
    fun testScenarioWithoutSlugIsNotFlagged() {
        check(addSpec("user-auth", spec("#### Scenario: Valid credentials")))
    }

    @OpenSpec(capability = "ide-spec-navigation", scenario = "untested-in-change-spec")
    fun testUntestedScenarioInActiveChangeIsFlagged() {
        check(addChangeSpec("add-login", "user-auth", spec(untested(valid, "valid-credentials"))))
    }

    @OpenSpec(capability = "ide-spec-navigation", scenario = "untested-in-change-spec")
    fun testArchivedChangeIsNotChecked() {
        check(myFixture.addFileToProject("openspec/changes/archive/2026-01-01-add-login/specs/user-auth/spec.md", spec(valid)))
    }

    @OpenSpec(capability = "ide-spec-navigation", scenario = "coverage-follows-edits")
    fun testAddingTestClearsWarning() {
        val spec = addSpec("user-auth", spec(untested(valid, "valid-credentials")))
        check(spec)

        addTestClass("""class LoginTest { @OpenSpec(capability = "user-auth", scenario = "valid-credentials") void valid() {} }""")

        myFixture.checkHighlighting() // markup was consumed by the first check, so this expects no warning
    }
}
