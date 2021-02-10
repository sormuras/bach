package com.github.sormuras.bach;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated module is a project declaration.
 *
 * <p>In other words, it transforms an annotated {@code module-info.java} compilation unit into a
 * logical {@code project-info.java} compilation unit.
 *
 * <p>This annotation declares a set of untargeted nested annotations intended solely for use as a
 * member type in complex annotation type declarations. They cannot be used to annotate anything
 * directly.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.MODULE)
public @interface ProjectInfo {
  /** @return the name of the project, defaults to {@code "*"} */
  String name() default "*";

  /** @return the version of the project, defaults to {@code "0"} */
  String version() default "0";
}
