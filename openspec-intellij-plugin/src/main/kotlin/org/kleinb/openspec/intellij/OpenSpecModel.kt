package org.kleinb.openspec.intellij

import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil
import org.intellij.plugins.markdown.lang.psi.impl.MarkdownHeader

const val OPEN_SPEC = "org.kleinb.openspec.annotation.OpenSpec"
const val OPEN_SPECS = "org.kleinb.openspec.annotation.OpenSpecs"

private val MAIN_SPEC = Regex("/openspec/specs/(.+)/spec\\.md$")
private val CHANGE_SPEC = Regex("/openspec/changes/([^/]+)/specs/(.+)/spec\\.md$")

/** The capability path of a capability spec in the main specs or an active change, or null for any other file. */
fun capabilityOf(file: VirtualFile): String? {
    MAIN_SPEC.find(file.path)?.let { return it.groupValues[1] }
    val change = CHANGE_SPEC.find(file.path) ?: return null
    return change.groupValues[2].takeIf { change.groupValues[1] != "archive" }
}

/** All capability spec files of the project by capability path; one capability can live in several files. */
fun specFiles(project: Project): Map<String, List<PsiFile>> {
    val psiManager = PsiManager.getInstance(project)
    return FilenameIndex.getVirtualFilesByName("spec.md", GlobalSearchScope.projectScope(project))
        .sortedBy { it.path }
        .mapNotNull { file -> capabilityOf(file)?.let { capability -> psiManager.findFile(file)?.let { capability to it } } }
        .groupBy({ it.first }, { it.second })
}

/**
 * A `#### Scenario:` heading. [anchorRange] (relative to [header]) is the slug anchor when [slug] is set,
 * or the offending `<a …>` when the heading has an invalid anchor.
 */
class Scenario(val header: MarkdownHeader, val name: String, val slug: String?, val anchorRange: TextRange?)

private val SCENARIO_PREFIX = Regex("^####\\s+Scenario:")
private val SLUG_ANCHOR = Regex("""(<a id="([a-z0-9]+(?:-[a-z0-9]+)*)"></a>)\s*$""")
private val ANY_ANCHOR = Regex("""<a\b[^>]*>(?:</a>)?""", RegexOption.IGNORE_CASE)

fun scenarios(file: PsiFile): List<Scenario> = CachedValuesManager.getCachedValue(file) {
    CachedValueProvider.Result.create(
        PsiTreeUtil.findChildrenOfType(file, MarkdownHeader::class.java).mapNotNull(::parseScenario),
        file,
    )
}

private fun parseScenario(header: MarkdownHeader): Scenario? {
    val text = header.text
    val nameStart = SCENARIO_PREFIX.find(text)?.range?.last?.plus(1) ?: return null
    val anchors = ANY_ANCHOR.findAll(text).toList()
    val slug = SLUG_ANCHOR.find(text)
    if (anchors.size == 1 && slug != null) {
        val anchor = slug.groups[1]!!.range
        return Scenario(header, text.substring(nameStart, anchor.first).trim(), slug.groupValues[2], TextRange(anchor.first, anchor.last + 1))
    }
    val invalid = anchors.firstOrNull()?.range?.let { TextRange(it.first, it.last + 1) }
    return Scenario(header, text.substring(nameStart).trim(), null, invalid)
}

/** Spec targets of an annotation: the capability's files, or the headings carrying [scenario] when it is not empty. */
fun specTargets(project: Project, capability: String, scenario: String): List<PsiElement> {
    val files = specFiles(project)[capability].orEmpty()
    if (scenario.isEmpty()) return files
    return files.flatMap(::scenarios).filter { it.slug == scenario }.map { it.header }
}

/** One `@OpenSpec` on a test class or method. */
class Link(val owner: PsiModifierListOwner, val capability: String, val scenario: String)

fun links(project: Project): List<Link> = CachedValuesManager.getManager(project).getCachedValue(project) {
    CachedValueProvider.Result.create(findLinks(project), PsiModificationTracker.MODIFICATION_COUNT)
}

/** Tests linked to [capability], or only those linked to its [scenario] slug. */
fun linkedTests(project: Project, capability: String, scenario: String? = null): List<PsiModifierListOwner> =
    links(project).filter { it.capability == capability && (scenario == null || it.scenario == scenario) }.map { it.owner }.distinct()

private fun findLinks(project: Project): List<Link> {
    val facade = JavaPsiFacade.getInstance(project)
    val scope = GlobalSearchScope.projectScope(project)
    return listOf(OPEN_SPEC, OPEN_SPECS)
        .mapNotNull { facade.findClass(it, GlobalSearchScope.allScope(project)) }
        .flatMap { AnnotatedElementsSearch.searchElements(it, scope, PsiClass::class.java, PsiMethod::class.java).findAll() }
        .distinct()
        .flatMap { owner -> openSpecAnnotations(owner).mapNotNull { link(owner, it) } }
}

private fun openSpecAnnotations(owner: PsiModifierListOwner): List<PsiAnnotation> =
    owner.modifierList?.annotations.orEmpty().flatMap { annotation ->
        when {
            annotation.hasQualifiedName(OPEN_SPEC) -> listOf(annotation)
            annotation.hasQualifiedName(OPEN_SPECS) ->
                AnnotationUtil.arrayAttributeValues(annotation.findAttributeValue("value")).filterIsInstance<PsiAnnotation>()
            else -> emptyList()
        }
    }

private fun link(owner: PsiModifierListOwner, annotation: PsiAnnotation): Link? {
    val capability = AnnotationUtil.getStringAttributeValue(annotation, "capability") ?: return null
    return Link(owner, capability, AnnotationUtil.getStringAttributeValue(annotation, "scenario").orEmpty())
}
