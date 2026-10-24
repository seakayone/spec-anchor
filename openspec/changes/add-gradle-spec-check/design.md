# Design

## Context

See proposal.md (Why) for the motivation. The behavior is specified in `specs/build-spec-check/spec.md`, and the slug and discovery rules live in `openspec/specs/ide-spec-navigation/spec.md`.

The IntelliJ plugin already implements those rules in `OpenSpecModel.kt`: the `capabilityOf` path regexes and the `SLUG_ANCHOR`/`ANY_ANCHOR` heading regexes. That code is PSI-based Kotlin and can't run in Gradle.

The build uses Gradle 9.6 and JDK 21. The annotation has runtime retention and lives in `spec-anchor-annotations` (Java 8). Releases go through `scripts/release.sh`, then `scripts/publish.sh`, then JReleaser, which deploys the Gradle-staged `build/staging-deploy` directories to Maven Central.

## Goals / Non-Goals

**Goals:**
- One Gradle plugin, compatible with the configuration cache, with no runtime dependencies.
- Keep the check logic free of Gradle API, so the later Maven plugin can reuse it after a package move.

**Non-Goals:**
- Maven plugin, Gradle Plugin Portal, aggregating tests across subprojects, source sets other than `test`.
- Machine-readable reports (JSON, SARIF) or HTML.
- Sharing parser code with the IntelliJ plugin. The PSI and text-based parsers stay separate, and the shared spec scenarios keep them in line.
- Applying the plugin to this repository's own build.

## Decisions

### Module, language and target
New module `spec-anchor-gradle-plugin`, written in Java with `java-gradle-plugin`, compiled with `--release 17`. Plugin ID `org.kleinb.openspec`, implementation class `org.kleinb.openspec.gradle.SpecAnchorPlugin`. It requires Gradle 9.0 or later.
- Java instead of Kotlin: a Kotlin plugin compiled with Kotlin 2.4 would run against Gradle's embedded, older kotlin-stdlib and need careful `apiVersion` pinning. Java has no such coupling and matches `spec-anchor-annotations`.
- The plugin ID can match the IntelliJ plugin ID because the two live in separate namespaces. The marker artifact becomes `org.kleinb.openspec:org.kleinb.openspec.gradle.plugin`.

### Package split
- `org.kleinb.openspec.gradle.check` contains plain Java with no Gradle imports. It holds the spec parser (capability path, scenarios with name, line, slug or invalid anchor), the link reader (classes dir plus classpath → links), the checker (specs plus links → findings) and the `Finding` record (kind, location, message).
- `org.kleinb.openspec.gradle` contains the plugin, the extension, the task and the worker action.
- The later Maven plugin can move `check` into its own artifact without changing behavior.

### Spec parsing: line-based, same regexes as the IDE
- The parser walks `<openspecDir>/specs/**/spec.md` and `<openspecDir>/changes/*/specs/**/spec.md`, skipping `changes/archive`. It derives the capability path from the directory between `specs/` and `spec.md`.
- It treats a line as a scenario heading when it matches `^ {0,3}####\s+Scenario:`, and skips lines inside fenced code blocks (``` or ~~~).
- The slug and invalid-anchor logic ports `parseScenario` from `OpenSpecModel.kt` regex for regex: exactly one `<a …>` that matches `SLUG_ANCHOR` gives a slug, any other `<a` makes the anchor invalid, and no `<a` at all means the slug is missing.
- Rejected alternative: a Markdown library. It would add a dependency for one heading level.

### Link reading: reflection in a forked worker on the test JVM
- The task submits a Worker API action with `processIsolation`. The fork uses the executable of the `test` task's `javaLauncher` and gets the plugin's own classes on its classpath.
- The worker builds a `URLClassLoader` over the test classes dirs plus `testRuntimeClasspath`, with the platform class loader as parent.
- For each `.class` file it calls `Class.forName(name, false, loader)`, which loads the class without initializing it. It then reads `getAnnotationsByType(openSpec)` on the class and on each declared method. `openSpec` is the `OpenSpec` class as loaded by that class loader.
- Annotation attributes are read reflectively (`capability()`, `scenario()`), so the plugin has no compile dependency on the annotations.
- `getAnnotationsByType` resolves `@OpenSpecs` containers, which covers repeated annotations. Kotlin classes need no special handling.
- A class that throws `LinkageError` is skipped and logged at `warn`. If `OpenSpec` is missing from the classpath, the worker finds no links.
- The test JVM is used because classes compiled for Java 21+ can't load in a daemon running Java 17. That JVM is guaranteed to read them.
- Rejected: ASM. It adds a dependency, and each ASM release caps the class-file version it reads.
- Rejected: parsing sources. It would need separate Java and Kotlin parsers.

### Task, extension and wiring
- `SpecAnchorExtension` is named `specAnchor`. It has a `DirectoryProperty openspecDir`, which defaults via `project.getIsolated().getRootProject().getProjectDirectory().dir("openspec")` (safe for isolated projects), and five `Property<Boolean>` toggles. `failOnInvalidSlug`, `failOnDuplicateSlug` and `failOnUnknownReference` default to `true`, the other two to `false`.
- `SpecAnchorCheck` is an abstract task. Its inputs are:
  - spec files: `@InputFiles`, a file tree of `openspecDir` filtered to `**/spec.md`, `PathSensitivity.RELATIVE`
  - test classes dirs and test runtime classpath: `@Classpath`
  - the toggles: `@Input`
  - the launcher: `@Nested`

  Its output is the report file `@OutputFile build/reports/spec-anchor/findings.txt`. Each run that executes prints its findings, and failed runs are never up to date, so a failing check always reruns.
- Plugin wiring:
  - The plugin always registers the task.
  - On `pluginManager.withPlugin("java")` it wires the `test` source set output and runtime classpath and the `test` task's launcher, and makes `check` depend on the task.
  - Without `java` the task has no test classes, so every slugged scenario is reported as untested.
- Output:
  - Each finding is a line `<kind> <location>: <message>`, logged at `warn` and written to the report file.
  - Kinds are `untested-scenario`, `missing-slug`, `invalid-slug`, `duplicate-slug` and `unknown-reference`.
  - Spec locations are paths relative to the root project directory plus `:line`. Test locations are the class name or `Class#method`.
  - A summary line gives the count per kind, or `spec-anchor: no findings`.
  - If any finding's kind is enabled, the task throws a `GradleException` with a message like `spec-anchor: 2 untested-scenario, 1 unknown-reference`.

### Delta sections: REMOVED and MODIFIED supersede living requirements
- The parser also records each scenario's `### Requirement:` name (whitespace collapsed, as OpenSpec matches names) and, per file, the requirement names listed under `## REMOVED Requirements` and `## MODIFIED Requirements`. Scenarios inside a `REMOVED` section are dropped.
- For each capability, the checker takes the union of removed and modified names over all change specs. A living-spec scenario under one of those requirements is superseded: it gets no findings and does not resolve links. The change spec's own scenarios (`ADDED`, `MODIFIED`) count normally.
- Slugs that only superseded scenarios carry are the capability's removed slugs. A link to one is a `removed-reference` instead of an `unknown-reference`, with `failOnRemovedReference` off by default. Before a change is applied, the linked test is still legitimate, so proposing a removal must not break the build. Projects can turn it on to force cleanup before archive.
- `RENAMED` only renames requirements, and slugs don't change, so it is ignored. The IntelliJ plugin keeps reading all scenarios. Navigation should still reach a scenario that a change is about to remove.
- Rejected: treating removed scenarios as gone. Links to them would become `unknown-reference`, which fails by default, as soon as a removal is proposed.

### Resolution rules
- A link resolves against the union of all discovered files of its capability, both main and change specs, which matches `specTargets` in the IDE.
- A scenario counts as tested when any link has the same capability and its slug.
- Duplicates are counted per file.
- Each heading gets at most one of `missing-slug`/`invalid-slug`, and only headings with a valid slug can be `untested-scenario`.

### Publishing to Maven Central
- `java-gradle-plugin` with `maven-publish` already creates the `pluginMaven` publication and the marker publication. A `withType<MavenPublication>` block gives both the same POM metadata as `spec-anchor-annotations`.
- `withSourcesJar()`/`withJavadocJar()` provide the jars Central requires. Both publications go to `build/staging-deploy`.
- `jreleaser.yml` adds `spec-anchor-gradle-plugin/build/staging-deploy` to `stagingRepositories`.
- `publish.sh` runs `:spec-anchor-gradle-plugin:publish` and prints the plugin's Central URL.
- `release.sh` updates the version in `id("org.kleinb.openspec") version "x.y.z"` snippets in `README.md` and `docs/`.

### Testing
- Unit tests (JUnit Jupiter) cover the parser and checker on in-memory or `@TempDir` specs and hand-built links. They cover the discovery, finding and resolution scenarios.
- TestKit functional tests use `withPluginClasspath()` and `--configuration-cache`. They cover task wiring, rerun after a spec edit, report output, the fail toggles, repeated annotations and reading compiled test classes.
  - The generated test projects use plain annotated classes in `src/test/java` (no JUnit needed) and depend on the annotations jar via `files(...)`. Its path is passed from the build as a system property, and the build depends on `:spec-anchor-annotations:jar`.
- One functional test applies `org.jetbrains.kotlin.jvm` (version from the catalog) to cover the `reads-kotlin-tests` scenario.
- Every test is annotated `@OpenSpec(capability = "build-spec-check", scenario = "<slug>")`. The plugin module therefore has `testImplementation(project(":spec-anchor-annotations"))`.

## Risks / Trade-offs

- [In multi-project builds the task only sees its own project's tests, so scenarios tested in other subprojects show as untested] → Documented. Apply the plugin where the tests live, or leave `failOnUntestedScenario` off. Aggregation is a follow-up.
- [Forking a worker JVM adds roughly a second per run] → The task is up to date when specs and classes haven't changed.
- [The IDE and Gradle parsers could drift] → Same regexes, same spec scenarios, and the build spec refers to `ide-spec-navigation` for the slug rules.
- [Central may reject the POM-only marker artifact] → Rehearse with `just release-dry` (JReleaser `--dry-run`) before the first real release. Packaging `pom` is valid on Central.
- [The Kotlin functional test downloads the Kotlin Gradle plugin and is slow] → Only one such test. CI already caches Gradle.
- [Users must add `mavenCentral()` to `pluginManagement.repositories`] → Shown first on the docs page.
