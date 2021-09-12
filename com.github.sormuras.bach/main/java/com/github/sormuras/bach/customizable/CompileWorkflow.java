package com.github.sormuras.bach.customizable;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Command;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.command.JavacCommand;
import com.github.sormuras.bach.project.ProjectSpace;
import java.nio.file.Path;

/** Compiles and archives Java source files. */
public class CompileWorkflow extends Workflow {

  protected final ProjectSpace space;

  public CompileWorkflow(Bach bach, Project project, ProjectSpace space) {
    super(bach, project);
    this.space = space;
  }

  protected JavacCommand computeJavacCommand(Path classes) {
    var computedModuleSourcePath = ModuleSourcePathComputer.compute(space);
    return Command.javac()
        .modules(space.modules().names())
        .option(computedModuleSourcePath.patterns())
        .option(computedModuleSourcePath.specifics())
        .outputDirectoryForClasses(classes);
  }

  protected Path computeOutputDirectoryForClasses() {
    return bach.path().workspace(space.name(), "classes", "" + computeRelease());
  }

  protected Path computeOutputDirectoryForModules() {
    return bach.path().workspace(space.name(), "modules");
  }

  protected int computeRelease() {
    return /*space.release().map(JavaRelease::feature).orElse*/ Runtime.version().feature();
  }

  @Override
  public void run() {
    if (space.modules().values().isEmpty()) {
      bach.logMessage("No %s module present".formatted(space.name()));
      return;
    }
    var size = space.modules().values().size();
    bach.logCaption("Compile %d %s module%s".formatted(size, space.name(), size == 1 ? "" : "s"));

    var classes = computeOutputDirectoryForClasses();
    bach.run(computeJavacCommand(classes));
  }
}
