# Spec Delta

## Purpose

Checks a project's OpenSpec capability specs against the tests linked to them with `@OpenSpec` at build time. It reports gaps and broken links, and can fail the build on the kinds of findings the project chooses.

## ADDED Requirements

### Requirement: Check task runs with the build's checks
Applying the Gradle plugin `org.kleinb.openspec` SHALL add a task `specAnchorCheck`. When the project also applies the `java` plugin, `check` SHALL depend on `specAnchorCheck`, and the task SHALL read the compiled classes of the `test` source set.

#### Scenario: Check runs the spec check <a id="runs-with-check"></a>
- **WHEN** a project applies `java` and `org.kleinb.openspec` and the user runs `gradle check`
- **THEN** the test classes are compiled and `specAnchorCheck` runs

#### Scenario: Check reruns after a spec edit <a id="reruns-on-spec-change"></a>
- **WHEN** `specAnchorCheck` has run, a `spec.md` is then edited, and the user runs it again
- **THEN** the task runs again instead of being reported as up to date

### Requirement: Specs are discovered like in the IDE
The task SHALL read every `spec.md` below `<openspecDir>/specs/` and below `<openspecDir>/changes/<change>/specs/`, ignoring `<openspecDir>/changes/archive/`, and derive capability paths and scenario slugs by the rules of `ide-spec-navigation`. `openspecDir` SHALL default to the `openspec` directory of the root project and SHALL be configurable.

#### Scenario: Main and change specs are read <a id="discovers-main-and-change-specs"></a>
- **WHEN** the project contains `openspec/specs/user-auth/spec.md` and `openspec/changes/add-login/specs/billing/spec.md`
- **THEN** the task checks scenarios of the capabilities `user-auth` and `billing`

#### Scenario: Archived specs are ignored <a id="ignores-archived-specs"></a>
- **WHEN** the only spec is `openspec/changes/archive/2026-01-01-add-login/specs/user-auth/spec.md`
- **THEN** the task reports no findings for it, and an annotation naming `user-auth` is reported as unknown

#### Scenario: Custom openspec directory <a id="custom-openspec-dir"></a>
- **WHEN** the build sets `specAnchor { openspecDir = file("docs/openspec") }`
- **THEN** specs are read from `docs/openspec/specs/` and `docs/openspec/changes/`

#### Scenario: Heading in a code block <a id="ignores-code-blocks"></a>
- **WHEN** a `#### Scenario:` line appears inside a fenced code block in a `spec.md`
- **THEN** it is not treated as a scenario

### Requirement: Links are read from compiled test classes
The task SHALL read every `@OpenSpec` on test classes and test methods, including repeated annotations, from the compiled test classes, whether the tests are written in Java or Kotlin.

#### Scenario: Repeated annotations on a method <a id="reads-repeated-annotations"></a>
- **WHEN** a test method carries two `@OpenSpec` annotations with the scenarios `a` and `b` of one capability
- **THEN** both scenarios count as tested

#### Scenario: Kotlin test <a id="reads-kotlin-tests"></a>
- **WHEN** a Kotlin test method is annotated `@OpenSpec(capability = "user-auth", scenario = "valid-credentials")`
- **THEN** that scenario counts as tested

### Requirement: Untested scenarios are reported
The task SHALL report every scenario that has a valid slug and no test linked to that slug in its capability. A test linked only to the whole capability SHALL NOT count as covering a scenario.

#### Scenario: Scenario without test <a id="reports-untested-scenario"></a>
- **WHEN** `user-auth` has a scenario with slug `valid-credentials` and no test links to it
- **THEN** an untested-scenario finding names the spec file, the heading's line and the slug

#### Scenario: Capability-level test only <a id="capability-link-does-not-cover"></a>
- **WHEN** the only linked test is annotated `@OpenSpec(capability = "user-auth")`
- **THEN** every slugged scenario of `user-auth` is reported as untested

#### Scenario: Tested scenario <a id="tested-scenario-not-reported"></a>
- **WHEN** a test is annotated with `user-auth` and `scenario = "valid-credentials"`
- **THEN** that scenario is not reported as untested

### Requirement: Scenarios without slug are reported
The task SHALL report every scenario heading that contains no `<a` anchor as missing a slug.

#### Scenario: Heading without anchor <a id="reports-missing-slug"></a>
- **WHEN** a spec contains `#### Scenario: Valid credentials` with no anchor
- **THEN** a missing-slug finding names the spec file and the heading's line, and the scenario is not reported as untested

### Requirement: Invalid slug anchors are reported
The task SHALL report every scenario heading whose `<a` anchor is not a valid slug anchor as defined by `ide-spec-navigation`. Such a heading SHALL NOT also be reported as missing a slug or as untested.

#### Scenario: Uppercase slug <a id="reports-invalid-slug"></a>
- **WHEN** a scenario heading ends with `<a id="Valid-Credentials"></a>`
- **THEN** exactly one finding, an invalid-slug finding, is reported for that heading

### Requirement: Duplicate slugs are reported
The task SHALL report every scenario heading whose slug is also used by another scenario heading in the same `spec.md`.

#### Scenario: Slug used twice in one file <a id="reports-duplicate-slug"></a>
- **WHEN** two scenario headings in one spec carry `<a id="valid-input"></a>`
- **THEN** a duplicate-slug finding is reported for each of the two headings

#### Scenario: Same slug in two files <a id="slug-in-two-files-not-duplicate"></a>
- **WHEN** `openspec/specs/user-auth/spec.md` and `openspec/changes/add-login/specs/user-auth/spec.md` each contain the slug `valid-input` once
- **THEN** no duplicate-slug finding is reported

### Requirement: Unknown references are reported
The task SHALL report every `@OpenSpec` whose `capability` matches no discovered capability, and every one whose `scenario` is not empty and matches no slug of that capability.

#### Scenario: Unknown capability <a id="reports-unknown-capability"></a>
- **WHEN** a test is annotated `@OpenSpec(capability = "no-such-capability")`
- **THEN** an unknown-reference finding names the test class or method and the capability

#### Scenario: Unknown scenario <a id="reports-unknown-scenario"></a>
- **WHEN** a test is annotated with an existing capability and `scenario = "no-such-slug"`
- **THEN** an unknown-reference finding names the test and the slug

### Requirement: Findings are reported in the build output and a file
The task SHALL print each finding on its own line with its kind, location and message, followed by a count per kind. It SHALL write the same lines to `build/reports/spec-anchor/findings.txt`. With no findings, it SHALL print that there are none.

#### Scenario: Findings printed and written <a id="prints-and-writes-findings"></a>
- **WHEN** the check finds one untested scenario and one missing slug
- **THEN** both lines and the counts appear in the build output and in `build/reports/spec-anchor/findings.txt`

#### Scenario: No findings <a id="reports-no-findings"></a>
- **WHEN** every scenario has a valid, unique slug and a linked test, and every annotation resolves
- **THEN** the task prints that there are no findings and succeeds

### Requirement: The build fails only on the finding kinds the project selects
The `specAnchor` extension SHALL offer `failOnUntestedScenario`, `failOnMissingSlug`, `failOnInvalidSlug`, `failOnDuplicateSlug` and `failOnUnknownReference`, all `false` by default. The task SHALL fail after reporting all findings when at least one finding has a kind whose toggle is `true`, and SHALL name those kinds and their counts in the failure message.

#### Scenario: Default configuration only reports <a id="reports-only-by-default"></a>
- **WHEN** no toggle is set and the check finds untested scenarios
- **THEN** the findings are reported and the task succeeds

#### Scenario: Enabled kind fails the build <a id="fails-on-enabled-kind"></a>
- **WHEN** `failOnUntestedScenario = true` and the check finds two untested scenarios
- **THEN** all findings are reported and the task fails with a message naming 2 untested scenarios

#### Scenario: Other kinds do not fail <a id="ignores-disabled-kinds"></a>
- **WHEN** `failOnUntestedScenario = true` and the only findings are missing slugs
- **THEN** the task succeeds
