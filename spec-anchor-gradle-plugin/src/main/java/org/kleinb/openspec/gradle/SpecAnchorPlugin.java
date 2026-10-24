package org.kleinb.openspec.gradle;

import java.util.List;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.testing.Test;
import org.kleinb.openspec.gradle.check.Finding.Kind;

/** Adds the {@code specAnchorCheck} task, which checks OpenSpec scenarios against the tests linked to them. */
public abstract class SpecAnchorPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        Directory rootDir = project.getIsolated().getRootProject().getProjectDirectory();
        SpecAnchorExtension extension = project.getExtensions().create("specAnchor", SpecAnchorExtension.class);
        extension.getOpenspecDir().convention(rootDir.dir("openspec"));
        extension.getFailOnUntestedScenario().convention(false);
        extension.getFailOnMissingSlug().convention(false);
        extension.getFailOnInvalidSlug().convention(false);
        extension.getFailOnDuplicateSlug().convention(false);
        extension.getFailOnUnknownReference().convention(false);

        TaskProvider<SpecAnchorCheck> check = project.getTasks().register("specAnchorCheck", SpecAnchorCheck.class, task -> {
            task.setGroup("verification");
            task.setDescription("Checks OpenSpec scenarios against the tests linked to them with @OpenSpec.");
            task.getOpenspecDir().set(extension.getOpenspecDir());
            task.getBaseDir().set(rootDir);
            failOn(task, extension.getFailOnUntestedScenario(), Kind.UNTESTED_SCENARIO);
            failOn(task, extension.getFailOnMissingSlug(), Kind.MISSING_SLUG);
            failOn(task, extension.getFailOnInvalidSlug(), Kind.INVALID_SLUG);
            failOn(task, extension.getFailOnDuplicateSlug(), Kind.DUPLICATE_SLUG);
            failOn(task, extension.getFailOnUnknownReference(), Kind.UNKNOWN_REFERENCE);
            task.getReportFile().set(project.getLayout().getBuildDirectory().file("reports/spec-anchor/findings.txt"));
        });

        project.getPluginManager().withPlugin("java", java -> {
            SourceSet test = project.getExtensions().getByType(SourceSetContainer.class).getByName(SourceSet.TEST_SOURCE_SET_NAME);
            check.configure(task -> {
                task.getTestClassesDirs().from(test.getOutput().getClassesDirs());
                task.getTestClasspath().from(test.getRuntimeClasspath());
                task.getJavaLauncher().set(project.getTasks().named("test", Test.class).flatMap(Test::getJavaLauncher));
            });
            project.getTasks().named("check").configure(task -> task.dependsOn(check));
        });
    }

    private static void failOn(SpecAnchorCheck task, Property<Boolean> toggle, Kind kind) {
        task.getFailOn().addAll(toggle.map(on -> on ? List.of(kind.id) : List.<String>of()));
    }
}
