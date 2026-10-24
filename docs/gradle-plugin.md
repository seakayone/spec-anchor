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
| `removed-reference` | An `@OpenSpec` names a slug that only exists in a requirement an active change removes or modifies away (see [Changes](#changes)). |

A heading is reported at most once for its slug, as `missing-slug` or `invalid-slug`. Only headings with a valid slug can be reported as `untested-scenario`.

## Changes

The task reads the specs as they will be once the active changes are applied:

- A requirement that a change lists under `## REMOVED Requirements` is gone. Its scenarios in the living spec (`openspec/specs/…`) get no findings, and tests that still link to them are reported as `removed-reference`.
- A requirement that a change lists under `## MODIFIED Requirements` is replaced by the change's version. Only the change's scenarios are checked, so a modified scenario isn't reported twice. A scenario the modification drops counts as removed.
- Requirements match by name, ignoring differences in whitespace, as in OpenSpec. `RENAMED` doesn't affect slugs, so it's ignored.

`failOnRemovedReference` is off by default. Until the change is applied, the linked test is still valid. Turn it on, for example in CI before archiving, to make sure those tests are deleted.

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

By default the task fails on invalid slugs, duplicate slugs and unknown references, since those are always mistakes. It only reports untested scenarios and missing slugs, which are normal while a change is being written. Each kind has its own option:

```kotlin
specAnchor {
    failOnUntestedScenario = true   // default false
    failOnMissingSlug = true        // default false
    failOnInvalidSlug = false       // default true
    failOnDuplicateSlug = false     // default true
    failOnUnknownReference = false  // default true
    failOnRemovedReference = true   // default false
}
```

| Option | Default | Effect |
|---|---|---|
| `openspecDir` | `openspec` in the root project | The OpenSpec directory to read, for example `file("docs/openspec")`. If it doesn't exist, there are no specs, so every annotation is reported as `unknown-reference`. |
| `failOnUntestedScenario` | `false` | Fail on `untested-scenario`. |
| `failOnMissingSlug` | `false` | Fail on `missing-slug`. |
| `failOnInvalidSlug` | `true` | Fail on `invalid-slug`. |
| `failOnDuplicateSlug` | `true` | Fail on `duplicate-slug`. |
| `failOnUnknownReference` | `true` | Fail on `unknown-reference`. |
| `failOnRemovedReference` | `false` | Fail on `removed-reference`. |

The task always reports all findings before it fails. It fails exactly when it finds a kind whose option is on, and the failure message names each such kind with its count and no others, for example `spec-anchor check failed: 2 untested-scenario, 1 unknown-reference`.

To adopt the plugin without breaking the build, set the three default options to `false` first. Once every scenario has a slug and a test, turn on `failOnMissingSlug` and `failOnUntestedScenario` too.

## Multi-project builds

The task only sees the tests of the project it is applied to. In a build with several subprojects, a scenario tested in another subproject is reported as untested there. Apply the plugin in the subproject that holds the tests for your specs, or leave `failOnUntestedScenario` off.
