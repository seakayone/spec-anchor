# The `@OpenSpec` annotation

`@OpenSpec` declares which OpenSpec capability, and optionally which scenario of it, a test verifies.

```java
import org.kleinb.openspec.annotation.OpenSpec;

@OpenSpec(capability = "user-auth", scenario = "valid-credentials")
@Test
void validLogin() { }
```

| Attribute | Required | Meaning |
|---|---|---|
| `capability` | yes | Directory of the capability's `spec.md`, relative to `openspec/specs/`. |
| `scenario` | no | Slug of one scenario in that capability. Leave it out to link to the whole capability. |

## Step 1: give the scenario a slug

A test cannot point at a scenario by its name, because names get reworded. Instead each scenario you want to link gets a **slug**: a short, stable id written as an HTML anchor at the very end of the `#### Scenario:` heading.

```markdown
### Requirement: Users can log in

#### Scenario: Valid credentials <a id="valid-credentials"></a>
- **WHEN** a user submits a known email and the matching password
- **THEN** they are logged in
```

Markdown renderers (GitHub, IntelliJ's preview) hide the anchor, so the rendered spec still reads normally, and the anchor doubles as a link target (`spec.md#valid-credentials`).

A valid slug:

- consists of lowercase ASCII letters `a`–`z`, digits `0`–`9` and hyphens,
- starts and ends with a letter or digit, and has no double hyphens,
- is written exactly as `<a id="slug"></a>` (double quotes, `id` not `name`) at the end of the heading,
- is unique within its `spec.md`. The same slug may appear in different capabilities.

| Heading | Result |
|---|---|
| `#### Scenario: Valid credentials <a id="valid-credentials"></a>` | slug `valid-credentials` |
| `#### Scenario: Valid credentials` | no slug; tests cannot link to it, nothing is reported |
| `#### Scenario: Valid credentials <a id="Valid-Credentials"></a>` | error: uppercase |
| `#### Scenario: Größe <a id="größe"></a>` | error: non-ASCII |
| `#### Scenario: Valid credentials <a name="valid-credentials"></a>` | error: wrong attribute |
| `#### Scenario: <a id="valid-credentials"></a> Valid credentials` | error: not at the end |

Slugging is opt-in per scenario: add slugs where you want traceability and leave the rest alone.

## Step 2: annotate the test

### Link to a scenario

```java
@OpenSpec(capability = "user-auth", scenario = "valid-credentials")
@Test
void validLogin() { }
```

The slug is matched exactly and case-sensitively, and only within the named capability.

### Link to a whole capability

Leave out `scenario`. This is useful on a test class whose tests exercise a capability as a whole:

```java
@OpenSpec(capability = "user-auth")
class UserAuthIntegrationTest { }
```

A capability-level link does **not** count as covering any of the capability's scenarios; see [untested scenarios](intellij-plugin.md#untested-scenarios).

### Nested capabilities

If specs are grouped in subdirectories, the capability is the whole path below `openspec/specs/`:

```java
// openspec/specs/identity/user-auth/spec.md
@OpenSpec(capability = "identity/user-auth", scenario = "valid-credentials")
```

### Several scenarios per test

The annotation is repeatable, so one test can cover several scenarios or capabilities:

```java
@OpenSpec(capability = "user-auth", scenario = "valid-credentials")
@OpenSpec(capability = "audit-log", scenario = "login-recorded")
@Test
void loginIsAudited() { }
```

### Where it goes

`@OpenSpec` is allowed on classes and methods only; anywhere else is a compile error. It works with any test framework (JUnit 4, JUnit 5, TestNG, …) because it does not interact with the framework at all.

## Reading the annotation at runtime

The annotation has runtime retention, so reporting tools can read it by reflection. Because it is repeatable, use `getAnnotationsByType`, which returns all of them whether there is one or several:

```java
for (OpenSpec link : method.getAnnotationsByType(OpenSpec.class)) {
    System.out.println(link.capability() + " / " + link.scenario()); // scenario() is "" for capability links
}
```
