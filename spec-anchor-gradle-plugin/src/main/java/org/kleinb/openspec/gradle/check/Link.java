package org.kleinb.openspec.gradle.check;

import java.io.Serializable;

/** One {@code @OpenSpec} on a test; [scenario] is empty for a link to the whole capability. */
public record Link(String test, String capability, String scenario) implements Serializable { }
