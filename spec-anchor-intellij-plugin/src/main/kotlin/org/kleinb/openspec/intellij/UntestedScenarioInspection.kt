package org.kleinb.openspec.intellij

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiFile

/** Warns on slugged scenarios in capability specs that no test links to. */
class UntestedScenarioInspection : LocalInspectionTool() {

    override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        val capability = file.virtualFile?.let(::capabilityOf) ?: return null
        return scenarios(file)
            .filter { it.slug != null && linkedTests(file.project, capability, it.slug).isEmpty() }
            .map {
                manager.createProblemDescriptor(
                    it.header, "No test linked to scenario '${it.slug}'", isOnTheFly, emptyArray(), ProblemHighlightType.WARNING,
                )
            }
            .toTypedArray()
    }
}
