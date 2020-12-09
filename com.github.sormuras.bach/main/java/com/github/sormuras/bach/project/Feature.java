package com.github.sormuras.bach.project;

/** A feature flag. */
public enum Feature {
  /** Call {@code javadoc} to generate HTML pages of API documentation from main source files. */
  GENERATE_API_DOCUMENTATION,

  /** Call {@code jlink} to assemble main modules and their dependencies into a runtime image. */
  GENERATE_CUSTOM_RUNTIME_IMAGE,

  /** Generate Maven-based consumer POM files. */
  GENERATE_MAVEN_POM_FILES,

  /** Include {@code *.java} files alongside with {@code *.class} files into each modular JAR. */
  INCLUDE_SOURCES_IN_MODULAR_JAR,
}
