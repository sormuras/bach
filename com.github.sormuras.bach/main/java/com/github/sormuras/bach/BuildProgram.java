package com.github.sormuras.bach;

import com.github.sormuras.bach.project.Project;
import java.lang.System.Logger.Level;

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
    bach.debug("Execute default build program");
    var buildModule = new BuildModule(bach);
    var buildProgram = buildModule.findBuildProgram();
    if (buildProgram.isPresent()) {
      var custom = buildProgram.get();
      bach.logbook().log(Level.DEBUG, "Delegate to custom build program: %s", custom);
      custom.build(bach, args);
      return;
    }
    var info = buildModule.findProjectInfo().orElse(Bach.INFO);
    bach.debug("project-info -> %s", info);
    var project = Project.of(info, buildModule.findModuleLookups());
    var builder = new Builder(bach, project);
    builder.build();
  }
}
