package org.kleinb.openspec.gradle.check;

/** One problem found by the check, at a spec line ({@code path:line}) or a test ({@code Class} or {@code Class#method}). */
public record Finding(Kind kind, String location, String message) {

    public enum Kind {
        UNTESTED_SCENARIO("untested-scenario"),
        MISSING_SLUG("missing-slug"),
        INVALID_SLUG("invalid-slug"),
        DUPLICATE_SLUG("duplicate-slug"),
        UNKNOWN_REFERENCE("unknown-reference"),
        REMOVED_REFERENCE("removed-reference");

        public final String id;

        Kind(String id) {
            this.id = id;
        }
    }

    @Override
    public String toString() {
        return kind.id + " " + location + ": " + message;
    }
}
