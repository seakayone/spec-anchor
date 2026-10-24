package org.kleinb.openspec.gradle;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.logging.Logging;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;
import org.kleinb.openspec.gradle.check.Link;
import org.kleinb.openspec.gradle.check.Links;

/** Reads the links in the test JVM, so test classes compiled for a newer Java than the daemon's still load. */
public abstract class ReadLinks implements WorkAction<ReadLinks.Parameters> {

    public interface Parameters extends WorkParameters {
        ConfigurableFileCollection getClassesDirs();

        ConfigurableFileCollection getClasspath();

        /** Receives the links as a serialized {@code ArrayList<Link>}. */
        RegularFileProperty getOutput();
    }

    @Override
    public void execute() {
        Parameters parameters = getParameters();
        List<Link> links = Links.read(
            List.copyOf(parameters.getClassesDirs().getFiles()),
            List.copyOf(parameters.getClasspath().getFiles()),
            Logging.getLogger(ReadLinks.class)::warn);
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(parameters.getOutput().get().getAsFile().toPath()))) {
            out.writeObject(new ArrayList<>(links));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
