package com.github.sormuras.bach;

import com.github.sormuras.bach.project.DeclaredModule;
import java.nio.file.Files;
import java.nio.file.Path;

/** Default settings. */
@FunctionalInterface
public interface Configurator {

  static Configurator identity() {
    return project -> project;
  }

  default Project configureProject(Paths paths, String syntaxAndPattern) {
    var project = Project.ofDefaults();
    project = withWalkingDirectory(project, paths.root(), syntaxAndPattern);
    return configureProject(project);
  }

  Project configureProject(Project project);

  default Flags configureFlags() {
    return Flags.of();
  }

  default ToolCallTweak configureToolCallTweak() {
    return ToolCallTweak.identity();
  }

  default ToolFinder configureToolFinder(Paths paths) {
    return ToolFinder.compose(
        ToolFinder.ofToolsInModuleLayer(getClass().getModule()),
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
  static Project withWalkingDirectory(Project project, Path directory, String syntaxAndPattern) {
    var name = directory.normalize().toAbsolutePath().getFileName();
    if (name != null) project = project.withName(name.toString());
    var matcher = directory.getFileSystem().getPathMatcher(syntaxAndPattern);
    try (var stream = Files.find(directory, 9, (p, a) -> matcher.matches(p))) {
      for (var path : stream.toList()) {
        var uri = path.toUri().toString();
        if (uri.contains("/.bach/")) continue; // exclude project-local modules
        if (uri.matches(".*?/java-\\d+.*")) continue; // exclude non-base modules
        var module = DeclaredModule.of(path);
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
