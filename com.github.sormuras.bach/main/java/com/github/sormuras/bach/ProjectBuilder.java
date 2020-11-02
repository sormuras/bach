package com.github.sormuras.bach;

/** The {@code ProjectBuilder} interface should be implemented by a custom build program. */
@FunctionalInterface
public interface ProjectBuilder {

  /**
   * Builds a modular Java project.
   *
   * @param bach the calling Bach instance
   * @param args the command line arguments
   */
  void build(Bach bach, String... args);

  /**
   * Builds a modular Java project using the default build program with an Bach instance that is
   * using components configured from system properties.
   *
   * @param args the command line arguments
   */
  static void build(String... args) {
    new ProjectBuildProgram(Bach.ofSystem()).build(args);
  }
}
