package com.github.sormuras.bach;

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
     * @return the names of the modules to compile, with an empty array for none effectively
     *     skipping compilation of this space, or the single-element array {@code ["*"]} indicating
     *     to find all module compilation units via {@link #moduleSourcePaths()} patterns
     */
    String[] modules() default "*";

    /** @return the module source paths */
    String[] moduleSourcePaths() default {"*/main/java", "*/main/java-module"};

    /** @return the Java version (release feature number) to compile for */
    int release() default 0;

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
