package com.github.sormuras.bach;

import com.github.sormuras.bach.lookup.ExternalModuleLookup;
import com.github.sormuras.bach.lookup.ModuleLookup;
import com.github.sormuras.bach.project.JavaStyle;
import com.github.sormuras.bach.project.Libraries;
import com.github.sormuras.bach.project.Settings;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.module.ModuleDescriptor;

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

  /** Path to the directory that collects external modules. */
  String EXTERNAL_MODULES = ".bach/external-modules";

  /** Path to the directory that collects all generated assets. */
  String WORKSPACE = ".bach/workspace";

  /** Path to the directory that collects this project's main modules. */
  String MAIN_MODULES = WORKSPACE + "/modules";

  /** Default name of the module that is usually annotated with {@code @ProjectInfo}. */
  String MODULE = "bach.info";

  /**
   * {@return the name of the project, defaults to {@code "*"}}
   *
   * @see Bach#computeProjectName(ProjectInfo, String)
   * @see Project#name()
   */
  String name() default "*";

  /**
   * {@return the version of the project, defaults to {@code "0"}}
   *
   * @see Bach#computeProjectVersion(ProjectInfo)
   * @see Project#version()
   * @see ModuleDescriptor.Version
   */
  String version() default "0";

  /**
   * {@return the style to format Java source-code files with, defaults to {@link JavaStyle#FREE}}
   *
   * @see Bach#computeProjectSpaces(ProjectInfo, Settings, Libraries)
   * @see Project#spaces()
   */
  JavaStyle format() default JavaStyle.FREE;

  /** {@return the names of the main modules to compile} */
  String[] modules() default "*";

  /** {@return the module path elements for main modules} */
  String[] modulePaths() default {EXTERNAL_MODULES};

  /**
   * {@return the Java version (release feature number) to compile main modules for}
   *
   * <p>For a value of {@code 0}, the default value, the {@code --release} option is omitted.
   */
  int compileModulesForJavaRelease() default 0;

  /**
   * {@return an array of external modules on which the project has a dependence}
   *
   * @see Bach#computeProjectLibraries(ProjectInfo, Settings)
   * @see Libraries#requires()
   * @see ModuleDescriptor.Requires
   */
  String[] requires() default {};

  /**
   * {@return an array of external module lookup annotations}
   *
   * @see Bach#computeProjectLibraries(ProjectInfo, Settings)
   * @see Libraries#find(String)
   * @see ModuleLookup#lookupUri(String)
   */
  External[] lookupExternal() default {};

  /**
   * An external module name to URI pair annotation.
   *
   * @see Bach#computeProjectModuleLookup(External)
   * @see ExternalModuleLookup
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
      /** Maven-based coordinates. */
      MAVEN
    }
  }

  /**
   * {@return the version constant of the }
   *
   * @see Bach#computeProjectLibraries(ProjectInfo, Settings)
   * @see ModuleLookup
   */
  Externals[] lookupExternals() default {};

  /** An external source of modules that lookup by a built-in module lookup implementation. */
  @Target({})
  @interface Externals {

    /** The name of the module lookup, usually a name of a modular library or a framework. */
    Name name();

    /** The version of the library or framework to lookup. */
    String version();

    /** The name of the module lookup, usually a name of a modular library or a framework. */
    enum Name {
      /** Look up module URIs for modules uploaded to their GitHub Releases environment. */
      GITHUB_RELEASES,
      /** Open JavaFX, requires a version argument like {@code "15.0.1"}. */
      JAVAFX,
      /** JUnit 5 and related modules, requires a version like {@code "5.7.1"}. */
      JUNIT,
      /** Lightweight Java Gaming Library, requires a version like {@code "3.2.3"}. */
      LWJGL,
      /**
       * Look up unique modules published at Maven Central.
       *
       * @see <a href="https://github.com/sormuras/modules">sormuras/modules</a>
       */
      SORMURAS_MODULES
    }
  }

  Metadata[] metadata() default {};

  /** An external module metadata configuration annotation. */
  @Target({})
  @interface Metadata {
    String module();

    long size();

    Checksum[] checksums() default {};

    @Target({})
    @interface Checksum {
      String algorithm() default "MD5";

      String value();
    }
  }

  /** {@return the additional main space arguments to be passed on a per-tool basis} */
  Tweak[] tweaks() default {
    @Tweak(tool = "javac", option = "-encoding", value = "UTF-8"),
    @Tweak(tool = "javadoc", option = "-encoding", value = "UTF-8"),
    @Tweak(tool = "jlink", option = "--compress", value = "2"),
    @Tweak(tool = "jlink", option = "--no-header-files"),
    @Tweak(tool = "jlink", option = "--no-man-pages"),
    @Tweak(tool = "jlink", option = "--strip-debug"),
  };

  /** {@return the names of the test modules to compile} */
  String[] testModules() default "*";

  /** {@return the module path elements for compiling and running test modules} */
  String[] testModulePaths() default {MAIN_MODULES, EXTERNAL_MODULES};

  /** {@return the additional test space arguments to be passed on a per-tool basis} */
  Tweak[] testTweaks() default {
    @Tweak(tool = "javac", option = "-encoding", value = "UTF-8"),
  };

  /** Tool name-args pair annotation. */
  @Target({})
  @interface Tweak {
    /** {@return the trigger describing the tool call to be tweaked} */
    String tool();

    /** {@return } */
    String option();

    /** {@return the additional arguments to be passed to the tool call} */
    String[] value() default {};
  }
}
