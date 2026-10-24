# Proposal

## Why

Today, spec–test gaps are visible only in IntelliJ: untested scenarios, missing or invalid slugs and broken `@OpenSpec` references show up as inspections, but nothing stops them from being merged. A Gradle plugin brings the same checks to the command line and CI, where the build can report them and, if the project chooses, fail on them.

## What Changes

- New Gradle plugin `org.kleinb.openspec` in a new module `spec-anchor-gradle-plugin`. It adds a `specAnchorCheck` task that runs as part of `check`.
- The task reads the project's capability specs (`openspec/specs/` and active changes, not `openspec/changes/archive/`) and the `@OpenSpec` annotations on the compiled test classes, and reports:
  - slugged scenarios without a linked test
  - scenario headings without a slug
  - invalid slug anchors
  - duplicate slugs within one `spec.md`
  - `@OpenSpec` references to an unknown capability or scenario slug
  - `@OpenSpec` references to a scenario that an active change removes
- Requirements that an active change lists as `REMOVED` or `MODIFIED` supersede the living version, so findings describe the specs as they will be after the change.
- Findings are printed to the build output and written to a report file.
- A `specAnchor { }` extension has one `failOn…` toggle per finding kind. `failOnInvalidSlug`, `failOnDuplicateSlug` and `failOnUnknownReference` are on by default, since those findings are always mistakes. The two that are normal while a change is being written, untested scenarios and missing slugs, are off.
- The plugin and its plugin marker artifact are published to Maven Central with the annotation library, in the same release.
- Docs gain a Gradle plugin page and installation instructions.
- Out of scope: a Maven plugin (planned for a later change), and aggregating tests across several Gradle subprojects.

## Capabilities

### New Capabilities
- `build-spec-check`: build-time checking of OpenSpec specs against linked tests, covering findings, reporting and configurable build failure. A later Maven plugin will implement the same capability.

### Modified Capabilities
None. The slug and discovery rules stay defined in `ide-spec-navigation`, and the new spec refers to them.

## Impact

- New module `spec-anchor-gradle-plugin` (Java, `java-gradle-plugin`), included in `settings.gradle.kts` and the CI matrix.
- Release: `jreleaser.yml` gets a second staging repository, `scripts/publish.sh` publishes the new module, and `scripts/release.sh` keeps the plugin version in the docs in sync.
- Docs: new `docs/gradle-plugin.md`. `docs/README.md` (intro, *Contents*, *Requirements* table) and `docs/installation.md` (new Gradle plugin section). `README.md` (tagline, *Modules*, *Installation*, *Release*).
- Users who apply the plugin must add `mavenCentral()` to `pluginManagement.repositories`.
