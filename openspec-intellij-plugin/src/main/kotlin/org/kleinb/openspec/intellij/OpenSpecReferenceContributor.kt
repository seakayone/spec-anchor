package org.kleinb.openspec.intellij

import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInsight.daemon.EmptyResolveMessageProvider
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.patterns.PsiJavaPatterns.literalExpression
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import java.text.MessageFormat

/** Makes `capability` and `scenario` values of `@OpenSpec` resolve to the spec file and scenario headings. */
class OpenSpecReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(literalExpression().annotationParam(OPEN_SPEC, "capability"), provider(::CapabilityReference))
        registrar.registerReferenceProvider(literalExpression().annotationParam(OPEN_SPEC, "scenario"), provider(::ScenarioReference))
    }

    private fun provider(create: (PsiLiteralExpression) -> PsiReference) = object : PsiReferenceProvider() {
        override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
            val literal = element as PsiLiteralExpression
            return if ((literal.value as? String).isNullOrEmpty()) PsiReference.EMPTY_ARRAY else arrayOf(create(literal))
        }
    }
}

/** Marks `@OpenSpec` values that resolve to nothing; Java does not report unresolved references in literals itself. */
class UnresolvedSpecReferenceAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is PsiLiteralExpression) return
        element.references.filterIsInstance<SpecReference>().filter { it.multiResolve(false).isEmpty() }.forEach {
            holder.newAnnotation(HighlightSeverity.ERROR, MessageFormat.format(it.unresolvedMessagePattern, it.value))
                .range(it.absoluteRange)
                .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
                .create()
        }
    }
}

private abstract class SpecReference(literal: PsiLiteralExpression) :
    PsiPolyVariantReferenceBase<PsiLiteralExpression>(literal, false), EmptyResolveMessageProvider {

    abstract fun targets(): List<PsiElement>

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> =
        targets().map(::PsiElementResolveResult).toTypedArray()
}

private class CapabilityReference(literal: PsiLiteralExpression) : SpecReference(literal) {
    override fun targets(): List<PsiElement> = specFiles(element.project)[value].orEmpty()
    override fun getUnresolvedMessagePattern() = "Unknown OpenSpec capability ''{0}''"
}

private class ScenarioReference(literal: PsiLiteralExpression) : SpecReference(literal) {
    override fun targets(): List<PsiElement> {
        val annotation = PsiTreeUtil.getParentOfType(element, PsiAnnotation::class.java) ?: return emptyList()
        val capability = AnnotationUtil.getStringAttributeValue(annotation, "capability") ?: return emptyList()
        return specTargets(element.project, capability, value)
    }

    override fun getUnresolvedMessagePattern() = "Unknown scenario slug ''{0}'' in this OpenSpec capability"
}
