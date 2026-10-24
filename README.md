# spec-anchor

Links Java tests to [OpenSpec](https://github.com/Fission-AI/OpenSpec) scenarios, lets you navigate between them in IntelliJ IDEA, and checks them in your Gradle build.

**Documentation:** [docs/](docs/README.md): [installation](docs/installation.md), [the `@OpenSpec` annotation](docs/annotation.md), [the IntelliJ plugin](docs/intellij-plugin.md), [the Gradle plugin](docs/gradle-plugin.md), [using with AI agents](docs/agents.md).

## Modules

- `spec-anchor-annotations` – the `@OpenSpec` annotation (Java 8+, no dependencies).
- `spec-anchor-gradle-plugin` – Gradle plugin `org.kleinb.openspec` (Gradle 9+, Java 17+): a `specAnchorCheck` task, run by `check`, that reports untested scenarios, missing, invalid or duplicate slugs, and unknown references, and can fail the build on the kinds you choose.
- `spec-anchor-intellij-plugin` – IntelliJ IDEA 2025.2+ plugin: gutter navigation between `spec.md` scenarios and annotated tests, Ctrl+Click from the annotation to the spec, and highlighting of unknown references, invalid or duplicate slugs, and untested scenarios.

## Installation

The annotation is on Maven Central. Gradle:

```kotlin
testImplementation("org.kleinb.openspec:spec-anchor-annotations:0.3.0")
```

Maven:

```xml
<dependency>
    <groupId>org.kleinb.openspec</groupId>
    <artifactId>spec-anchor-annotations</artifactId>
    <version>0.3.0</version>
    <scope>test</scope>
</dependency>
```

The IntelliJ plugin (IntelliJ IDEA 2025.2 or later) is not on the JetBrains Marketplace yet. Download `spec-anchor-intellij-plugin-<version>.zip` from the [GitHub releases](https://github.com/seakayone/spec-anchor/releases), then in IntelliJ go to *Settings → Plugins → ⚙ → Install Plugin from Disk*, pick the zip and restart the IDE if asked.

The Gradle plugin is on Maven Central too. Add `mavenCentral()` to `pluginManagement { repositories { } }` in `settings.gradle.kts`, then:

```kotlin
plugins {
    id("org.kleinb.openspec") version "0.3.0"
}
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

The IntelliJ plugin warns on every scenario heading that has a slug but no linked test. A test linked only to the whole capability does not cover its scenarios, and scenarios without a slug are not reported. Only Java tests are found. To turn the warning off or change its level, go to *Settings → Editor → Inspections → OpenSpec → OpenSpec scenario without linked test*. The [Gradle plugin](docs/gradle-plugin.md) runs the same check in the build and also finds Kotlin tests.

## Build

Requires JDK 21 (`mise install` sets up JDK and Gradle from `mise.toml`).

```sh
./gradlew build                                      # compile and test everything
./gradlew :spec-anchor-intellij-plugin:buildPlugin      # plugin zip in spec-anchor-intellij-plugin/build/distributions
./gradlew :spec-anchor-intellij-plugin:runIde           # try the plugin in a sandbox IDE
./gradlew :spec-anchor-gradle-plugin:test -PspecAnchor.shard=0/1   # also run all 1024 fail-option combinations (k/n runs a 1/n slice)
```

## Release

`just release` publishes `spec-anchor-annotations` to Maven Central and creates the GitHub release with the plugin zip attached (`just release-dry` rehearses it). Credentials go in `~/.jreleaser/config.properties` (`JRELEASER_GITHUB_TOKEN`, `JRELEASER_MAVENCENTRAL_USERNAME`, `JRELEASER_MAVENCENTRAL_PASSWORD`), signing keys in `~/.jreleaser/public.pgp` and `private.pgp`; `just publish --check` verifies them.

## License

[Apache License 2.0](LICENSE)
