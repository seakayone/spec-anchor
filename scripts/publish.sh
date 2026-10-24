#!/usr/bin/env bash
# Usage: scripts/publish.sh [--check | --notes [version] | --github-only | <jreleaser args, e.g. --dry-run>]
# Stages spec-anchor-annotations with Gradle, deploys to Maven Central with the JReleaser CLI and
# creates the GitHub release with gh, with the IntelliJ plugin zip attached.
#   --check        only verify that tools, credentials and signing keys are available
#   --github-only  only create the GitHub release (recovery after Central succeeded)
#   --notes [ver]  print the release notes for a version (default: version.txt) and exit
#   --dry-run      passed to JReleaser; stages locally, uploads nothing, creates no release,
#                  and prints the summary with the release notes that would be used
set -euo pipefail

cd "$(dirname "$0")/.."

mode="${1:-}"
config="$HOME/.jreleaser/config.properties"
notes="out/jreleaser/release/CHANGELOG.md"

# Release notes come from JReleaser's conventional-commits changelog (previous tag..this tag);
# gh --generate-notes only lists merged pull requests, which direct commits to main never have.
generate_notes() { # <version>
  JRELEASER_PROJECT_VERSION="$1" jreleaser changelog --git-root-search >/dev/null 2>&1 \
    || { echo "could not generate the release notes" >&2; return 1; }
}

if [[ "$mode" == "--notes" ]]; then
  command -v jreleaser >/dev/null || { echo "jreleaser not installed (run via mise)" >&2; exit 1; }
  generate_notes "${2:-$(<version.txt)}"
  cat "$notes"
  exit 0
fi

has_setting() {
  [[ -n "${!1:-}" ]] && return 0
  [[ -f "$config" ]] && grep -Eq "^$1=.+" "$config"
}

missing=()
command -v gh >/dev/null || missing+=("gh (not installed)")
command -v jreleaser >/dev/null || missing+=("jreleaser (not installed; run via mise)")
if [[ "$mode" != "--github-only" ]]; then
  for var in JRELEASER_GITHUB_TOKEN JRELEASER_MAVENCENTRAL_USERNAME JRELEASER_MAVENCENTRAL_PASSWORD; do
    has_setting "$var" || missing+=("$var")
  done
  for key in public.pgp private.pgp; do
    [[ -f "$HOME/.jreleaser/$key" ]] || missing+=("$HOME/.jreleaser/$key")
  done
fi
if command -v gh >/dev/null && ! gh auth status >/dev/null 2>&1; then
  missing+=("gh authentication (run gh auth login or set GH_TOKEN)")
fi

if ((${#missing[@]})); then
  echo "missing publish settings (credentials go in $config or the environment):" >&2
  printf '  - %s\n' "${missing[@]}" >&2
  exit 1
fi

[[ "$mode" == "--check" ]] && { echo "publish settings ok"; exit 0; }

version="$(<version.txt)"
export JRELEASER_PROJECT_VERSION="$version"
export JRELEASER_GPG_PUBLIC_KEY="${JRELEASER_GPG_PUBLIC_KEY:-$HOME/.jreleaser/public.pgp}"
export JRELEASER_GPG_SECRET_KEY="${JRELEASER_GPG_SECRET_KEY:-$HOME/.jreleaser/private.pgp}"

plugin_zip="spec-anchor-intellij-plugin/build/distributions/spec-anchor-intellij-plugin-$version.zip"

create_github_release() {
  if gh release view "v$version" >/dev/null 2>&1; then
    gh release upload "v$version" "$plugin_zip" --clobber
  else
    gh release create "v$version" "$plugin_zip" --title "v$version" --verify-tag --notes-file "$notes"
  fi
}

print_summary() { # <heading>
  local repo_url
  repo_url="$(gh repo view --json url -q .url)"
  printf '\n===== %s =====\n' "$1"
  printf 'Version:        %s (tag v%s)\n' "$version" "$version"
  printf 'GitHub release: %s/releases/tag/v%s\n' "$repo_url" "$version"
  printf 'Plugin zip:     %s\n' "$plugin_zip"
  printf 'Maven Central:  https://central.sonatype.com/artifact/org.kleinb.openspec/spec-anchor-annotations/%s\n' "$version"
  printf '\nRelease notes:\n\n'
  cat "$notes"
  printf '\n\n'
}

generate_notes "$version"

if [[ "$mode" == "--github-only" ]]; then
  ./gradlew :spec-anchor-intellij-plugin:buildPlugin
  create_github_release
  print_summary "release summary"
  exit 0
fi

./gradlew :spec-anchor-annotations:publish :spec-anchor-intellij-plugin:buildPlugin
jreleaser deploy --git-root-search "$@"

# The native JReleaser CLI cannot create tags (JGit fails under GraalVM), so release.sh
# pushes the tag and the GitHub release is created here.
if [[ " $* " == *" --dry-run "* ]]; then
  print_summary "dry run summary (nothing was published)"
else
  create_github_release
  print_summary "release summary"
fi
