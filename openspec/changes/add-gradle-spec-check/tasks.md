# Tasks

## 1. Module setup

- [x] 1.1 Create `spec-anchor-gradle-plugin` with `java-gradle-plugin` and `maven-publish`, compiled with `--release 17` on the JDK 21 toolchain. Declare the plugin `org.kleinb.openspec` → `org.kleinb.openspec.gradle.SpecAnchorPlugin` (an empty `apply` for now). Add JUnit Jupiter, `gradleTestKit()` and `testImplementation(project(":spec-anchor-annotations"))`, and pass the annotations jar path to tests as a system property. Include the module in `settings.gradle.kts` and the CI matrix. Verify `./gradlew :spec-anchor-gradle-plugin:build` passes.

## 2. Spec parsing and checking (no Gradle API)

- [x] 2.1 Implement the spec parser in `org.kleinb.openspec.gradle.check`:
  - discovery of main and change specs, skipping archive
  - capability path from the directory
  - `#### Scenario:` lines outside fenced code blocks
  - slug, invalid anchor or missing slug, using the regexes ported from `OpenSpecModel.kt`
  - line numbers

  Add unit tests for `discovers-main-and-change-specs`, `ignores-archived-specs` (spec side), `custom-openspec-dir` (parser given another root) and `ignores-code-blocks`, each annotated `@OpenSpec(capability = "build-spec-check", scenario = …)`. Verify `./gradlew :spec-anchor-gradle-plugin:test` passes.
- [x] 2.2 Implement the `Finding` record and the checker: untested scenario (capability-level links don't count), missing slug, invalid slug (no further finding for that heading), duplicate slug per file, unknown capability and unknown scenario resolved against all files of a capability. Add annotated unit tests for:
  - `reports-untested-scenario`
  - `capability-link-does-not-cover`
  - `tested-scenario-not-reported`
  - `reports-missing-slug`
  - `reports-invalid-slug`
  - `reports-duplicate-slug`
  - `slug-in-two-files-not-duplicate`
  - `reports-unknown-capability`
  - `reports-unknown-scenario`

  Verify the tests pass.
- [x] 2.3 Implement the link reader:
  - `URLClassLoader` over classes dirs plus classpath, with the platform class loader as parent
  - `Class.forName(…, false, …)` for each class
  - reflective `getAnnotationsByType` on classes and declared methods
  - skip and warn on `LinkageError`
  - no links when `OpenSpec` is absent

  Add an annotated unit test for `reads-repeated-annotations` against fixture classes in the module's own test classes. Verify it passes.

## 3. Gradle task and plugin

- [x] 3.1 Implement `SpecAnchorExtension` (`openspecDir` defaulting to the root project's `openspec`, five `failOn…` toggles defaulting to `false`). Implement `SpecAnchorCheck` with the inputs and output from design.md and a `processIsolation` worker on the `test` task's `javaLauncher`. Implement the plugin wiring: always register `specAnchorCheck`; when `java` is applied, wire the `test` source set and launcher and make `check` depend on the task. Add TestKit tests (with `--configuration-cache`) for `runs-with-check`, `reruns-on-spec-change`, `custom-openspec-dir` and `ignores-archived-specs` (an annotation naming an archived capability is reported as unknown). Verify they pass.
- [x] 3.2 Implement reporting: one `<kind> <location>: <message>` line per finding at `warn`, counts per kind, `spec-anchor: no findings` when empty, and the same lines in `build/reports/spec-anchor/findings.txt`. Add TestKit tests for `prints-and-writes-findings` and `reports-no-findings`. Verify they pass.
- [x] 3.3 Implement the fail toggles: fail with a `GradleException` naming the enabled kinds and their counts, after all findings are reported. Add TestKit tests for `reports-only-by-default`, `fails-on-enabled-kind` and `ignores-disabled-kinds`. Verify they pass.
- [x] 3.4 Add a TestKit test with `org.jetbrains.kotlin.jvm` (version from the catalog) and an annotated Kotlin test class, for `reads-kotlin-tests` and `reads-repeated-annotations` (end to end). Verify it passes.
- [x] 3.5 Write `docs/gradle-plugin.md`, covering:
  - the `pluginManagement { repositories { mavenCentral() } }` setup and the `plugins { id("org.kleinb.openspec") version "0.3.0" }` snippet
  - the task and the finding kinds
  - the `specAnchor { }` options with their defaults
  - the report file
  - the multi-project limitation

  Update `docs/README.md` (add the build check to *Why*, a *Contents* entry, a *Requirements* row "Gradle plugin: Gradle 9 or later, Java 17 or later"), add a Gradle plugin section to `docs/installation.md`, and in `README.md` update the tagline, the *Modules* list and the *Installation* section. Verify by reading each changed page against the spec and the implemented extension options.

## 4. Publishing

- [x] 4.1 Give every `MavenPublication` of the plugin module (`pluginMaven` and the marker) the same POM metadata as `spec-anchor-annotations`, add `withSourcesJar()`/`withJavadocJar()`, and publish to `build/staging-deploy`. Verify `./gradlew :spec-anchor-gradle-plugin:publish` stages the plugin jar, the sources and javadoc jars, and `org/kleinb/openspec/org.kleinb.openspec.gradle.plugin/<version>/*.pom`.
- [ ] 4.2 Make these changes:
  - add `spec-anchor-gradle-plugin/build/staging-deploy` to `jreleaser.yml` `stagingRepositories`
  - run `:spec-anchor-gradle-plugin:publish` in `scripts/publish.sh` and print the plugin's Maven Central URL in the summary
  - extend the `release.sh` sed (and its dry-run backup/restore file list, if new files are touched) to bump `id("org.kleinb.openspec") version "x.y.z"` in `README.md` and `docs/`

  Verify `just release-dry` succeeds, stages both modules, and leaves the docs versions unchanged afterwards.
- [ ] 4.3 Update the `README.md` *Release* section so it says `just release` also publishes `spec-anchor-gradle-plugin` and its plugin marker to Maven Central. Verify by reading the section against `scripts/publish.sh`.

## 5. Integration

- [ ] 5.1 Run `./gradlew build` and verify all modules pass, including the plugin's unit and functional tests.
- [ ] 5.2 Point a scratch project outside the repo at the locally staged plugin (`pluginManagement { repositories { maven(url = "<repo>/spec-anchor-gradle-plugin/build/staging-deploy") } }`) and run `gradle check`, which checks that the marker artifact resolves the plugin. Record the result in this change.

## Workflow follow-up

- Archive the change after review.
- Plan a follow-up change for the Maven plugin, moving `org.kleinb.openspec.gradle.check` into a shared artifact.
