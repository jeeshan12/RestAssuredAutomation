package com.framework.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Serial {

    /**
     * Documents why the test(s) can't be parallelized. Purely informational - shows up in code
     * review and IDE tooltips so the constraint doesn't get "optimized away" by accident later.
     */
    String reason() default "";
}
