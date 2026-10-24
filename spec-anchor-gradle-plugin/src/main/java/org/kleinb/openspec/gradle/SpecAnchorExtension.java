package org.kleinb.openspec.gradle;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;

/**
 * The {@code specAnchor { }} block. The toggles for findings that are always mistakes (invalid and duplicate slugs,
 * unknown references) are on by default; untested scenarios and missing slugs only fail when turned on.
 */
public abstract class SpecAnchorExtension {

    /** The OpenSpec directory; defaults to {@code openspec} in the root project. */
    public abstract DirectoryProperty getOpenspecDir();

    public abstract Property<Boolean> getFailOnUntestedScenario();

    public abstract Property<Boolean> getFailOnMissingSlug();

    public abstract Property<Boolean> getFailOnInvalidSlug();

    public abstract Property<Boolean> getFailOnDuplicateSlug();

    public abstract Property<Boolean> getFailOnUnknownReference();

    /** Fails on tests linked to a scenario that an active change removes; off by default, since such tests are fine until the change is applied. */
    public abstract Property<Boolean> getFailOnRemovedReference();
}
