# spec-anchor

Links Java tests to [OpenSpec](https://github.com/Fission-AI/OpenSpec) scenarios and lets you navigate between them in IntelliJ IDEA.

## Modules

- `spec-anchor-annotations` – the `@OpenSpec` annotation (Java 8+, no dependencies).
- `spec-anchor-intellij-plugin` – IntelliJ IDEA 2025.2+ plugin: gutter navigation between `spec.md` scenarios and annotated tests, Ctrl+Click from the annotation to the spec, and highlighting of unknown references, invalid or duplicate slugs, and untested scenarios.

## Installation

The annotation is on Maven Central:

```kotlin
testImplementation("org.kleinb.openspec:spec-anchor-annotations:0.0.1")
```

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

## Untested scenarios

The plugin warns on every scenario heading that has a slug but no linked test. A test linked only to the whole capability does not cover its scenarios, and scenarios without a slug are not reported. Only Java tests are found. To turn the warning off or change its level, go to *Settings → Editor → Inspections → OpenSpec → OpenSpec scenario without linked test*.

## Build

Requires JDK 21 (`mise install` sets up JDK and Gradle from `mise.toml`).

```sh
./gradlew build                                      # compile and test everything
./gradlew :spec-anchor-intellij-plugin:buildPlugin      # plugin zip in spec-anchor-intellij-plugin/build/distributions
./gradlew :spec-anchor-intellij-plugin:runIde           # try the plugin in a sandbox IDE
```

## Release

`just release` publishes `spec-anchor-annotations` to Maven Central and creates the GitHub release with the plugin zip attached (`just release-dry` rehearses it). Credentials go in `~/.jreleaser/config.properties` (`JRELEASER_GITHUB_TOKEN`, `JRELEASER_MAVENCENTRAL_USERNAME`, `JRELEASER_MAVENCENTRAL_PASSWORD`), signing keys in `~/.jreleaser/public.pgp` and `private.pgp`; `just publish --check` verifies them.

## License

[Apache License 2.0](LICENSE)
