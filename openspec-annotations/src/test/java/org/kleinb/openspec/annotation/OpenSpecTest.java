package org.kleinb.openspec.annotation;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;

class OpenSpecTest {

    @OpenSpec(capability = "user-auth")
    static class CapabilityLinked {

        @OpenSpec(capability = "user-auth", scenario = "valid-credentials")
        void scenarioLinked() { }

        @OpenSpec(capability = "identity/user-auth", scenario = "valid-credentials")
        void nestedLinked() { }

        @OpenSpec(capability = "user-auth", scenario = "valid-credentials")
        @OpenSpec(capability = "user-auth", scenario = "wrong-password")
        void twoScenarios() { }
    }

    private static OpenSpec[] onMethod(String name) throws NoSuchMethodException {
        Method method = CapabilityLinked.class.getDeclaredMethod(name);
        return method.getAnnotationsByType(OpenSpec.class);
    }

    @Test
    @OpenSpec(capability = "spec-test-annotation", scenario = "link-scenario")
    void linksSpecificScenario() throws Exception {
        OpenSpec spec = onMethod("scenarioLinked")[0];
        assertEquals("user-auth", spec.capability());
        assertEquals("valid-credentials", spec.scenario());
    }

    @Test
    @OpenSpec(capability = "spec-test-annotation", scenario = "link-capability")
    void scenarioDefaultsToWholeCapability() {
        OpenSpec spec = CapabilityLinked.class.getAnnotation(OpenSpec.class);
        assertEquals("user-auth", spec.capability());
        assertEquals("", spec.scenario());
    }

    @Test
    @OpenSpec(capability = "spec-test-annotation", scenario = "nested-capability")
    void keepsNestedCapabilityPath() throws Exception {
        assertEquals("identity/user-auth", onMethod("nestedLinked")[0].capability());
    }

    @Test
    @OpenSpec(capability = "spec-test-annotation", scenario = "annotate-class")
    void annotatesClass() {
        assertTrue(CapabilityLinked.class.isAnnotationPresent(OpenSpec.class));
    }

    @Test
    @OpenSpec(capability = "spec-test-annotation", scenario = "annotate-method")
    void annotatesMethod() throws Exception {
        assertEquals(1, onMethod("scenarioLinked").length);
    }

    @Test
    @OpenSpec(capability = "spec-test-annotation", scenario = "repeatable")
    void isRepeatable() throws Exception {
        String[] scenarios = Arrays.stream(onMethod("twoScenarios")).map(OpenSpec::scenario).toArray(String[]::new);
        assertArrayEquals(new String[] {"valid-credentials", "wrong-password"}, scenarios);
    }

    @Test
    @OpenSpec(capability = "spec-test-annotation", scenario = "runtime-reflection")
    void isVisibleAtRuntime() throws Exception {
        Method self = OpenSpecTest.class.getDeclaredMethod("isVisibleAtRuntime");
        OpenSpec spec = self.getAnnotation(OpenSpec.class);
        assertEquals("spec-test-annotation", spec.capability());
        assertEquals("runtime-reflection", spec.scenario());
    }

    @Test
    @OpenSpec(capability = "spec-test-annotation", scenario = "annotate-field")
    void rejectsField() {
        String source = "import org.kleinb.openspec.annotation.OpenSpec;\n"
                + "class Fielded { @OpenSpec(capability = \"user-auth\") int field; }\n";
        JavaFileObject file = new SimpleJavaFileObject(URI.create("string:///Fielded.java"), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return source;
            }
        };
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        List<String> options = Arrays.asList("-proc:none", "-d", System.getProperty("java.io.tmpdir"),
                "-classpath", System.getProperty("java.class.path"));

        boolean compiled = compiler.getTask(null, null, diagnostics, options, null, Collections.singletonList(file)).call();

        assertFalse(compiled);
        assertTrue(diagnostics.getDiagnostics().stream()
                .anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR && d.getCode().contains("annotation")),
                diagnostics.getDiagnostics().toString());
    }
}
