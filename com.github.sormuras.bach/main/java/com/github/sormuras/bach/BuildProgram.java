package com.github.sormuras.bach;

import com.github.sormuras.bach.project.Base;

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
  static void build(String... args) {
    build(Bach.ofSystem(), Base.ofCurrentDirectory(), args);
  }

  /**
   * Builds a modular Java project.
   *
   * @param bach the {@code Bach} instance
   * @param base the base paths object
   * @param args the command line arguments
   */
  static void build(Bach bach, Base base, String... args) {
    var buildModule = BuildModule.of(base.directory());
    var buildProgram = buildModule.findBuildProgram();
    if (buildProgram.isPresent()) {
      buildProgram.get().build(bach, args);
      return;
    }
    var projectInfo = buildModule.findProjectInfo();
    if (projectInfo.isPresent()) {
      new ProjectBuilder(bach, Project.of(base, projectInfo.get())).build();
      return;
    }
    new ProjectBuilder(bach, Project.of(base)).build();
  }
}
