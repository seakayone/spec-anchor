# Tasks

## 1. Untested-scenario inspection

- [x] 1.1 Implement `UntestedScenarioInspection` (`checkFile` on capability specs, warning on the header of each slugged scenario with no `linkedTests` for its slug) and register it in `plugin.xml` as `OpenSpecUntestedScenario` (Markdown, `WARNING`, enabled, group `OpenSpec`) with `inspectionDescriptions/OpenSpecUntestedScenario.html`; verify `./gradlew :openspec-intellij-plugin:verifyPluginProjectConfiguration` reports no problems
- [x] 1.2 Add `checkHighlighting` tests for untested-scenario-flagged, tested-scenario-not-flagged, capability-test-does-not-cover, unslugged-scenario-not-flagged and untested-in-change-spec (including no warning in an archived change), each annotated with its slug; verify `./gradlew :openspec-intellij-plugin:test` passes
- [x] 1.3 Add a test for coverage-follows-edits: warning present, add a linked Java test class, warning gone; verify it passes
- [x] 1.4 Mention the coverage warning in `README.md` (what it flags, that it can be disabled under Settings → Inspections → OpenSpec) and verify `./gradlew build` passes

## 2. Integration

- [x] 2.1 Run `./gradlew :openspec-intellij-plugin:verifyPlugin` and verify the result is Compatible
- [x] 2.2 Run `./gradlew :openspec-intellij-plugin:runIde`, open this repository, and manually check that untested scenarios in `openspec/specs/spec-test-annotation/spec.md` are highlighted (e.g. `no-dependencies`) and tested ones are not; record the result in the change
  - Result (2026-10-24, sandbox IDE): untested scenarios are highlighted and tested ones are not, as expected; checked by the user.

## Workflow follow-up

- Archive the change after review.
