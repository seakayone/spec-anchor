#!/usr/bin/env bash
# Usage: scripts/release.sh [<version>] [--dry-run]
# Bumps the version, builds, commits, tags, pushes, deploys to Maven Central and creates the
# GitHub release (see publish.sh).
#   <version>  release exactly this version, without asking
#   (none)     suggest the next version from the conventional commits since the last tag
#              (git-cliff) and ask; accept the suggestion with Enter or type another version
# A version must be semantic (MAJOR.MINOR.PATCH) and higher than the current one.
# With --dry-run, builds and rehearses the publish (using the suggestion if no version is given,
# without asking), then reverts: no commit, tag or push. The version is printed at the end. A dirty
# working tree is allowed (with a warning); a real release never runs on a dirty tree.
set -euo pipefail

dry_run=false
version=""
for arg in "$@"; do
  case "$arg" in
    --dry-run) dry_run=true ;;
    -*) echo "unknown option: $arg" >&2; exit 1 ;;
    *) [[ -z "$version" ]] || { echo "more than one version given" >&2; exit 1; }
       version="${arg#v}" ;;
  esac
done

cd "$(dirname "$0")/.."

semver='^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$'

is_higher() { [[ "$1" != "$2" && "$(printf '%s\n%s\n' "$1" "$2" | sort -V | tail -n1)" == "$1" ]]; }

check_version() {
  [[ "$1" =~ $semver ]] \
    || { echo "'$1' is not a semantic version (expected MAJOR.MINOR.PATCH, e.g. 1.2.3)" >&2; return 1; }
  is_higher "$1" "$current" \
    || { echo "'$1' must be higher than the current version $current" >&2; return 1; }
}

[[ "$(git branch --show-current)" == "main" ]] || { echo "must be on main" >&2; exit 1; }
if [[ -n "$(git status --porcelain)" ]]; then
  $dry_run || { echo "working tree not clean" >&2; exit 1; }
  echo "warning: working tree not clean; the dry run includes your uncommitted changes" >&2
fi
scripts/publish.sh --check

# current = the higher of version.txt and the latest release tag
current="$(<version.txt)"
latest_tag="$(git tag --list 'v[0-9]*' --sort=-v:refname | head -n1)"
latest_tag="${latest_tag#v}"
[[ "$current" =~ $semver ]] || { echo "version.txt does not contain a semantic version: $current" >&2; exit 1; }
if [[ -n "$latest_tag" ]] && is_higher "$latest_tag" "$current"; then current="$latest_tag"; fi

if [[ -n "$version" ]]; then
  check_version "$version" || exit 1
else
  command -v git-cliff >/dev/null || { echo "git-cliff not installed (run via mise)" >&2; exit 1; }
  suggested="$(git-cliff --bumped-version 2>/dev/null || true)"
  suggested="${suggested#v}"
  check_version "$suggested" || { echo "could not calculate a valid next version" >&2; exit 1; }

  if $dry_run; then
    version="$suggested"
  else
    [[ -t 0 ]] || { echo "no version given and no terminal to ask on" >&2; exit 1; }
    echo "Release notes for v$suggested:"
    echo
    scripts/publish.sh --notes "$suggested" || true
    echo; echo
    echo "Current version: $current"
    while true; do
      read -r -p "Version to release [$suggested]: " version \
        || { echo; echo "aborted" >&2; exit 1; }
      version="${version:-$suggested}"
      version="${version#v}"
      check_version "$version" && break
    done
  fi
fi

! git rev-parse "v$version" >/dev/null 2>&1 || { echo "tag v$version already exists" >&2; exit 1; }
rc=0
git ls-remote --exit-code --tags origin "v$version" >/dev/null 2>&1 || rc=$?
case "$rc" in
  0) echo "tag v$version already exists on origin" >&2; exit 1 ;;
  2) ;; # no such tag on origin
  *) echo "could not check origin for tag v$version (network?)" >&2; exit 1 ;;
esac

if $dry_run; then
  # restore from copies, not git, so uncommitted edits to these files survive
  backup="$(mktemp -d)"
  cp version.txt README.md "$backup"/
  trap 'cp "$backup"/version.txt "$backup"/README.md . && rm -rf "$backup"' EXIT
fi

if ! $dry_run; then
  # a failed build must not leave a half-bumped, dirty tree behind
  trap 'git checkout -- version.txt README.md' EXIT
fi

echo "$version" > version.txt
sed -i.bak -E -e "s/(spec-anchor-annotations:)[0-9]+\.[0-9]+\.[0-9]+/\1$version/" \
  -e "s#<version>[0-9]+\.[0-9]+\.[0-9]+</version>#<version>$version</version>#" README.md
rm -f README.md.bak

./gradlew clean build

if $dry_run; then
  scripts/publish.sh --dry-run
  echo "release-dry ok: the next release would be $version"
  exit 0
fi

trap - EXIT
if ! git diff --quiet; then
  git commit -am "chore: release $version"
fi
git tag -s "v$version" -m "v$version"
git push origin main "v$version"

scripts/publish.sh
