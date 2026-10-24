package org.kleinb.openspec.gradle;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;

/** The {@code specAnchor { }} block. Every {@code failOn…} toggle is off by default, so the check only reports. */
public abstract class SpecAnchorExtension {

    /** The OpenSpec directory; defaults to {@code openspec} in the root project. */
    public abstract DirectoryProperty getOpenspecDir();

    public abstract Property<Boolean> getFailOnUntestedScenario();

    public abstract Property<Boolean> getFailOnMissingSlug();

    public abstract Property<Boolean> getFailOnInvalidSlug();

    public abstract Property<Boolean> getFailOnDuplicateSlug();

    public abstract Property<Boolean> getFailOnUnknownReference();
}
