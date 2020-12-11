package com.github.sormuras.bach.project;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated module is a project declaration.
 *
 * <p>In other words, it transforms an annotated {@code module-info.java} compilation unit into a
 * logical {@code project-info.java} compilation unit. Usually, this compilation unit's path is:
 * {@code .bach/build/module-info.java}.
 *
 * <p>This annotation declares a set of untargeted nested annotations intended solely for use as a
 * member type in complex annotation type declarations. They cannot be used to annotate anything
 * directly.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.MODULE)
public @interface ProjectInfo {

  /** Path to the directory that collects downloaded external modules. */
  String EXTERNAL_MODULES = ".bach/external-modules";

  /** Path to the directory that collects all generated assets. */
  String WORKSPACE = ".bach/workspace";

  /** Path to the directory that collects this project's main modules. */
  String WORKSPACE_MODULES = WORKSPACE + "/modules";

  /** @return the name of the project */
  String name() default "*";

  /** @return the version of the project, defaults to {@code 0-ea} */
  String version() default "0-ea";

  /**
   * An array of paths to module compilation units.
   *
   * @return the {@code module-info.java} files to compile, with an empty array for none
   *     (effectively skipping compilation of the main code space), or the single-element array
   *     {@code ["*"]} indicating to find all matching module compilation units
   */
  String[] modules() default "*";

  /** @return the module path elements for main modules */
  String[] modulePaths() default {EXTERNAL_MODULES};

  /** @return the Java version (release feature number) to compile main modules for */
  int compileModulesForJavaRelease() default 0;

  /** @return the name of the main module, {@code "*"} for auto-detect, an empty string for none */
  String launchCustomRuntimeImageWithModule() default "*";

  /** @return a set of feature flags */
  Feature[] features() default {};

  /** @return the additional arguments to be passed on a per-tool basis */
  Tweak[] tweaks() default {
    @Tweak(tool = "javac", with = {"-encoding", "UTF-8"}),
    @Tweak(tool = "jlink", with = {"--compress", "2"}),
    @Tweak(tool = "jlink", with = "--no-header-files"),
    @Tweak(tool = "jlink", with = "--no-man-pages"),
    @Tweak(tool = "jlink", with = "--strip-debug")
  };

  /** @return the array of required module names */
  String[] requires() default {};

  /** @return an array of external module links */
  Link[] links() default {};

  /** @return an array of module lookup service implementations */
  Class<? extends ModuleLookup>[] lookups() default {};

  /**
   * @return the names of the test modules to compile, with an empty array for none (effectively
   *     skipping compilation of this space), or the single-element array {@code ["*"]} indicating
   *     to find all matching module compilation units
   */
  String[] tests() default "*";

  /** @return the module path patterns for test modules */
  String[] testModulePaths() default {WORKSPACE_MODULES, EXTERNAL_MODULES};

  /** Tool name-args pair annotation. */
  @Target({})
  @interface Tweak {
    /** @return the trigger describing the tool call to be tweaked */
    String tool();

    /** @return the additional arguments to be passed to the tool call */
    String[] with();

    /** @return the code spaces in which this tweak is registered */
    Space[] in() default {Space.MAIN, Space.TEST};

    /** An enumeration of code spaces. */
    enum Space {
      /** Main code space. */
      MAIN,
      /** Test code space. */
      TEST
    }
  }

  /** An external module name to URI pair annotation. */
  @Target({})
  @interface Link {
    /** @return the module name */
    String module();

    /** @return the target of the link, usually resolvable to a remote JAR file */
    String to();

    /** @return the type of the string returned by {@link #to()}, defaults to {@link Type#AUTO} */
    Type type() default Type.AUTO;

    /** @return the repository of Maven-based target coordinates */
    String mavenRepository() default "https://repo.maven.apache.org/maven2";

    /** Link target type. */
    enum Type {
      /** Detect type of link automatically. */
      AUTO,
      /** Uniform Resource Identifier (URI) reference as is {@link java.net.URI}. */
      URI,
      /** Maven-based coordinates. */
      MAVEN
    }
  }
}
