package com.github.sormuras.bach;

import com.github.sormuras.bach.project.Project;

/** The {@code BuildProgram} interface should be implemented by custom build programs. */
@FunctionalInterface
public interface BuildProgram {

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
  static void execute(String... args) {
    execute(Bach.ofSystem(), args);
  }

  /**
   * Builds a modular Java project.
   *
   * @param bach the {@code Bach} instance
   * @param args the command line arguments
   */
  static void execute(Bach bach, String... args) {
    var buildModule = new BuildModule();
    var buildProgram = buildModule.findBuildProgram();
    if (buildProgram.isPresent()) {
      buildProgram.get().build(bach, args);
      return;
    }
    var project = buildModule.findProjectInfo().map(Project::of).orElseGet(Project::of);
    var builder = new Builder(bach, project);
    builder.build();
  }
}
