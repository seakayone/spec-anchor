# Tasks

## 1. This repository's OpenSpec config

- [x] 1.1 Add `rules.specs` (slug anchor on every scenario heading; keep existing anchors when modifying a scenario) and `rules.tasks` (tasks that change Java or Kotlin code include writing or updating the `@OpenSpec`-linked tests for the affected scenarios) to `openspec/config.yaml`; verify `openspec instructions specs --change add-agent-linking-guidance --json` and `openspec instructions tasks --change add-agent-linking-guidance --json` show the rules and print no config warnings
- [x] 1.2 Add `operations.apply.guidance` making `@OpenSpec` links mandatory for Java or Kotlin code changes (a change is not done while a scenario it touches has no linked test); verify `openspec instructions apply --change add-agent-linking-guidance` prints the guidance bullet and no config warnings
- [x] 1.3 Remove the commented-out `rules` and `operations` examples in `openspec/config.yaml` that the real sections now replace; verify the file still parses with `openspec context --json`

## 2. User documentation

- [x] 2.1 Write `docs/agents.md` ("Using with AI agents"): why the config is the agent-agnostic channel, the copy-paste snippet (`rules.specs` with the two slug rules, `operations.apply.guidance` with the annotation line, advisory wording), a note on making it mandatory, the OpenSpec version it was tested with, and links to `annotation.md` for the full slug rules; verify the snippet is valid by pasting it into a scratch copy of a config and running `openspec instructions apply` against it
- [x] 2.2 Link the page from the docs index (`docs/README.md` Contents), the site sidebar (`docs/_config.yml` `nav`) and the README documentation line; verify the docs site builds (`bundle exec jekyll build` in `docs/`, if available) and the new page appears in the sidebar, otherwise verify every link target exists with grep

## Workflow follow-up

- Later change: backfill `@OpenSpec` links on this repository's existing tests and write tests for uncovered scenarios.
- Later change: plugin support for finding Kotlin tests.
