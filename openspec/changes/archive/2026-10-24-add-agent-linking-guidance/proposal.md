# Proposal

## Why

The `@OpenSpec` annotation and the scenario slug convention exist, but an AI agent working in an OpenSpec project has no reason to know about them: it writes scenarios without slugs and tests without links. OpenSpec already delivers project configuration (`rules.<artifact>` and `operations.apply.guidance` in `openspec/config.yaml`) to every agent through `openspec instructions`, so a few lines of config make the convention visible at exactly the moments it matters, independent of which agent is used.

## What Changes

- New documentation page "Using with AI agents" with a copy-paste `config.yaml` snippet:
  - `rules.specs`: end new scenario headings with a slug anchor; keep existing anchors when modifying a scenario.
  - `operations.apply.guidance`: annotate tests that verify a scenario with `@OpenSpec`.
- Link the page from the docs index, the docs site sidebar and the README.
- This repository's own `openspec/config.yaml` adopts the snippet in a stricter form: every Java or Kotlin code change must come with `@OpenSpec`-linked tests for the scenarios it touches, plus a `rules.tasks` entry so test-linking work shows up in `tasks.md`.

Out of scope (later changes):

- Backfilling `@OpenSpec` links on this repository's existing tests and writing tests for uncovered scenarios.
- Plugin support for finding Kotlin tests (accepted gap: links in our Kotlin tests are not seen by the gutter icons or the untested-scenario inspection).
- Alternatives considered and rejected: an Agent Skill, a Claude Code plugin, a forked OpenSpec schema, an MCP server.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None. Docs and project configuration only; the change sets `skip_specs: true`.

## Impact

- `docs/agents.md` (new), `docs/README.md`, `docs/_config.yml` (sidebar), `README.md`.
- `openspec/config.yaml`: new `rules` and `operations` sections. Affects every future change planned and applied in this repository.
- No code, API or dependency changes.
