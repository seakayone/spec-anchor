# ide-spec-navigation Specification

## Purpose
Lets developers in IntelliJ IDEA jump between OpenSpec capabilities and scenarios in `spec.md` files and the Java tests annotated with `@OpenSpec`, in both directions.

## Requirements
### Requirement: Spec files are discovered in main specs and active changes
The plugin SHALL treat every `spec.md` below `openspec/specs/` and below `openspec/changes/<change>/specs/` as a capability spec, so unarchived changes can be reviewed. The capability path is the directory path between that `specs/` directory and `spec.md`. Files below `openspec/changes/archive/` SHALL be ignored.

#### Scenario: Flat capability <a id="flat-capability"></a>
- **WHEN** the project contains `openspec/specs/user-auth/spec.md`
- **THEN** the plugin knows a capability `user-auth`

#### Scenario: Nested capability <a id="nested-capability"></a>
- **WHEN** the project contains `openspec/specs/identity/user-auth/spec.md`
- **THEN** the plugin knows a capability `identity/user-auth`

#### Scenario: Capability in an active change <a id="change-spec-included"></a>
- **WHEN** the project contains `openspec/changes/add-login/specs/user-auth/spec.md` and no `openspec/specs/user-auth/spec.md`
- **THEN** the plugin knows a capability `user-auth`, resolves annotations against that file and shows navigation markers in it

#### Scenario: Archived change is ignored <a id="archived-change-ignored"></a>
- **WHEN** the project contains `openspec/changes/archive/2026-01-01-add-login/specs/user-auth/spec.md`
- **THEN** that file is not used to resolve annotations and shows no navigation markers

### Requirement: Scenario slugs are declared in the spec heading
A scenario SHALL declare its slug as an HTML anchor `<a id="slug"></a>` at the end of its `#### Scenario:` heading. A slug SHALL consist of lowercase ASCII letters, digits and single hyphens, starting and ending with a letter or digit. The scenario's name is the heading text without the anchor.

#### Scenario: Heading with slug <a id="heading-with-slug"></a>
- **WHEN** a spec contains `#### Scenario: Valid credentials <a id="valid-credentials"></a>`
- **THEN** the scenario's name is `Valid credentials` and its slug is `valid-credentials`

#### Scenario: Heading without slug <a id="heading-without-slug"></a>
- **WHEN** a scenario heading contains no `<a` anchor
- **THEN** the scenario has no slug, no test can link to it specifically, and nothing is highlighted

### Requirement: Navigate from a scenario to its tests
The plugin SHALL show a gutter marker on each `#### Scenario:` heading of a capability spec that has at least one linked test. Activating the marker SHALL navigate to the linked tests, offering a chooser when there are several.

#### Scenario: Scenario with one linked test <a id="scenario-one-test"></a>
- **WHEN** exactly one test method is annotated with the capability and this scenario's slug, and the user clicks the marker on the scenario heading
- **THEN** the editor opens that test method

#### Scenario: Scenario with several linked tests <a id="scenario-several-tests"></a>
- **WHEN** several tests are linked to the scenario and the user clicks the marker
- **THEN** a chooser lists all linked tests and selecting one opens it

#### Scenario: Scenario without tests <a id="scenario-no-tests"></a>
- **WHEN** no test is linked to a scenario
- **THEN** that scenario heading shows no marker

### Requirement: Navigate from a capability to its tests
The plugin SHALL show a gutter marker on the first heading of a capability spec when at least one test is linked to that capability. It SHALL navigate to all tests linked to the capability, whether or not they name a scenario.

#### Scenario: Capability-level and scenario-level tests <a id="capability-all-tests"></a>
- **WHEN** one class is annotated `@OpenSpec(capability = "user-auth")` and one method is annotated `@OpenSpec(capability = "user-auth", scenario = "valid-credentials")`, and the user clicks the capability marker
- **THEN** both the class and the method are offered as targets

### Requirement: Navigate from an annotation to the spec
The plugin SHALL resolve the `capability` value of an `@OpenSpec` annotation to its capability spec files and the `scenario` value to the `#### Scenario:` headings carrying that slug, so that go-to-declaration on either value opens the target. When there are several targets, a chooser SHALL list them. A gutter marker on the annotated element SHALL offer the same navigation.

#### Scenario: Go to capability <a id="goto-capability"></a>
- **WHEN** the user invokes go-to-declaration on the `capability` value `"user-auth"`
- **THEN** the editor opens `openspec/specs/user-auth/spec.md`

#### Scenario: Go to scenario <a id="goto-scenario"></a>
- **WHEN** the user invokes go-to-declaration on the `scenario` value `"valid-credentials"`
- **THEN** the editor opens the capability spec with the caret on the heading `#### Scenario: Valid credentials <a id="valid-credentials"></a>`

#### Scenario: Capability in main specs and in a change <a id="main-and-change-targets"></a>
- **WHEN** both `openspec/specs/user-auth/spec.md` and `openspec/changes/add-login/specs/user-auth/spec.md` contain the slug `valid-credentials`, and the user invokes go-to-declaration on `"valid-credentials"`
- **THEN** a chooser offers both headings

#### Scenario: Gutter marker on annotated test <a id="annotation-gutter"></a>
- **WHEN** a test method is annotated with `@OpenSpec` and the user clicks its spec marker
- **THEN** the editor opens the referenced scenario, or the capability spec if no scenario is given

### Requirement: Scenario slugs resolve within their capability
A `scenario` value SHALL resolve only to headings in the referenced capability whose slug is exactly equal to the value. Scenario names SHALL NOT be used for resolution. If several headings carry the slug, navigation SHALL offer all of them.

#### Scenario: Same slug in two capabilities <a id="slug-scoped-to-capability"></a>
- **WHEN** capabilities `a` and `b` both contain a scenario with slug `valid-input` and a test is annotated with capability `a`
- **THEN** the test links only to the scenario in `a`

#### Scenario: Slug with different case <a id="slug-case-sensitive"></a>
- **WHEN** the annotation says `scenario = "Valid-Credentials"` and the heading carries `<a id="valid-credentials"></a>`
- **THEN** the value does not resolve

#### Scenario: Name instead of slug <a id="name-does-not-resolve"></a>
- **WHEN** the annotation says `scenario = "Valid credentials"` and the heading is `#### Scenario: Valid credentials <a id="valid-credentials"></a>`
- **THEN** the value does not resolve

### Requirement: Invalid slug anchors are errors
The plugin SHALL mark as an error any `<a` in a `#### Scenario:` heading that is not exactly `<a id="slug"></a>` at the end of the heading with a valid slug.

#### Scenario: Uppercase slug <a id="invalid-slug-uppercase"></a>
- **WHEN** a scenario heading ends with `<a id="Valid-Credentials"></a>`
- **THEN** the anchor is marked as an error and the scenario has no slug

#### Scenario: Non-ASCII slug <a id="invalid-slug-non-ascii"></a>
- **WHEN** a scenario heading ends with `<a id="größe"></a>`
- **THEN** the anchor is marked as an error and the scenario has no slug

#### Scenario: Wrong anchor form <a id="invalid-slug-form"></a>
- **WHEN** a scenario heading ends with `<a name="valid-credentials"></a>` or `<a id='valid-credentials'></a>`
- **THEN** the anchor is marked as an error and the scenario has no slug

#### Scenario: Anchor not at the end <a id="invalid-slug-position"></a>
- **WHEN** a scenario heading is `#### Scenario: <a id="valid-credentials"></a> Valid credentials`
- **THEN** the anchor is marked as an error and the scenario has no slug

### Requirement: Duplicate slugs are highlighted
The plugin SHALL highlight as a warning every scenario heading in a capability spec whose slug is also used by another scenario heading in the same file.

#### Scenario: Two headings with one slug <a id="duplicate-slug"></a>
- **WHEN** one scenario heading in a spec carries `<a id="valid-input"></a>` and another carries the same anchor
- **THEN** both anchors are highlighted as duplicates

### Requirement: Unknown references are highlighted
The plugin SHALL highlight an annotation `capability` value that matches no capability spec, and a `scenario` value that matches no scenario slug in the referenced capability, as unresolved references.

#### Scenario: Unknown capability <a id="unknown-capability"></a>
- **WHEN** an annotation names a capability with no `spec.md`
- **THEN** the `capability` value is highlighted as unresolved

#### Scenario: Unknown scenario <a id="unknown-scenario"></a>
- **WHEN** an annotation names an existing capability and a slug that capability does not contain
- **THEN** the `scenario` value is highlighted as unresolved and the `capability` value is not

### Requirement: Navigation follows edits
Navigation targets and markers SHALL reflect the current content of spec files and test sources without requiring a rebuild or IDE restart.

#### Scenario: Scenario renamed in spec <a id="rename-scenario"></a>
- **WHEN** the user changes a scenario's name but keeps its slug
- **THEN** linked annotations still resolve to it and its marker still lists the same tests

#### Scenario: Slug changed in spec <a id="change-slug"></a>
- **WHEN** the user changes a scenario's slug
- **THEN** annotations using the old slug become unresolved right away

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
