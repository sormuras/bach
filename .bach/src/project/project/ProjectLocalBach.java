package project;

import run.bach.*;
import run.bach.project.Project;

class ProjectLocalBach extends Bach {
  ProjectLocalBach(Configuration configuration) {
    super(configuration);
  }

  @Override
  protected Browser createBrowser() {
    return super.createBrowser();
  }

  @Override
  protected Locators createLocators() {
    return super.createLocators();
  }

  @Override
  protected Paths createPaths() {
    return super.createPaths();
  }

  @Override
  protected Tools createTools() {
    return super.createTools();
  }

  @Override
  protected Project createProject() {
    return super.createProject()
        .withRequiresModule("org.junit.jupiter")
        .withRequiresModule("org.junit.platform.console");
  }
}
