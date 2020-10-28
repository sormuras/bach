package com.github.sormuras.bach.module;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Module-URI pair annotation. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.MODULE)
@Repeatable(Links.class)
public @interface Link {
  /** @return the module name */
  String module();

  /** @return the target of the link, usually resolvable to a remote JAR file */
  String target();

  /** @return the type of the string returned by {@link #target()}, defaults to {@link Type#AUTO} */
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
