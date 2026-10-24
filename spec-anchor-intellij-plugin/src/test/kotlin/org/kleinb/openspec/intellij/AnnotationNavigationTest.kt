package org.kleinb.openspec.intellij

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import org.intellij.plugins.markdown.lang.psi.impl.MarkdownHeader
import org.kleinb.openspec.annotation.OpenSpec

class AnnotationNavigationTest : OpenSpecTestCase() {

    @OpenSpec(capability = "ide-spec-navigation", scenario = "goto-capability")
    fun testGoToCapability() {
        val spec = addSpec("user-auth", USER_AUTH)
        assertEquals(listOf(spec), resolveAtCaret("""class T { @OpenSpec(capability = "user-<caret>auth") void t() {} }"""))
    }

    @OpenSpec(capability = "ide-spec-navigation", scenario = "goto-scenario")
    fun testGoToScenario() {
        addSpec("user-auth", USER_AUTH)
        val target = resolveAtCaret("""class T { @OpenSpec(capability = "user-auth", scenario = "valid-<caret>credentials") void t() {} }""").single()
        assertTrue(target.text, target is MarkdownHeader && target.text.startsWith("#### Scenario: Valid credentials"))
    }

    @OpenSpec(capability = "ide-spec-navigation", scenario = "goto-scenario")
    fun testGoToScenarioInsideContainer() {
        addSpec("user-auth", USER_AUTH)
        val targets = resolveAtCaret("""class T { @OpenSpecs({@OpenSpec(capability = "user-auth", scenario = "wrong-<caret>password")}) void t() {} }""")
        assertEquals(1, targets.size)
    }

    @OpenSpec(capability = "ide-spec-navigation", scenario = "main-and-change-targets")
    fun testMainAndChangeTargets() {
        val main = addSpec("user-auth", USER_AUTH)
        val change = addChangeSpec("add-login", "user-auth", USER_AUTH)
        val targets = resolveAtCaret("""class T { @OpenSpec(capability = "user-auth", scenario = "valid-<caret>credentials") void t() {} }""")
        assertEquals(setOf(main, change), targets.map { it.containingFile }.toSet())
    }

    @OpenSpec(capability = "ide-spec-navigation", scenario = "slug-case-sensitive")
    fun testSlugWithDifferentCaseDoesNotResolve() {
        addSpec("user-auth", USER_AUTH)
        assertEmpty(resolveAtCaret("""class T { @OpenSpec(capability = "user-auth", scenario = "Valid-<caret>Credentials") void t() {} }"""))
    }

    @OpenSpec(capability = "ide-spec-navigation", scenario = "slug-scoped-to-capability")
    fun testSlugScopedToCapability() {
        val a = addSpec("a", USER_AUTH)
        addSpec("b", USER_AUTH)
        val targets = resolveAtCaret("""class T { @OpenSpec(capability = "a", scenario = "valid-<caret>credentials") void t() {} }""")
        assertEquals(listOf<PsiFile>(a), targets.map { it.containingFile })
    }

    @OpenSpec(capability = "ide-spec-navigation", scenario = "name-does-not-resolve")
    fun testScenarioNameDoesNotResolve() {
        addSpec("user-auth", USER_AUTH)
        assertEmpty(resolveAtCaret("""class T { @OpenSpec(capability = "user-auth", scenario = "Valid <caret>credentials") void t() {} }"""))
    }

    @OpenSpec(capability = "ide-spec-navigation", scenario = "unknown-capability")
    fun testUnknownCapabilityIsHighlighted() {
        addSpec("user-auth", USER_AUTH)
        myFixture.configureByText(
            "T.java",
            """
            import org.kleinb.openspec.annotation.OpenSpec;
            class T { @OpenSpec(capability = "<error descr="Unknown OpenSpec capability 'nope'">nope</error>") void t() {} }
            """.trimIndent(),
        )
        myFixture.checkHighlighting()
    }

    @OpenSpec(capability = "ide-spec-navigation", scenario = "unknown-scenario")
    fun testUnknownScenarioIsHighlighted() {
        addSpec("user-auth", USER_AUTH)
        myFixture.configureByText(
            "T.java",
            """
            import org.kleinb.openspec.annotation.OpenSpec;
            class T {
                @OpenSpec(capability = "user-auth", scenario = "<error descr="Unknown scenario slug 'nope' in this OpenSpec capability">nope</error>") void t() {}
                @OpenSpec(capability = "user-auth", scenario = "valid-credentials") void u() {}
            }
            """.trimIndent(),
        )
        myFixture.checkHighlighting()
    }

    @OpenSpec(capability = "ide-spec-navigation", scenario = "annotation-gutter")
    fun testGutterOnAnnotatedTest() {
        addSpec("user-auth", USER_AUTH)
        val test = addTestClass(
            """
            @OpenSpec(capability = "user-auth")
            class LoginTest {
                @OpenSpec(capability = "user-auth", scenario = "valid-credentials") void valid() {}
                @OpenSpec(capability = "user-auth", scenario = "nope") void unresolved() {}
            }
            """
        ).containingFile
        val gutters = gutterTargets(test, TestLineMarkerProvider.TOOLTIP)
        // Lines 0-1 are the imports added by addTestClass.
        assertEquals(setOf(2, 4), gutters.keys)
        assertInstanceOf(gutters.getValue(2).single(), PsiFile::class.java)
        assertTrue(gutters.getValue(4).single().text.startsWith("#### Scenario: Valid credentials"))
    }

    private fun editSpec(spec: PsiFile, from: String, to: String) {
        val document = PsiDocumentManager.getInstance(project).getDocument(spec)!!
        WriteCommandAction.runWriteCommandAction(project) {
            document.setText(document.text.replace(from, to))
            PsiDocumentManager.getInstance(project).commitDocument(document)
        }
    }

    @OpenSpec(capability = "ide-spec-navigation", scenario = "rename-scenario")
    fun testRenamingScenarioKeepsLink() {
        val spec = addSpec("user-auth", USER_AUTH)
        // The resolved annotation is also the scenario's one linked test.
        val annotation = """class T { @OpenSpec(capability = "user-auth", scenario = "valid-<caret>credentials") void t() {} }"""
        assertEquals(1, resolveAtCaret(annotation).size)

        editSpec(spec, "Scenario: Valid credentials", "Scenario: Correct username and password")

        assertTrue(resolveAtCaret(annotation).single().text.contains("Correct username and password"))
        assertEquals(1, gutterTargets(spec, SpecLineMarkerProvider.TOOLTIP)[6]?.size)
    }

    @OpenSpec(capability = "ide-spec-navigation", scenario = "change-slug")
    fun testChangingSlugBreaksLink() {
        val spec = addSpec("user-auth", USER_AUTH)
        val annotation = """class T { @OpenSpec(capability = "user-auth", scenario = "valid-<caret>credentials") void t() {} }"""
        assertEquals(1, resolveAtCaret(annotation).size)

        editSpec(spec, """<a id="valid-credentials">""", """<a id="correct-credentials">""")

        assertEmpty(resolveAtCaret(annotation))
    }
}
