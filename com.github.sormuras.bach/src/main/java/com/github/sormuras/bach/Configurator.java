package com.github.sormuras.bach;

import com.github.sormuras.bach.project.DeclaredModule;
import java.nio.file.Files;

/** Default settings. */
public interface Configurator {

  static Configurator ofDefaults() {
    record DefaultConfigurator() implements Configurator {}
    return new DefaultConfigurator();
  }

  default Project configureProject(Configuration configuration, String syntaxAndPattern) {
    var project = Project.ofDefaults();
    project = withWalkingDirectory(configuration, syntaxAndPattern, project);
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

  /**
   * {@return new project instance configured by finding {@code module-info.java} files matching the
   * given {@link java.nio.file.FileSystem#getPathMatcher(String) syntaxAndPattern} below the
   * specified root directory}
   */
  static Project withWalkingDirectory(
      Configuration configuration, String syntaxAndPattern, Project project) {
    var verbose = configuration.isVerbose();
    var printer = configuration.printer();
    var directory = configuration.paths().root();
    if (verbose) {
      printer.out("Walking Directory");
      printer.out("%20s = %s".formatted("directory", directory));
      printer.out("%20s = %s".formatted("syntaxAndPattern", syntaxAndPattern));
    }
    var name = directory.normalize().toAbsolutePath().getFileName();
    if (name != null) project = project.withName(name.toString());
    var matcher = directory.getFileSystem().getPathMatcher(syntaxAndPattern);
    try (var stream = Files.find(directory, 9, (p, a) -> matcher.matches(p))) {
      for (var path : stream.toList()) {
        var uri = path.toUri().toString();
        if (uri.contains("/.bach/")) continue; // exclude project-local modules
        if (uri.matches(".*?/java-\\d+.*")) continue; // exclude non-base modules
        var module = DeclaredModule.of(directory, path);
        if (verbose) {
          printer.out("%20s = %s -> %s".formatted("path", path, module.name()));
        }
        if (uri.contains("/init/")) {
          project = project.withModule(project.spaces().init(), module);
          continue;
        }
        if (uri.contains("/test/")) {
          project = project.withModule(project.spaces().test(), module);
          continue;
        }
        project = project.withModule(project.spaces().main(), module);
      }
    } catch (Exception exception) {
      throw new RuntimeException("Find with %s failed".formatted(syntaxAndPattern), exception);
    }
    return project;
  }
}
