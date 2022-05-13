package com.github.sormuras.bach;

@FunctionalInterface
public interface Configurator {

  static Configurator identity() {
    return project -> project;
  }

  Project configureProject(Project project);

  default ToolFinder configureToolFinder(Paths paths) {
    return ToolFinder.compose(
        ToolFinder.of(getClass().getModule().getLayer()),
        ToolFinder.ofModularTools(paths.externalModules()),
        ToolFinder.ofJavaTools(paths.externalTools()),
        ToolFinder.ofSystemTools(),
        ToolFinder.ofNativeToolsInJavaHome("java"));
  }
}
