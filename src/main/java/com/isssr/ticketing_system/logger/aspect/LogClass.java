package com.isssr.ticketing_system.logger.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation log a class
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
//use as @LogClass(logAttrs={"attribute1", "attribute2"})
public @interface LogClass {
    String[] logAttrs() default "";

    String[] idAttrs() default "";
}
