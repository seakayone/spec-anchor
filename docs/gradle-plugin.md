# The Gradle plugin

The Gradle plugin brings the IDE's checks to the command line and CI. Its `specAnchorCheck` task reads your specs and the `@OpenSpec` annotations on your compiled tests and reports what is missing or broken. If you choose, it also fails the build.

It requires Gradle 9 or later and Java 17 or later.

## Setup

The plugin is published to Maven Central, not to the Gradle Plugin Portal, so add Maven Central to the plugin repositories in `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}
```

Then apply it next to `java` (or `kotlin("jvm")`) in the project that contains your tests:

```kotlin
plugins {
    java
    id("org.kleinb.openspec") version "0.3.0"
}

dependencies {
    testImplementation("org.kleinb.openspec:spec-anchor-annotations:0.3.0")
}
```

`gradle check` now runs `specAnchorCheck` after compiling the tests. You can also run it on its own with `gradle specAnchorCheck`.

## What it reports

The task reads every `spec.md` below `openspec/specs/` and below `openspec/changes/<change>/specs/`, ignoring `openspec/changes/archive/`, the same files the [IntelliJ plugin](intellij-plugin.md) uses. It reads links from the compiled classes of the `test` source set, so it finds both Java and Kotlin tests.

| Kind | Reported when |
|---|---|
| `untested-scenario` | A scenario has a valid slug, and no test is linked to that slug in its capability. A test linked only to the whole capability doesn't count. |
| `missing-slug` | A scenario heading has no `<a id="…"></a>` anchor. |
| `invalid-slug` | A scenario heading has an anchor that isn't a valid slug anchor (see [slugs](annotation.md)). |
| `duplicate-slug` | Two scenario headings in the same `spec.md` use the same slug. |
| `unknown-reference` | An `@OpenSpec` names a capability that has no `spec.md`, or a slug that the capability doesn't contain. |

A heading is reported at most once for its slug, as `missing-slug` or `invalid-slug`. Only headings with a valid slug can be reported as `untested-scenario`.

## Output

Each finding is printed on its own line, followed by a count per kind:

```text
untested-scenario openspec/specs/user-auth/spec.md:12: no test linked to scenario 'wrong-password'
missing-slug openspec/specs/user-auth/spec.md:20: scenario 'Locked account' has no slug anchor
unknown-reference com.example.LoginTest#expired: unknown scenario 'expired-token' in capability 'user-auth'
spec-anchor: 1 untested-scenario, 1 missing-slug, 1 unknown-reference
```

Spec locations are relative to the root project directory. Test locations are the class name, or `Class#method` for a method.

With nothing to report, the task prints `spec-anchor: no findings`. The same lines are written to `build/reports/spec-anchor/findings.txt`.

## Failing the build

By default the task only reports, so you can adopt it without breaking anything. To fail on a kind of finding, turn on its toggle:

```kotlin
specAnchor {
    failOnUntestedScenario = true
    failOnMissingSlug = true
    failOnInvalidSlug = true
    failOnDuplicateSlug = true
    failOnUnknownReference = true
}
```

| Option | Default | Effect |
|---|---|---|
| `openspecDir` | `openspec` in the root project | The OpenSpec directory to read, for example `file("docs/openspec")`. |
| `failOnUntestedScenario` | `false` | Fail on `untested-scenario`. |
| `failOnMissingSlug` | `false` | Fail on `missing-slug`. |
| `failOnInvalidSlug` | `false` | Fail on `invalid-slug`. |
| `failOnDuplicateSlug` | `false` | Fail on `duplicate-slug`. |
| `failOnUnknownReference` | `false` | Fail on `unknown-reference`. |

The task always reports all findings before it fails. The failure message names the enabled kinds that were found, for example `spec-anchor check failed: 2 untested-scenario`.

A common start is `failOnInvalidSlug`, `failOnDuplicateSlug` and `failOnUnknownReference`, since those are always mistakes. While you are writing a change, its scenarios are untested by design, so `failOnUntestedScenario` can wait.

## Multi-project builds

The task only sees the tests of the project it is applied to. In a build with several subprojects, a scenario tested in another subproject is reported as untested there. Apply the plugin in the subproject that holds the tests for your specs, or leave `failOnUntestedScenario` off.
