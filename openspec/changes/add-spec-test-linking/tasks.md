# Tasks

## 1. Toolchain and build skeleton

- [ ] 1.1 Add `mise.toml` pinning `java = "21"` and `gradle = "9"`, run `mise install`, and verify `java -version` and `gradle --version` work in the repo
- [ ] 1.2 Create `settings.gradle.kts` (root name `open-spec-plugin`, includes `openspec-annotations` and `openspec-intellij-plugin`), root `build.gradle.kts` and `gradle/libs.versions.toml`; generate the wrapper with `gradle wrapper`, then verify `./gradlew projects` lists both subprojects
- [ ] 1.3 Add `README.md` describing the two modules and how to build them (`./gradlew build`), and verify the documented command runs

## 2. Annotation library (`openspec-annotations`)

- [ ] 2.1 Configure `openspec-annotations/build.gradle.kts` as `java-library` with `release = 8` on the `compileJava` task only (tests use the JDK 21 toolchain), no `api`/`implementation` dependencies, and JUnit (Jupiter) for tests; verify `./gradlew :openspec-annotations:dependencies --configuration runtimeClasspath` shows no dependencies and `javap -v` on `OpenSpec.class` reports major version 52
- [ ] 2.2 Implement `org.kleinb.openspec.annotation.OpenSpec` (`capability`, `scenario` default `""`, `@Target({TYPE, METHOD})`, `@Retention(RUNTIME)`, `@Repeatable(OpenSpecs.class)`, `@Documented`) and `OpenSpecs` container, with Javadoc describing the capability path and that `scenario` is the `<a id="slug"></a>` from the scenario heading; verify `./gradlew :openspec-annotations:javadoc` succeeds
- [ ] 2.3 Add reflection tests covering the specific-scenario, whole-capability, nested-path, class, method, repeatable and runtime-reflection scenarios, each test annotated with `@OpenSpec(capability = "spec-test-annotation", scenario = ...)`; verify `./gradlew :openspec-annotations:test` passes
- [ ] 2.4 Add an in-process `javax.tools.JavaCompiler` test showing that `@OpenSpec` on a field fails to compile; verify it passes in `:openspec-annotations:test`

## 3. Plugin scaffolding (`openspec-intellij-plugin`)

- [ ] 3.1 Configure `openspec-intellij-plugin/build.gradle.kts` with the IntelliJ Platform Gradle Plugin 2.x, Kotlin JVM 21, target IntelliJ IDEA 2025.2 (`sinceBuild = 252`), bundled plugins `com.intellij.java` and `org.intellij.plugins.markdown`, and the platform test framework plus the JUnit vintage engine; verify `./gradlew :openspec-intellij-plugin:buildPlugin` produces a zip
- [ ] 3.2 Add `plugin.xml` (ID `org.kleinb.openspec.intellij`, all plugin code under package `org.kleinb.openspec.intellij`, name, vendor, depends on Java and Markdown) and verify `./gradlew :openspec-intellij-plugin:verifyPluginProjectConfiguration` reports no problems
- [ ] 3.3 Add a test base class that sets up a light Java fixture with a stub `OpenSpec`/`OpenSpecs` annotation source and helpers to add `openspec/specs/<path>/spec.md` files; verify a smoke test using it passes

## 4. Spec model

- [ ] 4.1 Implement the `SpecIndex` project service: discover capability specs under `openspec/specs/` and `openspec/changes/<change>/specs/` (flat, nested, several roots), exclude `openspec/changes/archive/`, and map capability path → list of files; verify with fixture tests for the flat-capability, nested-capability, change-spec-included and archived-change-ignored scenarios
- [ ] 4.2 Extract `#### Scenario:` headers per spec file from the Markdown PSI into (name, slug, slug range), splitting off a trailing `<a id="slug"></a>` (lowercase ASCII) and flagging any other `<a` as invalid, cached through `CachedValuesManager`; verify with tests for heading-with-slug, heading-without-slug, and each invalid form (uppercase, non-ASCII, `name=`, single quotes, not at end)
- [ ] 4.3 Implement one `localInspection` for capability specs: `ERROR` on invalid anchors, `WARNING` on slugs used twice in the file; verify with `checkHighlighting` tests for the invalid-slug-uppercase, invalid-slug-non-ascii, invalid-slug-form, invalid-slug-position and duplicate-slug scenarios, and that headings without an anchor are not flagged

## 5. Test model

- [ ] 5.1 Implement lookup of `@OpenSpec` usages (direct and `@OpenSpecs` container, on classes and methods), returning (element, capability, scenario); verify with fixture tests for each form
- [ ] 5.2 Add queries "tests for capability" (all usages for the capability, with or without scenario) and "tests for scenario"; verify with tests covering the capability-level-and-scenario-level-tests scenario

## 6. Navigation from spec to tests

- [ ] 6.1 Implement the Markdown line marker provider: marker on scenario headings with linked tests (navigates directly for one target, chooser for several, no marker without tests) and on the first heading of a capability spec; verify with `findGuttersAtCaret` tests for the one-test, several-tests and no-tests scenarios and the capability marker
- [ ] 6.2 Verify with fixture tests that markers appear in an active change's `spec.md` and not in `openspec/changes/archive/**/spec.md`

## 7. Navigation from annotation to spec

- [ ] 7.1 Implement the reference contributor for `capability` and `scenario` literals in `@OpenSpec` (including inside `@OpenSpecs`) that resolves to the spec file and to the scenario header(s) carrying the slug; matching slugs exactly; verify with tests for goto-capability, goto-scenario, main-and-change-targets (chooser with both), slug-case-sensitive, slug-scoped-to-capability and name-does-not-resolve
- [ ] 7.2 Make the references non-soft so unresolved values are highlighted; verify with `checkHighlighting` tests for the unknown-capability and unknown-scenario scenarios (only the scenario flagged when the capability exists)
- [ ] 7.3 Implement the Java line marker provider on annotated classes/methods that navigates to the scenario or capability spec; verify with a gutter-marker test
- [ ] 7.4 Add tests for navigation following edits: renaming a scenario but keeping its slug keeps the annotation resolved and the marker unchanged; changing the slug makes the annotation unresolved

## 8. Integration and dogfooding

- [ ] 8.1 Annotate every plugin test method with `@OpenSpec(capability = "ide-spec-navigation", scenario = "<slug>")`, using the slugs already in this change's specs, (add `openspec-annotations` as a `testImplementation` project dependency) and verify `./gradlew build` passes
- [ ] 8.2 Run `./gradlew :openspec-intellij-plugin:verifyPlugin` against the target IDE and verify there are no compatibility errors
- [ ] 8.3 Run `./gradlew :openspec-intellij-plugin:runIde`, open this repository, and manually check navigation in both directions on this change's spec files; record the result in the change

## Workflow follow-up

- Archive the change so the specs move to `openspec/specs/`.
- Propose the follow-ups: coverage check, annotation completion, slug generation.
