# spec-anchor documentation

spec-anchor links Java tests to the scenarios in your [OpenSpec](https://github.com/Fission-AI/OpenSpec) specs and lets you jump between the two in IntelliJ IDEA.

## Why

OpenSpec keeps the behaviour of a system as capability specs (`openspec/specs/<capability>/spec.md`), each a list of requirements with `#### Scenario:` headings. The tests that verify those scenarios live somewhere else, and the link between a scenario and its tests usually exists only in someone's head. spec-anchor makes that link explicit and checkable:

- **In the code**, a test says which scenario it verifies with the `@OpenSpec` annotation.
- **In the IDE**, the plugin turns that annotation into navigation in both directions, flags references that point nowhere, and warns about scenarios that no test covers.

The annotation has no effect at runtime; it is plain metadata, retained so that other tools (test reports, coverage scripts) can read it by reflection.

## How it fits together

```text
openspec/specs/user-auth/spec.md                  src/test/java/.../LoginTest.java

#### Scenario: Valid credentials  ◀───────────▶  @OpenSpec(capability = "user-auth",
      <a id="valid-credentials"></a>                       scenario = "valid-credentials")
                                                   @Test void validLogin() { … }
```

1. Give a scenario a **slug**, a stable id written as an anchor at the end of its heading.
2. Annotate the test with the **capability** (the spec's directory below `openspec/specs/`) and the **slug**.
3. The plugin resolves the pair and shows gutter icons on both sides.

The slug, not the scenario's name, is the link, so you can reword a scenario heading without breaking any test.

## Contents

- [Installation](installation.md): add the annotation to Gradle or Maven, install the IntelliJ plugin.
- [The `@OpenSpec` annotation](annotation.md): slugs, linking tests to scenarios and capabilities, rules and examples.
- [The IntelliJ plugin](intellij-plugin.md): navigation, highlighting, inspections and their settings.

## Requirements

| Component | Requires |
|---|---|
| `spec-anchor-annotations` | Java 8 or later, no dependencies |
| IntelliJ plugin | IntelliJ IDEA 2025.2 or later, with the bundled Java and Markdown plugins |
