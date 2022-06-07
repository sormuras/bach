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

  default ToolFinder configureToolFinder(Paths paths) {
    var module = getClass().getModule();
    return ToolFinder.compose(
        module == Configurator.class.getModule()
            ? ToolFinder.of(/* no project-local tools */ )
            : ToolFinder.ofToolsInModuleLayer(module),
        ToolFinder.ofToolsInModulePath(paths.externalModules()),
        ToolFinder.ofJavaTools(paths.externalTools()),
        ToolFinder.ofSystemTools(),
        ToolFinder.ofNativeToolsInJavaHome("java"));
  }
}
