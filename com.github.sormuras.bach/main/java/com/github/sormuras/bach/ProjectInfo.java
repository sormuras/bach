package com.github.sormuras.bach;

import com.github.sormuras.bach.project.CodeStyle;
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

  /** Path to the directory that collects external modules. */
  String EXTERNAL_MODULES = ".bach/external-modules";

  /** Path to the directory that collect external tools. */
  String EXTERNAL_TOOLS = ".bach/external-tools";

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
   * @see java.lang.module.ModuleDescriptor.Version
   */
  String version() default "0";

  /** {@return libraries configuration} */
  Libraries libraries() default @Libraries;

  /** {@return main code space configuration} */
  MainSpace main() default @MainSpace;

  /** {@return options configuration} */
  Options options() default @Options;

  /** {@return test code space configuration} */
  TestSpace test() default @TestSpace;

  /** A checksum configuration. */
  @Target({})
  @interface Checksum {
    /** {@return the name of the message digest algorithm, defaults to {@code MD5}} */
    String algorithm() default "MD5";

    /** {@return the expected checksum} */
    String value();
  }

  /** An external source of modules that lookup by a built-in module lookup implementation. */
  @Target({})
  @interface ExternalLibrary {

    /** The name of the module lookup, usually a name of a modular library or a framework. */
    LibraryName name();

    /** The version of the library or framework to lookup. */
    String version();
  }

  /** The name of the module lookup, usually a name of a modular library or a framework. */
  enum LibraryName {
    /** Java/JavaFX/Kotlin Game Library, requires a version argument like {@code "11.14"}. */
    FXGL,
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

  /**
   * An external module name to URI pair annotation.
   *
   * @see com.github.sormuras.bach.lookup.ExternalModuleLookup
   */
  @Target({})
  @interface ExternalModule {
    /** {@return the module name} */
    String name();

    /** {@return the target of the lookup, usually resolvable to a remote JAR file} */
    String link();

    /** {@return the type of the {@link #link()} target string, defaults to {@link LinkType#AUTO}} */
    LinkType type() default LinkType.AUTO;

    /** {@return the base URI of the repository for Maven-based target coordinates} */
    String mavenRepository() default "https://repo.maven.apache.org/maven2";
  }

  /** An external module link target type. */
  enum LinkType {
    /** Detect type of the lookup target automatically. */
    AUTO,
    /** Uniform Resource Identifier ({@link java.net.URI URI}) reference as-is. */
    URI,
    /** Maven-based coordinates. */
    MAVEN
  }

  /**
   * Module launcher configuration.
   *
   * @see com.github.sormuras.bach.project.ModuleLauncher
   */
  @Target({})
  @interface Launcher {
    /** {@return command name, or {@code "*"} for using the name of the project} */
    String command() default "*";
    /** {@return module name of a main module} */
    String module();
    /** {@return possibly empty name of the main class} */
    String mainClass() default "";
  }

  /**
   * Libraries configuration.
   *
   * @see com.github.sormuras.bach.project.Libraries
   */
  @Target({})
  @interface Libraries {

    /**
     * {@return an array of external modules on which the project has a dependence}
     *
     * @see Libraries#requires()
     * @see java.lang.module.ModuleDescriptor.Requires
     */
    String[] requires() default {};

    /**
     * {@return an array of external module lookup annotations}
     *
     * @see com.github.sormuras.bach.lookup.ModuleLookup#lookupUri(String)
     */
    ExternalModule[] externalModules() default {};

    /**
     * {@return the version constant of the }
     *
     * @see com.github.sormuras.bach.lookup.ModuleLookup
     */
    ExternalLibrary[] externalLibraries() default {};

    Metadata[] metadata() default {};
  }

  /** Main space configuration. */
  @Target({})
  @interface MainSpace {

    /**
     * {@return an array of path matcher patterns for finding main module declarations}
     *
     * @see java.nio.file.FileSystem#getPathMatcher(String)
     */
    String[] modules() default "**module-info.java";

    /** {@return the module path elements for main modules} */
    String[] modulePaths() default {EXTERNAL_MODULES};

    /** {@return the additional main space arguments to be passed on a per-tool basis} */
    Tweak[] tweaks() default {
      @Tweak(tool = "javac", option = "-encoding", value = "UTF-8"),
      @Tweak(tool = "javadoc", option = "-encoding", value = "UTF-8"),
      @Tweak(tool = "jlink", option = "--compress", value = "2"),
      @Tweak(tool = "jlink", option = "--no-header-files"),
      @Tweak(tool = "jlink", option = "--no-man-pages"),
      @Tweak(tool = "jlink", option = "--strip-debug"),
    };
  }

  /** An external module metadata configuration annotation. */
  @Target({})
  @interface Metadata {
    /** {@return the name of the module} */
    String module();

    /** {@return the size of the modular JAR file in bytes} */
    long size();

    /** {@return an array of checksums} */
    Checksum[] checksums() default {};
  }

  /** An options configuration. */
  @Target({})
  @interface Options {
    /**
     * {@return the Java version (release feature number) to compile main modules for}
     *
     * <p>For a value of {@code 0}, the default value, the {@code --release} option is omitted.
     *
     * @see com.github.sormuras.bach.project.Property#PROJECT_TARGETS_JAVA
     */
    int compileModulesForJavaRelease() default 0;

    /**
     * {@return the style to format Java source-code files with, defaults to {@link CodeStyle#FREE}}
     *
     * @see Project#spaces()
     */
    CodeStyle formatSourceFilesWithCodeStyle() default CodeStyle.FREE;

    /**
     * {@return {@code true} in order to include all files found in source folders into their
     * modules}
     *
     * @see com.github.sormuras.bach.project.Flag#JAR_WITH_SOURCES
     */
    boolean includeSourceFilesIntoModules() default false;

    /**
     * {@return the launcher option value for the custom runtime image generated by {@code jlink}}
     *
     * <p>Defaults to {@code "*"} which triggers auto-detection of the value based on this project's
     * name and a unique main module providing a main class.
     */
    Launcher launcher() default @Launcher(module = "*");

    /** {@return the tools-releated settings} */
    Tools tools() default @Tools;
  }

  /** Test space configuration. */
  @Target({})
  @interface TestSpace {

    /**
     * {@return an array of path matcher patterns for finding test module declarations}
     *
     * @see java.nio.file.FileSystem#getPathMatcher(String)
     */
    String[] modules() default "**/test/**";

    /** {@return the module path elements for compiling and running test modules} */
    String[] modulePaths() default {MAIN_MODULES, EXTERNAL_MODULES};

    /** {@return the additional test space arguments to be passed on a per-tool basis} */
    Tweak[] tweaks() default {
      @Tweak(tool = "javac", option = "-encoding", value = "UTF-8"),
    };
  }

  /** Tools-related settings. */
  @Target({})
  @interface Tools {
    /** {@return limit the universe of executable tools} */
    String[] limit() default {};
    /** {@return list of tools to skip} */
    String[] skip() default {};
  }

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
