package com.github.sormuras.bach;

import com.github.sormuras.bach.lookup.JUnit;
import com.github.sormuras.bach.project.JavaStyle;
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

  /** Default name of the module that is usually annotated with {@code @ProjectInfo}. */
  String MODULE = "bach.info";

  /**
   * {@return the name of the project, defaults to {@code "*"}}
   *
   * @see Project#name()
   * @see Bach#computeProjectName(ProjectInfo, String)
   */
  String name() default "*";

  /**
   * {@return the version of the project, defaults to {@code "0"}}
   *
   * @see Project#version()
   * @see Bach#computeProjectVersion(ProjectInfo)
   * @see java.lang.module.ModuleDescriptor.Version
   */
  String version() default "0";

  /**
   * {@return the style to format Java source-code files with, defaults to {@link JavaStyle#FREE}}
   *
   * @see Project#spaces()
   * @see com.github.sormuras.bach.api.ProjectBuilderAPI#computeProjectSpaces(ProjectInfo,
   *     com.github.sormuras.bach.project.Settings, com.github.sormuras.bach.project.Libraries)
   */
  JavaStyle format() default JavaStyle.FREE;

  /**
   * {@return an array of external modules on which the project has a dependence}
   *
   * @see com.github.sormuras.bach.project.Libraries#requires()
   * @see com.github.sormuras.bach.api.ProjectBuilderAPI#computeProjectLibraries(ProjectInfo,
   *     com.github.sormuras.bach.project.Settings)
   * @see java.lang.module.ModuleDescriptor.Requires
   */
  String[] requires() default {};

  /**
   * {@return an array of external module lookup annotations}
   *
   * @see com.github.sormuras.bach.project.Libraries#find(String)
   * @see com.github.sormuras.bach.lookup.ModuleLookup#lookupUri(String)
   * @see com.github.sormuras.bach.api.ProjectBuilderAPI#computeProjectLibraries(ProjectInfo,
   *     com.github.sormuras.bach.project.Settings)
   */
  External[] lookup() default {};

  /**
   * An external module name to URI pair annotation.
   *
   * @see com.github.sormuras.bach.lookup.ExternalModuleLookup
   * @see Bach#computeProjectModuleLookup(External)
   */
  @Target({})
  @interface External {
    /** {@return the module name} */
    String module();

    /** {@return the target of the lookup, usually resolvable to a remote JAR file} */
    String via();

    /** {@return the type of the {@link #via()} target string, defaults to {@link Type#AUTO}} */
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

  /**
   * {@return the version constant of the {@link JUnit} enumeration used to lookup external
   * JUnit-related modules}
   *
   * <p>The default version constant is set to a stable version of JUnit, usually the "Latest
   * Release" as displayed on the <a href="https://junit.org/junit5/">JUnit 5 website</a>. Both
   * versions may change without further announcements. Hence it is advisable to return a specific
   * {@link JUnit} version constant in a project descriptors as follows:
   *
   * <pre>{@code
   * @ProjectInfo(lookupJUnit = JUnit.V_5_7_0)
   * module bach.info {
   *   requires com.github.sormuras.bach;
   * }
   * }</pre>
   *
   * <p>Returning a version constant like {@code JUnit.V_5_7_0} here is similar to
   *
   * <pre>{@code
   * @Override
   * public Libraries computeProjectLibraries(ProjectInfo info, Settings settings) {
   *   return super.computeProjectLibraries(info, settings).withModuleLookup(JUnit.V_5_7_0);
   * }
   * }</pre>
   *
   * @see com.github.sormuras.bach.lookup.ModuleLookup
   */
  JUnit lookupJUnit() default JUnit.V_5_7_1;
}
