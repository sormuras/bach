package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.project.DeclaredModules;
import com.github.sormuras.bach.project.ProjectSpace;
import java.util.List;

public class GrabExternalsWorkflow extends AbstractProjectWorkflow {

  public GrabExternalsWorkflow(Bach bach, Project project) {
    super(bach, project);
  }

  public List<String> computeMissingRequiredExternalModules() {
    var explorer = bach.explorer();
    return explorer.listMissingExternalModules(
        project.spaces().values().stream()
            .map(ProjectSpace::modules)
            .map(DeclaredModules::toModuleFinder)
            .toList(),
        project.externals().requires());
  }

  @Override
  public void run() {
    var missingRequiredExternalModules = computeMissingRequiredExternalModules();
    if (missingRequiredExternalModules.isEmpty()) return;

    var grabber = bach.grabber(project.externals().locators());
    grabber.grabExternalModules(missingRequiredExternalModules);
    grabber.grabMissingExternalModules();
  }
}
