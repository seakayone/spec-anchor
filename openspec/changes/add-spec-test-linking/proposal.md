# Proposal

## Why

OpenSpec specs describe behavior as capabilities, requirements and scenarios, but there is no traceable connection between a scenario in `spec.md` and the Java test that verifies it. Developers cannot tell which tests cover a scenario, or which scenario a test is meant to prove, without searching by hand. This repository is greenfield, so the linking mechanism and its IDE support can be built together from the start.

## What Changes

- Set up a Gradle multi-project build (Kotlin DSL, Gradle wrapper) in this repository with two subprojects.
- Scenarios get a stable slug written in their heading, e.g. `#### Scenario: Valid credentials <a id="valid-credentials"></a>`. Tests link to the slug, not the name, so renaming a scenario does not break its links. Slugs are lowercase ASCII only and matched exactly.
- New Java library providing an `@OpenSpec` annotation with two string attributes, `capability` and `scenario` (the slug). It can be placed on test classes and test methods and links them to a capability (`openspec/specs/<capability>/spec.md`) and, optionally, to one scenario in it. It can be repeated so one test can cover several scenarios.
- New IntelliJ plugin that navigates in both directions:
  - from a capability or scenario in `spec.md` to the annotated tests, in the main specs and in active (unarchived) changes, so a change can be reviewed after apply;
  - from an `@OpenSpec` annotation to the referenced capability or scenario heading;
  - with highlighting of unknown capability/scenario values inside the annotation;
  - with an error on invalid slug anchors and a warning on duplicate slugs in a spec.
- The plugin never writes to spec files: slugs are written by hand, the link lives in the annotation and is resolved by the IDE.

Assumptions, recorded because they were not explicitly confirmed:
- The annotation attributes are `capability` (spec path relative to `openspec/specs/`, e.g. `user-auth` or `identity/user-auth`) and `scenario` (the scenario's slug; empty means "whole capability").
- The plugin is written in Kotlin; the annotation library is plain Java.

Out of scope (planned follow-ups, kept in mind):
- a coverage check, as a Gradle task and/or IDE inspection, that reports scenarios with no linked test and annotations pointing at missing specs;
- completion of capability/slug values in the annotation;
- an action that generates a slug for a scenario heading.

## Capabilities

### New Capabilities
- `spec-test-annotation`: the `@OpenSpec` annotation contract: where it may be placed, its attributes, repeatability, retention, and how its values identify a capability and scenario.
- `ide-spec-navigation`: how scenario slugs are declared and matched, and IntelliJ support for navigating between OpenSpec `spec.md` capabilities/scenarios and annotated Java tests, including highlighting of unresolved references and of invalid or duplicate slugs.

### Modified Capabilities
None.

## Impact

- New build: `settings.gradle.kts`, root `build.gradle.kts`, Gradle wrapper, version catalog.
- New subprojects: `openspec-annotations` (Java library, no runtime dependencies) and `openspec-intellij-plugin` (IntelliJ Platform Gradle Plugin, depends on the bundled Java and Markdown plugins).
- Toolchain: requires a JDK (21) and Gradle; neither is configured on this machine right now (`java` missing, `mise` has no Gradle version set).
- No existing code or specs are affected.
