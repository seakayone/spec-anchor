package org.kleinb.openspec.intellij

import org.kleinb.openspec.annotation.OpenSpec
import org.kleinb.openspec.intellij.ScenarioSlugInspection.Companion.INVALID

class ScenarioSlugInspectionTest : OpenSpecTestCase() {

    private fun check(headings: String) {
        val spec = addSpec("user-auth", "# user-auth\n\n#### Scenario: Plain heading\n\n" + headings.trimIndent())
        myFixture.enableInspections(ScenarioSlugInspection())
        myFixture.configureFromExistingVirtualFile(spec.virtualFile)
        myFixture.checkHighlighting()
    }

    @OpenSpec(capability = "ide-spec-navigation", scenario = "invalid-slug-uppercase")
    fun testUppercaseSlug() =
        check("""#### Scenario: Valid credentials <error descr="$INVALID"><a id="Valid-Credentials"></a></error>""")

    @OpenSpec(capability = "ide-spec-navigation", scenario = "invalid-slug-non-ascii")
    fun testNonAsciiSlug() =
        check("""#### Scenario: Größe <error descr="$INVALID"><a id="größe"></a></error>""")

    @OpenSpec(capability = "ide-spec-navigation", scenario = "invalid-slug-form")
    fun testWrongAnchorForm() = check(
        """
        #### Scenario: Valid credentials <error descr="$INVALID"><a name="valid-credentials"></a></error>

        #### Scenario: Wrong password <error descr="$INVALID"><a id='wrong-password'></a></error>
        """
    )

    @OpenSpec(capability = "ide-spec-navigation", scenario = "invalid-slug-position")
    fun testAnchorNotAtEnd() =
        check("""#### Scenario: <error descr="$INVALID"><a id="valid-credentials"></a></error> Valid credentials""")

    @OpenSpec(capability = "ide-spec-navigation", scenario = "duplicate-slug")
    fun testDuplicateSlug() = check(
        """
        #### Scenario: Valid input <warning descr="Duplicate scenario slug 'valid-input'"><a id="valid-input"></a></warning>

        #### Scenario: Also valid input <warning descr="Duplicate scenario slug 'valid-input'"><a id="valid-input"></a></warning>

        #### Scenario: Unique <a id="unique"></a>
        """
    )
}
