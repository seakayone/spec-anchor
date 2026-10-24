package org.kleinb.openspec.gradle;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.IgnoreEmptyDirectories;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.work.DisableCachingByDefault;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;
import org.kleinb.openspec.gradle.check.Checker;
import org.kleinb.openspec.gradle.check.Finding;
import org.kleinb.openspec.gradle.check.Finding.Kind;
import org.kleinb.openspec.gradle.check.Link;
import org.kleinb.openspec.gradle.check.Specs;

/** Checks the capability specs against the {@code @OpenSpec} links of the test classes. */
@DisableCachingByDefault(because = "Findings name paths relative to the root project, and the check is cheap")
public abstract class SpecAnchorCheck extends DefaultTask {

    @Internal
    public abstract DirectoryProperty getOpenspecDir();

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    @IgnoreEmptyDirectories
    public FileTree getSpecFiles() {
        return getOpenspecDir().getAsFileTree().matching(pattern -> pattern.include("**/spec.md"));
    }

    /** Spec locations in findings are relative to this directory, the root project's. */
    @Internal
    public abstract DirectoryProperty getBaseDir();

    @Classpath
    public abstract ConfigurableFileCollection getTestClassesDirs();

    @Classpath
    public abstract ConfigurableFileCollection getTestClasspath();

    @Nested
    @Optional
    public abstract Property<JavaLauncher> getJavaLauncher();

    /** Ids of the finding kinds that fail the task. */
    @Input
    public abstract SetProperty<String> getFailOn();

    @OutputFile
    public abstract RegularFileProperty getReportFile();

    @Inject
    protected abstract WorkerExecutor getWorkerExecutor();

    @TaskAction
    public void check() throws IOException {
        List<Finding> findings = Checker.check(
            Specs.discover(getOpenspecDir().get().getAsFile().toPath()), readLinks(), getBaseDir().get().getAsFile().toPath());

        Map<Kind, Long> counts = new EnumMap<>(Kind.class);
        findings.forEach(finding -> counts.merge(finding.kind(), 1L, Long::sum));
        List<String> lines = new ArrayList<>(findings.stream().map(Finding::toString).toList());
        lines.add(counts.isEmpty() ? "spec-anchor: no findings" : "spec-anchor: " + describe(counts));
        lines.forEach(findings.isEmpty() ? getLogger()::lifecycle : getLogger()::warn);
        File report = getReportFile().get().getAsFile();
        Files.createDirectories(report.getParentFile().toPath());
        Files.write(report.toPath(), lines);

        Map<Kind, Long> failing = new EnumMap<>(Kind.class);
        counts.forEach((kind, count) -> { if (getFailOn().get().contains(kind.id)) failing.put(kind, count); });
        if (!failing.isEmpty()) {
            throw new GradleException("spec-anchor check failed: " + describe(failing) + " (see " + report + ")");
        }
    }

    private static String describe(Map<Kind, Long> counts) {
        return counts.entrySet().stream().map(e -> e.getValue() + " " + e.getKey().id).collect(Collectors.joining(", "));
    }

    @SuppressWarnings("unchecked")
    private List<Link> readLinks() throws IOException {
        if (getTestClassesDirs().getAsFileTree().isEmpty()) return List.of();
        File output = new File(getTemporaryDir(), "links.bin");
        WorkQueue queue = getWorkerExecutor().processIsolation(spec -> {
            if (getJavaLauncher().isPresent()) {
                spec.forkOptions(fork -> fork.setExecutable(getJavaLauncher().get().getExecutablePath().getAsFile()));
            }
        });
        queue.submit(ReadLinks.class, parameters -> {
            parameters.getClassesDirs().from(getTestClassesDirs());
            parameters.getClasspath().from(getTestClasspath());
            parameters.getOutput().set(output);
        });
        queue.await();
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(output.toPath()))) {
            return (List<Link>) in.readObject();
        } catch (ClassNotFoundException e) {
            throw new UncheckedIOException(new IOException(e));
        }
    }
}
