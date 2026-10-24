package org.kleinb.openspec.intellij

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.intellij.plugins.markdown.lang.psi.impl.MarkdownHeader

/** Gutter markers on a capability spec's first heading and on its scenario headings, leading to the linked tests. */
class SpecLineMarkerProvider : RelatedItemLineMarkerProvider() {

    override fun collectNavigationMarkers(element: PsiElement, result: MutableCollection<in RelatedItemLineMarkerInfo<*>>) {
        val header = PsiTreeUtil.getParentOfType(element, MarkdownHeader::class.java) ?: return
        if (PsiTreeUtil.getDeepestFirst(header) !== element) return
        val file = header.containingFile
        val capability = file.virtualFile?.let(::capabilityOf) ?: return
        val tests = if (header === PsiTreeUtil.findChildOfType(file, MarkdownHeader::class.java)) {
            linkedTests(file.project, capability)
        } else {
            val slug = scenarios(file).find { it.header === header }?.slug ?: return
            linkedTests(file.project, capability, slug)
        }
        if (tests.isEmpty()) return
        result.add(
            NavigationGutterIconBuilder.create(AllIcons.Gutter.ImplementedMethod)
                .setTargets(tests)
                .setTooltipText(TOOLTIP)
                .createLineMarkerInfo(element)
        )
    }

    companion object {
        const val TOOLTIP = "Go to linked tests"
    }
}
