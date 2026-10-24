# Design

## Context

The repository is empty apart from `.gitignore` and an OpenSpec setup with no specs. See proposal.md - Why. The behavior contracts are in `specs/spec-test-annotation/spec.md` and `specs/ide-spec-navigation/spec.md`.

The machine has no JDK on PATH, and `mise` has no Gradle version set. The toolchain has to be set up before anything builds.

## Goals / Non-Goals

**Goals:**
- One Gradle build that produces the annotation jar and an installable plugin zip.
- Plugin features that rely on IntelliJ's own indexes and PSI, so they stay correct while the user edits, with no separate scan or cache to keep in sync.
- The repository uses its own annotation: its tests are annotated against the specs this change creates.

**Non-Goals:**
- Coverage reporting (a planned follow-up, see proposal).
- Kotlin, Groovy or Scala test sources. Only Java tests are resolved, even though the annotation is usable from other JVM languages.
- Publishing to Maven Central or the JetBrains Marketplace. The build produces artifacts; release automation comes later.
- Writing to `spec.md` files in any way (no slug generation; see proposal follow-ups).
- The requirement level (`### Requirement:`) as a link target.
- Rename refactoring of slugs that also updates the annotations (a later improvement).

## Decisions

### Build layout
```
settings.gradle.kts            rootProject.name = "open-spec-plugin"
gradle/libs.versions.toml      versions for JUnit, Kotlin, IntelliJ Platform Gradle Plugin
openspec-annotations/          java-library
openspec-intellij-plugin/      org.jetbrains.intellij.platform + kotlin("jvm")
```
Use the Kotlin DSL, a version catalog and a committed Gradle wrapper (Gradle 9.x). The wrapper is generated once with a Gradle installed via `mise use gradle@9`, and the JDK comes from `mise use java@21`, both recorded in `mise.toml`. Gradle toolchains then pin compilation independently of the host JDK.

### Annotation shape
```java
package org.kleinb.openspec.annotation;

@Retention(RUNTIME)
@Target({TYPE, METHOD})
@Repeatable(OpenSpecs.class)
@Documented
public @interface OpenSpec {
    String capability();
    String scenario() default "";
}
```
- Named attributes rather than `value`: two positional strings would be easy to swap by mistake. Named attributes also give the plugin a stable attribute name to attach references to.
- `RUNTIME` retention (rather than `SOURCE`/`CLASS`) so the coverage follow-up and JUnit extensions can read it without parsing source.
- `@Repeatable` with container `@OpenSpecs`. The plugin has to handle both the direct and the container form.
- Compiled with `--release 8` so any test codebase can use it. The library has no dependencies; JUnit is `testImplementation` only.
- Maven group `org.kleinb.openspec`. Java packages: `org.kleinb.openspec.annotation` (annotation module) and `org.kleinb.openspec.intellij` (plugin module). The plugin ID is `org.kleinb.openspec`: plugin IDs must not contain "intellij" (Plugin Verifier and Marketplace rule).

### Plugin platform target
- IntelliJ Platform Gradle Plugin 2.x, targeting IntelliJ IDEA 2025.2+ (`sinceBuild = 252`), JVM 21, Kotlin.
- `plugin.xml` depends on `com.intellij.java` and `org.intellij.plugins.markdown`, both bundled.
- The plugin code identifies the annotation only by its fully qualified name, so it does not depend on the annotation jar at runtime.

### Spec model (Markdown side)
`SpecIndex` (a project service) locates capability specs and scenario headings:
- Capability specs: `FilenameIndex` for `spec.md`, kept when its path matches `…/openspec/specs/<cap>/spec.md` or `…/openspec/changes/<change>/specs/<cap>/spec.md` with `<change> != archive`. The capability path is `<cap>` (may be nested). A capability is a list of files: main spec, active change deltas, and matches from several `openspec/` roots (e.g. a monorepo). Every lookup returns all of them; navigation shows a chooser when there is more than one.
- Archived changes are excluded because they are history: including them would add stale duplicate targets for every archived scenario.
- Scenarios: read from the Markdown PSI (`MarkdownHeader` at level 4 whose text starts with `Scenario:`). A trailing `<a id="([a-z0-9]+(?:-[a-z0-9]+)*)"></a>\s*$` (inline HTML tokens in the Markdown PSI) is split off into the slug; the rest, trimmed, is the name. Any other `<a` in the heading makes the heading "invalid anchor" (no slug). The model keeps the range of the anchor so references and highlighting can point at it. Slugs are compared exactly, which matches how browsers resolve `#slug` links. Results are cached with `CachedValuesManager` keyed on the PSI file, so they are recomputed when the file changes.
- Rejected: a custom `FileBasedIndex` for scenarios. The number of spec files is small, so the cached PSI walk is enough, and a custom index would add stub/versioning complexity.

### Scenario slugs
- Syntax: an HTML anchor `<a id="slug"></a>` at the end of the `#### Scenario:` heading. Alternatives considered (all accepted by `openspec validate --strict` in a scratch project):
  - `{#slug}` heading attribute: shown as literal text on GitHub, and not a link target;
  - HTML comment: hidden, but not a link target;
  - the anchor on the line after the heading: OpenSpec keeps a clean name, but the anchor can drift away from or lose its heading when the body is edited.
  The inline anchor is invisible when rendered, stays attached to its heading, and makes `spec.md#slug` a working deep link (GitHub, IDE preview).
- OpenSpec treats the anchor as part of the scenario name. As a result:
  - the slug survives `archive` (delta → main spec) and MODIFIED blocks, which copy the heading text verbatim;
  - `openspec show` output displays the anchor tag in the name. This is cosmetic: OpenSpec matches deltas by requirement name, never by scenario name.
- Resolution uses the slug only, exactly; a scenario without a slug cannot be linked (specified). Names are used only for display (chooser entries).
- Slug problems: one `localInspection` on Markdown capability specs. Invalid anchor → `ERROR` (red squiggle); duplicate slug in the file → `WARNING`, since navigation still works by offering both.

### Test model (Java side)
- Find usages through `AnnotatedElementsSearch` on the `OpenSpec` class, plus the `OpenSpecs` container. If the class does not resolve in the project, there are no usages: test code using it would not compile.
- Read attribute values with `AnnotationUtil.getStringAttributeValue`, so constant expressions work too.

### IDE features → extension points
| Spec requirement | Extension point |
|---|---|
| Navigate from scenario/capability to tests | `codeInsight.lineMarkerProvider` for `Markdown`, using `NavigationGutterIconBuilder` (gives the chooser for several targets) |
| Navigate from annotation to spec (go-to-declaration) | `psi.referenceContributor` on `PsiLiteralExpression` inside `@OpenSpec`'s `capability`/`scenario` |
| Gutter marker on annotated test | `codeInsight.lineMarkerProvider` for `JAVA` on the annotation's name identifier |
| Unknown references highlighted | `annotator` for `JAVA` that marks `@OpenSpec` references resolving to nothing (Java does not report unresolved references in string literals itself, even non-soft ones; found during implementation) |
| Invalid / duplicate slugs highlighted | one `localInspection` for `Markdown` |

Line markers are attached to leaf elements (the header's first token / the annotation's identifier), as the platform requires, to avoid duplicate-marker warnings.

### Testing
- Annotation module: only `compileJava` uses `--release 8`; tests compile and run on the JDK 21 toolchain, so current JUnit (which needs Java 17) works. JUnit reflection tests (retention, target, repeatable container). A compile-failure test for the field target uses `javax.tools.JavaCompiler` in-process.
- Plugin: `LightJavaCodeInsightFixtureTestCase` (JUnit 4 base class, run on the JUnit 5 vintage engine as the platform requires). Fixtures add `openspec/specs/**/spec.md`, a stub `OpenSpec` annotation source and annotated test classes, and then check gutter markers (`findGuttersAtCaret`), resolution and highlighting (`checkHighlighting`).
- All test methods carry `@OpenSpec` pointing at the scenarios in this change's specs. Because active changes are resolved too, they resolve before and after archive. This dogfoods the annotation and makes the plugin's own repo a manual test bed.

## Risks / Trade-offs

- [Slugs are hand-written and can collide or be malformed] → navigation offers all matches; the inspection warns on duplicates and errors on invalid anchors.
- [After archive, a scenario in a MODIFIED delta of another active change has the same slug as the main spec] → intended: both are offered, which is what a reviewer wants.
- [Changing a slug breaks annotations] → they show as unresolved references right away. Slug rename refactoring is a later improvement.
- [The anchor tag shows up in OpenSpec CLI output, and typing it by hand is verbose] → accepted; slug generation is a planned follow-up.
- [A future OpenSpec version could reject or strip inline HTML in headings] → parsing is isolated in the spec model, so switching syntax would be a local change.
- [Platform API churn across IDE versions] → pin `sinceBuild`, run `verifyPlugin` against the target IDE in the build.
- [Repeatable container handled incorrectly] → there are dedicated fixture tests for the `@OpenSpecs` form.
