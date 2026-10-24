default:
    @just --list

# Build and test everything
build:
    ./gradlew build

# Release: suggests the next version from the conventional commits and asks; `just release 1.2.3` skips the question
release version="":
    scripts/release.sh {{version}}

# Rehearse a release (suggested or given version): build + publish dry run, then revert; no commit, tag or push
release-dry version="":
    scripts/release.sh {{version}} --dry-run

# Publish the current version; use `--github-only` if Central succeeded but the GitHub release is missing
publish *args:
    scripts/publish.sh {{args}}
