package org.kleinb.openspec.intellij

import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.util.PsiTreeUtil

/** Gutter marker on each `@OpenSpec` name, leading to the referenced scenario or capability spec. */
class TestLineMarkerProvider : RelatedItemLineMarkerProvider() {

    override fun collectNavigationMarkers(element: PsiElement, result: MutableCollection<in RelatedItemLineMarkerInfo<*>>) {
        if (element !is PsiIdentifier) return
        val annotation = PsiTreeUtil.getParentOfType(element, PsiAnnotation::class.java) ?: return
        if (annotation.nameReferenceElement?.referenceNameElement !== element || !annotation.hasQualifiedName(OPEN_SPEC)) return
        val capability = AnnotationUtil.getStringAttributeValue(annotation, "capability") ?: return
        val scenario = AnnotationUtil.getStringAttributeValue(annotation, "scenario").orEmpty()
        val targets = specTargets(element.project, capability, scenario)
        if (targets.isEmpty()) return
        result.add(
            NavigationGutterIconBuilder.create(AllIcons.Gutter.ImplementingMethod)
                .setTargets(targets)
                .setTooltipText(TOOLTIP)
                .createLineMarkerInfo(element)
        )
    }

    companion object {
        const val TOOLTIP = "Go to OpenSpec spec"
    }
}
