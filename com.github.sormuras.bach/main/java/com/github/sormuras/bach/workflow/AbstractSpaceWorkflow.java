package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.command.JavacCommand;
import com.github.sormuras.bach.command.ModulePathsOption;
import com.github.sormuras.bach.project.ProjectSpace;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class AbstractSpaceWorkflow extends AbstractProjectWorkflow {

  protected final ProjectSpace space;

  protected AbstractSpaceWorkflow(Bach bach, Project project, ProjectSpace space) {
    super(bach, project);
    this.space = space;
  }

  protected JavacCommand.ReleaseOption computeReleaseOption() {
    return JavacCommand.ReleaseOption.of(space.release());
  }

  protected int computeReleaseVersionFeatureNumber() {
    return space.release() == 0 ? Runtime.version().feature() : space.release();
  }

  protected Path computeOutputDirectoryForModules(ProjectSpace space) {
    return bach.path().workspace(space.name(), "modules");
  }

  protected ModulePathsOption computeModulePathsOption() {
    var paths = ModulePathsOption.empty();
    for (var parent : space.parents()) paths = paths.add(computeOutputDirectoryForModules(parent));
    var externalModules = bach.path().externalModules();
    if (Files.isDirectory(externalModules)) paths = paths.add(externalModules);
    return paths;
  }
}
