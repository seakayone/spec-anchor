# spec-test-annotation Specification

## Purpose
Lets Java tests declare which OpenSpec capability and scenario they verify, so specs and tests can be traced to each other by tools.

## Requirements
### Requirement: Annotation identifies a capability and an optional scenario slug
The library SHALL provide an `@OpenSpec` annotation with a required `capability` attribute and an optional `scenario` attribute that defaults to the empty string. `capability` is the spec directory path relative to `openspec/specs/`. `scenario` is the scenario's slug, not its name; how slugs are declared and matched is defined by `ide-spec-navigation`.

#### Scenario: Link to a specific scenario <a id="link-scenario"></a>
- **WHEN** a test is annotated with `@OpenSpec(capability = "user-auth", scenario = "valid-credentials")`
- **THEN** the test is linked to the scenario whose heading carries `<a id="valid-credentials"></a>` in `openspec/specs/user-auth/spec.md`

#### Scenario: Link to a whole capability <a id="link-capability"></a>
- **WHEN** a test is annotated with `@OpenSpec(capability = "user-auth")`
- **THEN** the test is linked to the capability `user-auth` and to none of its scenarios specifically

#### Scenario: Nested capability path <a id="nested-capability"></a>
- **WHEN** a test is annotated with `@OpenSpec(capability = "identity/user-auth", scenario = "valid-credentials")`
- **THEN** the test is linked to the scenario in `openspec/specs/identity/user-auth/spec.md`

### Requirement: Annotation applies to test classes and methods
The annotation SHALL be allowed on classes and methods, and on no other element kinds.

#### Scenario: Annotate a class <a id="annotate-class"></a>
- **WHEN** a test class is annotated with `@OpenSpec`
- **THEN** the code compiles and the class is linked

#### Scenario: Annotate a method <a id="annotate-method"></a>
- **WHEN** a test method is annotated with `@OpenSpec`
- **THEN** the code compiles and the method is linked

#### Scenario: Annotate a field <a id="annotate-field"></a>
- **WHEN** a field is annotated with `@OpenSpec`
- **THEN** compilation fails

### Requirement: Annotation is repeatable
The annotation SHALL be repeatable on the same element, so one test can be linked to several scenarios or capabilities.

#### Scenario: Test covers two scenarios <a id="repeatable"></a>
- **WHEN** a test method carries two `@OpenSpec` annotations with different scenarios
- **THEN** the code compiles and the method is linked to both scenarios

### Requirement: Annotation is visible at runtime
The annotation SHALL be retained at runtime so that test frameworks and reporting tools can read it by reflection.

#### Scenario: Read annotation by reflection <a id="runtime-reflection"></a>
- **WHEN** a tool reads the annotations of an annotated test method by reflection
- **THEN** it obtains every `@OpenSpec` on that method with its `capability` and `scenario` values

### Requirement: Library has no runtime dependencies
The annotation library SHALL have no transitive dependencies, so adding it to a test classpath brings in nothing else.

#### Scenario: Add library to a project <a id="no-dependencies"></a>
- **WHEN** a project adds the annotation library as a test dependency
- **THEN** no other artifacts are added to its test classpath
