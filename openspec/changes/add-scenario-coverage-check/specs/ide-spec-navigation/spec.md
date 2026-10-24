# Spec Delta

## ADDED Requirements

### Requirement: Untested scenarios are highlighted
The plugin SHALL highlight as a warning every scenario heading in a capability spec that has a slug and no test linked to that slug in that capability. A test linked only to the whole capability SHALL NOT count as covering a scenario. Scenario headings without a slug SHALL NOT be highlighted.

#### Scenario: Untested scenario <a id="untested-scenario-flagged"></a>
- **WHEN** a capability spec contains `#### Scenario: Valid credentials <a id="valid-credentials"></a>` and no test is annotated with that capability and `scenario = "valid-credentials"`
- **THEN** the heading is highlighted as a warning saying the scenario has no linked test

#### Scenario: Tested scenario <a id="tested-scenario-not-flagged"></a>
- **WHEN** a test is annotated with the capability and `scenario = "valid-credentials"`
- **THEN** the heading is not highlighted

#### Scenario: Capability-level test only <a id="capability-test-does-not-cover"></a>
- **WHEN** the only linked test is annotated `@OpenSpec(capability = "user-auth")` without a scenario
- **THEN** every slugged scenario heading in `user-auth`'s spec is highlighted

#### Scenario: Scenario without slug <a id="unslugged-scenario-not-flagged"></a>
- **WHEN** a scenario heading has no slug anchor
- **THEN** it is not highlighted as untested

#### Scenario: Scenario in an active change <a id="untested-in-change-spec"></a>
- **WHEN** a spec in `openspec/changes/<change>/specs/` contains a slugged scenario with no linked test
- **THEN** the heading is highlighted, and nothing is highlighted in specs under `openspec/changes/archive/`

#### Scenario: Adding a test clears the warning <a id="coverage-follows-edits"></a>
- **WHEN** a scenario is highlighted as untested and the user adds a test linked to its slug
- **THEN** the highlight disappears without a rebuild or IDE restart
