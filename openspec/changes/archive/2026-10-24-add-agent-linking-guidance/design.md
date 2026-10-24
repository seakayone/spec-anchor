# Design

## Context

See proposal.md - Why. OpenSpec (1.14.3 at the time of writing) validates two optional `config.yaml` sections that matter here:

- `rules.<artifact-id>`: lines added to `openspec instructions <artifact>` output when an agent writes that artifact (here `specs` and `tasks`).
- `operations.apply.guidance`: lines printed as bullets by `openspec instructions apply` (`commands/workflow/instructions.js`); unknown fields under `operations.<op>` only warn.

Every agent's OpenSpec skill reads these commands, so the config is the one channel that reaches all agents without per-tool setup.

The slug convention has two halves written at different times: the anchor on the scenario heading (when delta specs are written, during propose) and the annotation on the test (during apply). Archiving a `MODIFIED` requirement replaces its whole block in the main spec.

## Goals / Non-Goals

**Goals:**
- A snippet short enough to paste and to be printed on every `instructions` call without noise.
- Each line points at the right moment: slug rules at spec time, annotation guidance at apply time.

**Non-Goals:**
- Enforcing links (compile-time checks, validators). Guidance only.
- Teaching the full slug rules in config; the docs page links to `annotation.md` for those.

## Decisions

**Config over skill/plugin/schema/MCP.** A skill is only loaded when its description matches what the agent is doing, so a "write a test" step may never trigger it; config is delivered on every relevant `instructions` call. A Claude Code plugin is not agent-agnostic. A forked `spec-driven` schema could bake the slug into the template but cuts users off from upstream schema updates. An MCP server is far too heavy for three rules.

**`rules.specs` in addition to apply guidance.** Apply guidance alone reaches the agent after scenarios are written, so it would either edit delta specs mid-apply or fall back to capability-level links, which do not count as scenario coverage. The second `specs` rule ("keep existing anchors") guards against `MODIFIED` requirements dropping a slug on archive and silently breaking linked tests.

**Docs snippet is advisory, this repo's config is mandatory.** Users decide how strict to be; the docs say "new scenarios get slugs" and "annotate tests that verify a scenario". This repo uses "MUST" wording, scoped to Java or Kotlin code changes, and adds a `rules.tasks` line so the linking work is an explicit task rather than an afterthought at apply time.

**Own docs page (`docs/agents.md`).** Keeps `annotation.md` about the annotation itself; the page gets a sidebar entry like the other pages.

## Risks / Trade-offs

- [Older OpenSpec versions may not know `operations`] → The docs page names the version the snippet was tested with; apply verifies the snippet with `openspec instructions` on that version.
- [Our Kotlin test links are invisible to the plugin] → Accepted gap; Kotlin support is a later change.
- [Mandatory linking while existing tests are unlinked] → The rule applies to new changes; the backfill is a later change.
- [Guidance is advisory; agents can ignore it] → Accepted; enforcement is out of scope.
