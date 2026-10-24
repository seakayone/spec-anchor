package org.kleinb.openspec.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Links a test class or test method to an OpenSpec capability and, optionally, to one of its scenarios.
 *
 * <pre>{@code
 * @OpenSpec(capability = "user-auth", scenario = "valid-credentials")
 * @Test
 * void validLogin() { }
 * }</pre>
 *
 * <p>Repeat the annotation to link one test to several scenarios or capabilities.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Repeatable(OpenSpecs.class)
public @interface OpenSpec {

    /**
     * The capability path: the directory of the capability's {@code spec.md} relative to {@code openspec/specs/},
     * for example {@code "user-auth"} for {@code openspec/specs/user-auth/spec.md} or {@code "identity/user-auth"}
     * for {@code openspec/specs/identity/user-auth/spec.md}.
     *
     * @return the capability path
     */
    String capability();

    /**
     * The slug of the linked scenario, or {@code ""} (the default) to link to the whole capability.
     *
     * <p>A slug is declared in the spec as an anchor at the end of the scenario heading, and is matched exactly.
     * Slugs consist of lowercase ASCII letters, digits and single hyphens:
     *
     * <pre>{@code #### Scenario: Valid credentials <a id="valid-credentials"></a>}</pre>
     *
     * @return the scenario slug, or {@code ""}
     */
    String scenario() default "";
}
