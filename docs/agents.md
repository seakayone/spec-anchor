# Using with AI agents

An AI agent working in your OpenSpec project does not know about scenario slugs or `@OpenSpec` unless something tells it. The place to tell it is `openspec/config.yaml`: OpenSpec passes its `rules` and `operations` entries to the agent through `openspec instructions`, which every agent's OpenSpec workflow (`/opsx:propose`, `/opsx:apply`, …) calls. That works the same for every agent, with no per-tool setup.

The convention has two halves, written at different times, so the snippet has two parts:

| When | What the agent must do | Config entry |
|---|---|---|
| Writing delta specs (propose) | put a slug anchor on scenario headings | `rules.specs` |
| Implementing (apply) | annotate the tests with `@OpenSpec` | `operations.apply.guidance` |

## The snippet

Add this to `openspec/config.yaml`:

```yaml
rules:
  specs:
    - 'End each new scenario heading with a slug anchor, e.g. `#### Scenario: Valid credentials <a id="valid-credentials"></a>` (lowercase a-z, 0-9, single hyphens, unique per spec.md)'
    - 'When modifying a scenario, keep its existing `<a id="..."></a>` anchor unchanged'

operations:
  apply:
    guidance:
      - 'Annotate each test that verifies a spec scenario with `@OpenSpec(capability = "<path under openspec/specs/>", scenario = "<slug>")` from `org.kleinb.openspec.annotation` (repeatable)'
```

If your config already has `rules` or `operations`, merge the entries into the existing sections.

Keep the single quotes. An unquoted `Scenario: Valid` contains `": "`, which YAML reads as a key/value pair; OpenSpec then warns and ignores the rule.

The second `specs` rule matters on archive: a `MODIFIED` requirement replaces the whole requirement in the main spec, so a scenario heading rewritten without its anchor loses the slug, and every test linked to it stops resolving.

## Making it mandatory

The snippet asks the agent to link tests; it does not insist. To make linking part of "done", use stronger wording and add a `tasks` rule so the linking work shows up as tasks:

```yaml
rules:
  tasks:
    - 'Every task that changes Java code includes writing or updating the tests that cover the affected scenarios, annotated with `@OpenSpec`'

operations:
  apply:
    guidance:
      - 'Every Java code change MUST come with tests annotated `@OpenSpec(capability = "<path under openspec/specs/>", scenario = "<slug>")` for each scenario they verify; a change is not done while a scenario it touches has no linked test'
```

This is guidance, not enforcement: nothing fails if an agent ignores it. The [IntelliJ plugin](intellij-plugin.md) shows what is still missing.

## Check it

Run the instructions for any change and look for your entries:

```sh
openspec instructions specs --change <change> --json   # "rules" lists the slug rules
openspec instructions apply --change <change>          # "Operation Guidance" lists the annotation line
```

Tested with OpenSpec 1.14.3. The full slug rules and annotation examples are in [the `@OpenSpec` annotation](annotation.md).
