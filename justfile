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

# Preview the GitHub Pages docs site at http://localhost:4000/spec-anchor/ (Docker; gems cached in a volume)
docs:
    docker run --rm -it -p 4000:4000 -p 35729:35729 -v "$PWD/docs":/site -v spec-anchor-gems:/usr/local/bundle -w /site ruby:3.3 \
        bash -c 'bundle install --quiet && bundle exec jekyll serve --host 0.0.0.0 --livereload'
