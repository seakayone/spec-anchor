package org.kleinb.openspec.gradle.check;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/** Reads {@code @OpenSpec} links from compiled test classes by reflection, without initializing them. */
public final class Links {

    static final String OPEN_SPEC = "org.kleinb.openspec.annotation.OpenSpec";

    private Links() { }

    /**
     * Links on the classes in [classesDirs], loaded together with [classpath]. Classes that fail to load are
     * passed to [warn] and skipped; without {@code OpenSpec} on the classpath there are no links.
     */
    public static List<Link> read(List<File> classesDirs, List<File> classpath, Consumer<String> warn) {
        List<URL> urls = new ArrayList<>();
        Stream.concat(classesDirs.stream(), classpath.stream()).forEach(file -> urls.add(url(file)));
        try (URLClassLoader loader = new URLClassLoader(urls.toArray(URL[]::new), ClassLoader.getPlatformClassLoader())) {
            Class<? extends Annotation> openSpec;
            try {
                openSpec = Class.forName(OPEN_SPEC, false, loader).asSubclass(Annotation.class);
            } catch (ClassNotFoundException e) {
                return List.of();
            }
            Method capability = openSpec.getMethod("capability");
            Method scenario = openSpec.getMethod("scenario");
            List<Link> links = new ArrayList<>();
            for (File dir : classesDirs) {
                for (String name : classNames(dir.toPath())) {
                    try {
                        Class<?> type = Class.forName(name, false, loader);
                        collect(type, type.getName(), openSpec, capability, scenario, links);
                        for (Method method : type.getDeclaredMethods()) {
                            // javac copies annotations onto bridge methods, which would count each link twice
                            if (method.isBridge() || method.isSynthetic()) continue;
                            collect(method, type.getName() + "#" + method.getName(), openSpec, capability, scenario, links);
                        }
                    } catch (ClassNotFoundException | LinkageError e) {
                        warn.accept("spec-anchor: skipped " + name + ": " + e);
                    }
                }
            }
            return links;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("unexpected @OpenSpec shape", e);
        }
    }

    private static void collect(AnnotatedElement element, String test, Class<? extends Annotation> openSpec,
                                Method capability, Method scenario, List<Link> links) throws ReflectiveOperationException {
        for (Annotation annotation : element.getAnnotationsByType(openSpec)) {
            links.add(new Link(test, (String) capability.invoke(annotation), (String) scenario.invoke(annotation)));
        }
    }

    private static List<String> classNames(Path dir) {
        if (!Files.isDirectory(dir)) return List.of();
        try (Stream<Path> files = Files.walk(dir)) {
            return files.map(dir::relativize).map(Path::toString)
                .filter(path -> path.endsWith(".class") && !path.endsWith("module-info.class") && !path.endsWith("package-info.class"))
                .map(path -> path.substring(0, path.length() - ".class".length()).replace(File.separatorChar, '.'))
                .sorted()
                .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static URL url(File file) {
        try {
            return file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
