package com.github.sormuras.bach;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Indicates that the annotated module is a project descriptor. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.MODULE)
public @interface ProjectInfo {

  /** The default version {@code 0-ea} as a string. */
  String VERSION_ZERO_EA = "0-ea";

  /** @return the name of the project */
  String name();

  /** @return the version of the project */
  String version() default VERSION_ZERO_EA;

  /** @return the feature number of the Java SE release to compile this project for */
  int java() default 0;

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
