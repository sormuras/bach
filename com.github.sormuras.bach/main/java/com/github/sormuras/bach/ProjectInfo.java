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

  /**
   * {@return the name of the project, defaults to {@code "*"}}
   *
   * @see Project#name()
   * @see ProjectBuilder#computeName()
   */
  String name() default "*";

  /**
   * {@return the version of the project, defaults to {@code "0"}}
   *
   * @see Project#version()
   */
  String version() default "0";

  /** {@return the array of required module names} */
  String[] requires() default {};

  /** {@return an array of external module lookup} */
  Lookup[] lookups() default {};

  /** An external module name to URI pair annotation. */
  @Target({})
  @interface Lookup {
    /** {@return the module name} */
    String module();

    /** {@return the target of the lookup, usually resolvable to a remote JAR file} */
    String via();

    /** {@return the type of the string returned by {@link #via()}, defaults to {@link Type#AUTO}} */
    Type type() default Type.AUTO;

    /** {@return the base path of path targets} */
    String pathBase() default ".";

    /** {@return the base URI of the repository for Maven-based target coordinates} */
    String mavenRepository() default "https://repo.maven.apache.org/maven2";

    /** Lookup target type. */
    enum Type {
      /** Detect type of the lookup target automatically. */
      AUTO,
      /** Uniform Resource Identifier ({@link java.net.URI URI}) reference as-is. */
      URI,
      /** Path to a local file. */
      PATH,
      /** Maven-based coordinates. */
      MAVEN
    }
  }
}
