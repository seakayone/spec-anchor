package org.kleinb.openspec.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container for repeated {@link OpenSpec} annotations. The compiler creates it; write {@code @OpenSpec} several times
 * instead of using it directly.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface OpenSpecs {

    /**
     * The repeated annotations.
     *
     * @return the repeated annotations
     */
    OpenSpec[] value();
}
