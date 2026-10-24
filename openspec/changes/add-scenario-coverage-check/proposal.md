# Proposal

## Why

Navigation shows which tests cover a scenario, but a scenario without tests is silent: its heading simply has no gutter icon. During the manual check of the first change, untested scenarios were easy to miss. Spec authors and reviewers need to see the gaps directly in `spec.md`.

## What Changes

- New IDE inspection on capability specs (main specs and active changes) that highlights, as a warning, every scenario heading that has a slug but no test linked to that slug.
- A test linked only to the whole capability does not count as covering its scenarios.
- Scenarios without a slug are not flagged, since no test can link to them yet.
- The warning updates as tests and specs are edited, like navigation.
- Separate from the existing slug inspection, so it can be turned off or re-levelled on its own (e.g. while authoring a change before tests exist).

Out of scope (kept in mind):
- a Gradle/CI task reporting untested scenarios;
- flagging scenarios without a slug;
- Kotlin test sources (see Impact).

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `ide-spec-navigation`: adds the requirement that untested scenarios are highlighted in capability specs.

## Impact

- `openspec-intellij-plugin`: one new inspection class, its registration in `plugin.xml`, an inspection description, and tests. Reuses the existing spec model (`scenarios`, `capabilityOf`) and test model (`linkedTests`).
- Only Java tests are found today. In this repository the plugin's own tests are Kotlin, so all `ide-spec-navigation` scenarios will show the warning until Kotlin test support lands.
