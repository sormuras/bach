package com.github.sormuras.bach;

/** Default settings. */
public interface Configurator {

  static Configurator ofDefaults() {
    record DefaultConfigurator() implements Configurator {}
    return new DefaultConfigurator();
  }

  default Project configureProject(Configuration configuration, String syntaxAndPattern) {
    var root = configuration.paths().root();
    var project = Project.ofDefaults().withWalkingDirectory(root, syntaxAndPattern);
    return configureProject(project);
  }

  default Project configureProject(Project project) {
    return project;
  }

  default Flags configureFlags() {
    return Flags.of();
  }

  default ToolCallTweak configureToolCallTweak() {
    return ToolCallTweak.identity();
  }

  default ToolFinder configureToolFinder(ToolFinder projectToolFinder, Paths paths) {
    return ToolFinder.compose(
        projectToolFinder,
        ToolFinder.ofToolsInModulePath(paths.externalModules()),
        ToolFinder.ofJavaTools(paths.externalTools()),
        ToolFinder.ofSystemTools(),
        ToolFinder.ofNativeToolsInJavaHome("java"));
  }
}
