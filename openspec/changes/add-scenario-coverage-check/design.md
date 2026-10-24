# Design

## Context

See proposal.md - Why. The behavior is in `specs/ide-spec-navigation/spec.md`. The plugin already has everything needed to decide coverage: `capabilityOf` (which files are capability specs, archive excluded), `scenarios(file)` (headings with slugs) and `linkedTests(project, capability, slug)` (Java tests, cached on PSI modification count). `ScenarioSlugInspection` shows the pattern for a Markdown `localInspection`.

## Goals / Non-Goals

**Goals:**
- One small inspection built only from the existing model functions.

**Non-Goals:**
- Quick fixes (e.g. "create test"), Gradle/CI reporting, Kotlin tests.
- Changing gutter markers: an untested scenario still shows no marker (existing `scenario-no-tests`).

## Decisions

### A separate inspection, not an extension of the slug inspection
`UntestedScenarioInspection` with short name `OpenSpecUntestedScenario`, registered for `Markdown`, `level="WARNING"`, `enabledByDefault="true"`, group `OpenSpec`.
- Alternative rejected: adding the check to `ScenarioSlugInspection`. Slug problems are always mistakes; missing tests are normal while authoring a change. Separate inspections let users disable or re-level coverage (Settings → Inspections, or per-file suppression) without losing slug errors.

### Check
`checkFile`: return nothing unless `capabilityOf(file)` is set. For each scenario with a slug, if `linkedTests(project, capability, slug)` is empty, report a problem on the whole `MarkdownHeader` with `ProblemHighlightType.WARNING` and message `No test linked to scenario '<slug>'`.
- Marking the whole heading (not only the name) keeps it simple and makes the warning hard to miss; it can overlap a slug error on the anchor, which is fine.
- `linkedTests` with a slug already ignores capability-only links, which gives the "capability-level test does not cover" rule for free.

### Freshness
No extra caching or listeners: `links()` is cached on `PsiModificationTracker.MODIFICATION_COUNT`, and any PSI change restarts highlighting of open editors, so adding or removing a Java test updates open specs.

### Testing
`checkHighlighting` fixture tests per spec scenario, in the existing `OpenSpecTestCase` style, each annotated with `@OpenSpec(capability = "ide-spec-navigation", scenario = "<slug>")`. The edit scenario: highlight, add a test class, highlight again.

## Risks / Trade-offs

- [Kotlin tests are invisible, so Kotlin-tested scenarios show as untested — in this repo, all of `ide-spec-navigation`] → expected until Kotlin support; users can disable the inspection meanwhile.
- [Change specs light up with warnings before tests are written] → intended visibility; the inspection can be switched off or set to weak warning.
- [Inspection runs `linkedTests` per scenario] → served from the cached link list, so a linear scan per heading; specs have tens of scenarios.
