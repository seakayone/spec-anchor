package org.kleinb.openspec.intellij

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiNamedElement
import org.kleinb.openspec.annotation.OpenSpec

class LinksTest : OpenSpecTestCase() {

    private fun names(capability: String, scenario: String? = null) =
        linkedTests(project, capability, scenario).map { (it as PsiNamedElement).name }.toSet()

    @OpenSpec(capability = "ide-spec-navigation")
    fun testFindsEveryAnnotationForm() {
        addTestClass(
            """
            @OpenSpec(capability = "user-auth")
            class LoginTest {
                @OpenSpec(capability = "user-auth", scenario = "valid-credentials") void direct() {}
                @OpenSpec(capability = "user-auth", scenario = "valid-credentials")
                @OpenSpec(capability = "user-auth", scenario = "wrong-password") void repeated() {}
                @OpenSpecs({@OpenSpec(capability = "user-auth", scenario = "wrong-password")}) void container() {}
                void unlinked() {}
            }
            """
        )
        assertEquals(setOf("direct", "repeated"), names("user-auth", "valid-credentials"))
        assertEquals(setOf("repeated", "container"), names("user-auth", "wrong-password"))
        assertInstanceOf(linkedTests(project, "user-auth", "").single(), PsiClass::class.java)
    }

    @OpenSpec(capability = "ide-spec-navigation", scenario = "capability-all-tests")
    fun testCapabilityIncludesScenarioLevelTests() {
        addTestClass(
            """
            @OpenSpec(capability = "user-auth")
            class LoginTest {
                @OpenSpec(capability = "user-auth", scenario = "valid-credentials") void valid() {}
            }
            """
        )
        addTestClass("""class OtherTest { @OpenSpec(capability = "billing") void other() {} }""")
        assertEquals(setOf("LoginTest", "valid"), names("user-auth"))
    }
}
