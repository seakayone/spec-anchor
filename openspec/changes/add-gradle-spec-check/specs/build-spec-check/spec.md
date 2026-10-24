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

#### Scenario: Missing openspec directory <a id="missing-openspec-dir"></a>
- **WHEN** `openspecDir` points to a directory that does not exist and a test is annotated `@OpenSpec(capability = "user-auth")`
- **THEN** the task reports that annotation as an unknown reference and no spec findings

#### Scenario: Heading in a code block <a id="ignores-code-blocks"></a>
- **WHEN** a `#### Scenario:` line appears inside a fenced code block in a `spec.md`
- **THEN** it is not treated as a scenario

### Requirement: Active changes supersede living requirements
A requirement that an active change lists under `## REMOVED Requirements` or `## MODIFIED Requirements` SHALL be superseded: the scenarios under that `### Requirement:` in the living spec of the same capability SHALL get no findings and SHALL NOT resolve references. Requirement names SHALL match ignoring differences in whitespace. A `MODIFIED` requirement's scenarios in the change spec count instead. Scenarios inside a `REMOVED` section SHALL be ignored.

#### Scenario: Removed requirement <a id="removed-scenarios-not-reported"></a>
- **WHEN** the living spec of `user-auth` has `### Requirement: Legacy login` with an untested scenario `legacy`, and an active change's spec lists `### Requirement: Legacy login` under `## REMOVED Requirements`
- **THEN** no finding is reported for the scenario `legacy`

#### Scenario: Modified requirement replaces the living one <a id="modified-requirement-replaces-living"></a>
- **WHEN** the living spec has `### Requirement: Login` with an untested scenario `valid-credentials`, and an active change's spec repeats `### Requirement: Login` with that scenario under `## MODIFIED Requirements`
- **THEN** exactly one untested-scenario finding is reported for `valid-credentials`, at the change spec's line

#### Scenario: Requirement names match ignoring whitespace <a id="requirement-names-match-loosely"></a>
- **WHEN** the living spec says `### Requirement: Legacy  login` and the change removes `### Requirement: Legacy login`
- **THEN** the living requirement is superseded

#### Scenario: Other requirements stay <a id="unaffected-requirements-checked"></a>
- **WHEN** an active change removes `Legacy login` and the living spec also has `### Requirement: Login` with an untested scenario
- **THEN** that scenario is still reported as untested

### Requirement: References to removed scenarios are reported
The task SHALL report every `@OpenSpec` whose `scenario` matches no slug of a scenario that is not superseded, but matches the slug of a superseded scenario of that capability, as a removed reference instead of an unknown reference.

#### Scenario: Test linked to a removed scenario <a id="reports-removed-reference"></a>
- **WHEN** an active change removes the requirement holding scenario `legacy` and a test is annotated with `user-auth` and `scenario = "legacy"`
- **THEN** a removed-reference finding names the test and the slug, and no unknown-reference finding is reported for it

#### Scenario: Scenario dropped by a modification <a id="modified-drops-scenario"></a>
- **WHEN** the living `### Requirement: Login` has the scenarios `a` and `b`, an active change modifies it keeping only `a`, and a test links to `b`
- **THEN** a removed-reference finding is reported for the link to `b`

#### Scenario: Slug still present <a id="kept-scenario-not-removed"></a>
- **WHEN** an active change modifies `### Requirement: Login` and keeps its scenario `a`, and a test links to `a`
- **THEN** no removed-reference or unknown-reference finding is reported

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

#### Scenario: Heading with a valid anchor <a id="valid-anchor-not-missing"></a>
- **WHEN** a scenario heading ends with `<a id="valid-credentials"></a>`
- **THEN** no missing-slug finding is reported for it

### Requirement: Invalid slug anchors are reported
The task SHALL report every scenario heading whose `<a` anchor is not a valid slug anchor as defined by `ide-spec-navigation`. Such a heading SHALL NOT also be reported as missing a slug or as untested.

#### Scenario: Uppercase slug <a id="reports-invalid-slug"></a>
- **WHEN** a scenario heading ends with `<a id="Valid-Credentials"></a>`
- **THEN** exactly one finding, an invalid-slug finding, is reported for that heading

#### Scenario: Valid anchor <a id="valid-anchor-not-invalid"></a>
- **WHEN** a scenario heading ends with `<a id="valid-credentials"></a>`
- **THEN** no invalid-slug finding is reported for it

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

#### Scenario: Known references <a id="known-references-not-reported"></a>
- **WHEN** tests link to an existing capability without a scenario, to a slug in its main spec, and to a slug that exists only in an active change's spec of that capability
- **THEN** no unknown-reference finding is reported

### Requirement: Findings are reported in the build output and a file
The task SHALL print each finding on its own line with its kind, location and message, followed by a count per kind. It SHALL write the same lines to `build/reports/spec-anchor/findings.txt`. With no findings, it SHALL print that there are none.

#### Scenario: Findings printed and written <a id="prints-and-writes-findings"></a>
- **WHEN** the check finds one untested scenario and one missing slug
- **THEN** both lines and the counts appear in the build output and in `build/reports/spec-anchor/findings.txt`

#### Scenario: No findings <a id="reports-no-findings"></a>
- **WHEN** every scenario has a valid, unique slug and a linked test, and every annotation resolves
- **THEN** the task prints that there are no findings and succeeds

### Requirement: The build fails only on the finding kinds the project selects
The `specAnchor` extension SHALL offer `failOnUntestedScenario`, `failOnMissingSlug`, `failOnInvalidSlug`, `failOnDuplicateSlug`, `failOnUnknownReference` and `failOnRemovedReference`. `failOnInvalidSlug`, `failOnDuplicateSlug` and `failOnUnknownReference` SHALL default to `true`, the other three to `false`. The task SHALL fail after reporting all findings when at least one finding has a kind whose toggle is `true`, and SHALL name those kinds and their counts in the failure message.

#### Scenario: Default configuration fails on mistakes <a id="default-fails-on-mistakes"></a>
- **WHEN** no toggle is set and the check finds findings of all six kinds
- **THEN** all findings are reported and the task fails with a message naming the invalid slug, duplicate slug and unknown reference findings, and not the untested scenario, missing slug or removed reference

#### Scenario: Each kind alone with the defaults <a id="default-toggles-per-kind"></a>
- **WHEN** no toggle is set and the only finding is of one kind
- **THEN** the task fails for `invalid-slug`, `duplicate-slug` and `unknown-reference`, and succeeds for `untested-scenario`, `missing-slug` and `removed-reference`

#### Scenario: Defaults turned off <a id="defaults-can-be-turned-off"></a>
- **WHEN** the build sets `failOnInvalidSlug`, `failOnDuplicateSlug` and `failOnUnknownReference` to `false` and the check finds findings of all six kinds
- **THEN** the findings are reported and the task succeeds

#### Scenario: Enabled kind fails the build <a id="fails-on-enabled-kind"></a>
- **WHEN** for any one kind only its toggle is `true` (for example `failOnMissingSlug`) and the only finding is of that kind
- **THEN** the finding is reported and the task fails with a message naming 1 finding of that kind

#### Scenario: Other kinds do not fail <a id="ignores-disabled-kinds"></a>
- **WHEN** for any one kind every toggle except its own is `true` and the only finding is of that kind
- **THEN** the task succeeds

#### Scenario: Several enabled kinds fail together <a id="names-all-failing-kinds"></a>
- **WHEN** `failOnUntestedScenario` and `failOnUnknownReference` are `true` and the check finds two untested scenarios, one missing slug and one unknown reference
- **THEN** the task fails with a message naming 2 untested scenarios and 1 unknown reference, and not the missing slug

#### Scenario: All toggles on without findings <a id="passes-without-findings"></a>
- **WHEN** every toggle is `true` and the check finds nothing
- **THEN** the task succeeds

#### Scenario: Any combination of toggles and findings <a id="fails-only-on-enabled-found-kinds"></a>
- **WHEN** every toggle is set explicitly, any subset of them to `true`, and any subset of the six kinds is found
- **THEN** the task fails exactly when an enabled kind is found, and the message names exactly the enabled kinds that were found
