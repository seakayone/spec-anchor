package org.kleinb.openspec.intellij

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiFile

/** Errors on invalid slug anchors and warns on duplicate slugs in capability specs. */
class ScenarioSlugInspection : LocalInspectionTool() {

    override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        if (file.virtualFile?.let(::capabilityOf) == null) return null
        val scenarios = scenarios(file)
        val duplicates = scenarios.mapNotNull { it.slug }.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        return scenarios.mapNotNull { scenario ->
            val range = scenario.anchorRange ?: return@mapNotNull null
            when (scenario.slug) {
                null -> manager.createProblemDescriptor(scenario.header, range, INVALID, ProblemHighlightType.GENERIC_ERROR, isOnTheFly)
                in duplicates -> manager.createProblemDescriptor(
                    scenario.header, range, "Duplicate scenario slug '${scenario.slug}'", ProblemHighlightType.WARNING, isOnTheFly,
                )
                else -> null
            }
        }.toTypedArray()
    }

    companion object {
        const val INVALID = "Invalid scenario slug anchor: expected an id of lowercase a-z, 0-9 and single hyphens, at the end of the heading"
    }
}
