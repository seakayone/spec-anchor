# Installation

You need the annotation library on your test classpath and the plugin in IntelliJ IDEA. The annotation works without the plugin (it is only metadata), but the plugin is what makes it useful while you work. The Gradle plugin is optional: add it to check specs against tests in the build.

## Annotation library

`spec-anchor-annotations` is published to Maven Central. It needs Java 8 or later and brings in no other dependencies. Add it with test scope, since only tests use it.

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    testImplementation("org.kleinb.openspec:spec-anchor-annotations:0.3.0")
}
```

### Gradle (Groovy DSL)

```groovy
dependencies {
    testImplementation 'org.kleinb.openspec:spec-anchor-annotations:0.3.0'
}
```

### Maven

```xml
<dependency>
    <groupId>org.kleinb.openspec</groupId>
    <artifactId>spec-anchor-annotations</artifactId>
    <version>0.3.0</version>
    <scope>test</scope>
</dependency>
```

The annotation is `org.kleinb.openspec.annotation.OpenSpec`.

## IntelliJ plugin

The plugin requires IntelliJ IDEA 2025.2 or later (Community or Ultimate) with the bundled *Java* and *Markdown* plugins enabled. It is not on the JetBrains Marketplace yet, so install it from disk:

1. Download `spec-anchor-intellij-plugin-<version>.zip` from the [GitHub releases](https://github.com/seakayone/spec-anchor/releases). Do not unpack it.
2. In IntelliJ, open *Settings → Plugins*, click the gear icon ⚙ and choose *Install Plugin from Disk*.

   ![Install Plugin from Disk in the Plugins settings](images/install-from-disk.png)

3. Pick the zip file and restart the IDE if asked.
4. Check that *Spec Anchor* is listed and enabled under *Settings → Plugins → Installed*.

   ![Spec Anchor listed among the installed plugins](images/plugin-installed.png)

To update, install the newer zip the same way; it replaces the old version.

## Gradle plugin

The Gradle plugin is published to Maven Central, so add Maven Central to the plugin repositories in `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}
```

Then apply it in the project that contains your tests:

```kotlin
plugins {
    java
    id("org.kleinb.openspec") version "0.3.0"
}
```

It needs Gradle 9 or later and Java 17 or later. See [the Gradle plugin](gradle-plugin.md) for what it reports and how to fail the build.

## Project layout

Both plugins look for specs in the standard OpenSpec locations of the open project:

- `openspec/specs/**/spec.md`: the main specs.
- `openspec/changes/<change>/specs/**/spec.md`: specs in active changes, so you can link and review tests before a change is archived.

Specs below `openspec/changes/archive/` are ignored. No further configuration is needed (the Gradle plugin's `openspecDir` option can point elsewhere).

Next: [link your first test](annotation.md).
