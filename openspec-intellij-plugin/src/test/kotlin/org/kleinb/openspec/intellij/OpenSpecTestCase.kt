package org.kleinb.openspec.intellij

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase

/** Light Java fixture with the `@OpenSpec` annotation on the classpath and helpers to add OpenSpec spec files. */
abstract class OpenSpecTestCase : LightJavaCodeInsightFixtureTestCase() {

    override fun setUp() {
        super.setUp()
        myFixture.addClass(
            """
            package org.kleinb.openspec.annotation;
            @java.lang.annotation.Repeatable(OpenSpecs.class)
            public @interface OpenSpec { String capability(); String scenario() default ""; }
            """.trimIndent()
        )
        myFixture.addClass(
            """
            package org.kleinb.openspec.annotation;
            public @interface OpenSpecs { OpenSpec[] value(); }
            """.trimIndent()
        )
    }

    /** Adds `openspec/specs/<capability>/spec.md`. */
    protected fun addSpec(capability: String, text: String): PsiFile =
        myFixture.addFileToProject("openspec/specs/$capability/spec.md", text.trimIndent())

    /** Adds `openspec/changes/<change>/specs/<capability>/spec.md`. */
    protected fun addChangeSpec(change: String, capability: String, text: String): PsiFile =
        myFixture.addFileToProject("openspec/changes/$change/specs/$capability/spec.md", text.trimIndent())

    /** Adds a Java test class; `@OpenSpec` and `@OpenSpecs` are imported. */
    protected fun addTestClass(body: String) = myFixture.addClass(
        "import org.kleinb.openspec.annotation.OpenSpec;\nimport org.kleinb.openspec.annotation.OpenSpecs;\n" + body.trimIndent()
    )

    /** Opens [file] in the editor and returns the targets of its gutter markers with [tooltip], by 0-based line. */
    protected fun gutterTargets(file: PsiFile, tooltip: String): Map<Int, List<PsiElement>> {
        myFixture.configureFromExistingVirtualFile(file.virtualFile)
        return myFixture.findAllGutters().filter { it.tooltipText == tooltip }.associate { gutter ->
            val info = (gutter as LineMarkerInfo.LineMarkerGutterIconRenderer<*>).lineMarkerInfo as RelatedItemLineMarkerInfo<*>
            myFixture.editor.document.getLineNumber(info.startOffset) to info.createGotoRelatedItems().mapNotNull { it.element }
        }
    }

    /** Opens a Java file with `<caret>` in an annotation value and returns what that value resolves to. */
    protected fun resolveAtCaret(javaBody: String): List<PsiElement> {
        myFixture.configureByText(
            "AtCaret.java",
            "import org.kleinb.openspec.annotation.OpenSpec;\nimport org.kleinb.openspec.annotation.OpenSpecs;\n" + javaBody.trimIndent(),
        )
        val reference = myFixture.getReferenceAtCaretPositionWithAssertion() as PsiPolyVariantReference
        return reference.multiResolve(false).mapNotNull { it.element }
    }

    companion object {
        const val USER_AUTH = """
            # user-auth Specification

            ## Requirements
            ### Requirement: Login
            The system SHALL log users in.

            #### Scenario: Valid credentials <a id="valid-credentials"></a>
            - **WHEN** x
            - **THEN** y

            #### Scenario: Wrong password <a id="wrong-password"></a>
            - **WHEN** x
            - **THEN** y
        """
    }
}
