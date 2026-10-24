# open-spec-plugin

Links Java tests to [OpenSpec](https://github.com/Fission-AI/OpenSpec) scenarios and lets you navigate between them in IntelliJ IDEA.

## Modules

- `openspec-annotations` – the `@OpenSpec` annotation (Java 8+, no dependencies).
- `openspec-intellij-plugin` – IntelliJ IDEA 2025.2+ plugin: gutter navigation between `spec.md` scenarios and annotated tests, Ctrl+Click from the annotation to the spec, and highlighting of unknown references and invalid or duplicate slugs.

## Linking a test to a scenario

Give the scenario a slug (lowercase ASCII, digits, single hyphens) as an anchor at the end of its heading:

```markdown
#### Scenario: Valid credentials <a id="valid-credentials"></a>
```

Annotate the test with the capability path (relative to `openspec/specs/`) and the slug:

```java
@OpenSpec(capability = "user-auth", scenario = "valid-credentials")
@Test
void validLogin() { }
```

Leave out `scenario` to link to the whole capability. The annotation is repeatable.

## Build

Requires JDK 21 (`mise install` sets up JDK and Gradle from `mise.toml`).

```sh
./gradlew build                                      # compile and test everything
./gradlew :openspec-intellij-plugin:buildPlugin      # plugin zip in openspec-intellij-plugin/build/distributions
./gradlew :openspec-intellij-plugin:runIde           # try the plugin in a sandbox IDE
```
