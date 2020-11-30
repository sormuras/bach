package com.github.sormuras.bach;

import com.github.sormuras.bach.module.ModuleSearcher;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
     * @return the names of the modules to compile, with an empty array for none (effectively
     *     skipping compilation of this space), or the single-element array {@code ["*"]} indicating
     *     to find all matching module compilation units
     */
    String[] modules() default "*";

    /** @return the module paths for main modules */
    String[] modulePaths() default {".bach/libraries"};

    /** @return the Java version (release feature number) to compile main modules for */
    int release() default 0;

    /** @return {@code true} if an API documenation should be generated, else {@code false} */
    boolean generateApiDocumentation() default false;

    /** @return {@code true} if a custom runtime image should be generated, else {@code false} */
    boolean generateCustomRuntimeImage() default false;

    /** @return the additional arguments to be passed on a per-tool basis */
    Tweak[] tweaks() default {
      @Tweak(
          tool = "javac",
          args = {"-encoding", "UTF-8"}),
      @Tweak(
          tool = "jlink",
          args = {"--compress", "2", "--no-header-files", "--no-man-pages", "--strip-debug"})
    };
  }

  /** @return the test module source space configuration */
  Test test() default @Test;

  /** Describes the main module source space. */
  @Target({})
  @interface Test {
    /**
     * @return the names of the modules to compile, with an empty array for none (effectively
     *     skipping compilation of this space), or the single-element array {@code ["*"]} indicating
     *     to find all matching module compilation units
     */
    String[] modules() default "*";

    /** @return the module path patterns for test modules */
    String[] modulePaths() default {".bach/workspace/modules", ".bach/libraries"};

    /** @return the additional arguments to be passed on a per-tool basis */
    Tweak[] tweaks() default {
      @Tweak(
          tool = "javac",
          args = {"-encoding", "UTF-8"})
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

    /** @return an array of module searchers */
    Searcher[] searchers() default {};

    /** Module URI searcher. */
    @Target({})
    @interface Searcher {
      /** @return a class that implements the module searcher inteface */
      Class<? extends ModuleSearcher> with();
      /** @return the version */
      String version();
    }
  }
}
