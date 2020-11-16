package com.github.sormuras.bach;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Indicates that the annotated module is a project descriptor.
 *
 * <p>In other words, it elevates an annotated {@code module-info.java} compilation unit into a
 * logical {@code project-info.java} compilation unit.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.MODULE)
public @interface ProjectInfo {

  /** @return the name of the project */
  String name();

  /** @return the version of the project, defaults to {@code 0-ea} */
  String version() default "0-ea";

  /** @return the main module source space configuration */
  Main main() default @Main;

  /** Describes the main module source space. */
  @Target({})
  @interface Main {

    /**
     * @return the names of the modules to compile, with an empty array for none effectively
     *     skipping compilation of this space, or the single-element array {@code ["*"]} indicating
     *     to find all module compilation units via {@link #moduleSourcePaths()} patterns
     */
    String[] modules() default "*";

    /** @return the module source path patterns for main modules */
    String[] moduleSourcePaths() default {"./*/main", "./*/main/java", "./*/main/java-module"};

    /** @return the module paths for main modules */
    String[] modulePaths() default {".bach/libraries"};

    /** @return the Java version (release feature number) to compile main modules for */
    int release() default 0;

    /** @return {@code true} if an API documenation should be generated, else {@code false} */
    boolean generateApiDocumentation() default false;

    /** @return the additional arguments to be passed on a per-tool basis */
    Tweak[] tweaks() default {
      @Tweak(
          tool = "javac",
          args = {"-encoding", "UTF-8"})
    };
  }

  /** @return the test module source space configuration */
  Test test() default @Test;

  /** Describes the main module source space. */
  @Target({})
  @interface Test {
    /**
     * @return the names of the modules to compile, with an empty array for none effectively
     *     skipping compilation of this space, or the single-element array {@code ["*"]} indicating
     *     to find all module compilation units via {@link #moduleSourcePaths()} patterns
     */
    String[] modules() default "*";

    /** @return the module source path patterns for test modules */
    String[] moduleSourcePaths() default {"./*/test", "./*/test/java", "./*/test/java-module"};

    /** @return the module path patterns for test modules */
    String[] modulePaths() default {".bach/workspace/modules", ".bach/libraries"};

    /** @return the additional arguments to be passed on a per-tool basis */
    Tweak[] tweaks() default {
      @Tweak(
          tool = "javac",
          args = {"-encoding", "UTF-8"}),
      @Tweak(
          tool = "junit",
          args = {"--fail-if-no-tests"})
    };
  }

  /** Tool name-args pair annotation. */
  @Target({})
  @interface Tweak {
    /** @return the name of tool to tweak */
    String tool();
    /** @return the additional arguments to be passed to the tool call */
    String[] args();
  }

  /** @return the library configuration */
  Library library() default @Library;

  /** Library configuration annotation. */
  @interface Library {

    /** Placeholder-replacement bindings support for link targets. */
    interface Binding {
      /** @return the default binding */
      static Map<String, String> ofSystem() {
        var os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
        var arch = System.getProperty("os.arch").toLowerCase(Locale.ENGLISH);
        var isWindows = os.contains("win");
        var isMac = os.contains("mac");
        var isLinux = !(isWindows || isMac);
        var is64 = arch.contains("64");

        var map = new TreeMap<String, String>();
        // JavaFX
        map.put("${JAVAFX-PLATFORM}", isLinux ? "linux" : isMac ? "mac" : "win");

        // LWJGL
        map.put("${LWJGL-NATIVES}", "natives-" + (isLinux ? "linux" : isMac ? "macos" : "windows"));
        if (isLinux)
          if (arch.startsWith("arm") || arch.startsWith("aarch64")) {
            var arm64 = is64 || arch.startsWith("armv8");
            map.put("${LWJGL-NATIVES}", arm64 ? "natives-linux-arm64" : "natives-linux-arm32");
          }
        if (isWindows) if (!is64) map.put("${LWJGL-NATIVES}", "natives-windows-x86");

        return map;
      }
    }

    /** @return the array of required module names */
    String[] requires() default {};

    /** @return the array of module links */
    Link[] links() default {};

    /** Module-URI pair annotation. */
    @Target({})
    @interface Link {
      /** @return the module name */
      String module();

      /** @return the target of the link, usually resolvable to a remote JAR file */
      String target();

      /**
       * @return the type of the string returned by {@link #target()}, defaults to {@link Type#AUTO}
       */
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
}
