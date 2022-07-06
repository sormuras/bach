package com.github.sormuras.bach;

import com.github.sormuras.bach.project.DeclaredFolders;
import com.github.sormuras.bach.project.DeclaredModule;
import com.github.sormuras.bach.project.ExternalModuleLocator;
import com.github.sormuras.bach.project.ProjectComponent;
import com.github.sormuras.bach.project.ProjectExternals;
import com.github.sormuras.bach.project.ProjectName;
import com.github.sormuras.bach.project.ProjectSpace;
import com.github.sormuras.bach.project.ProjectSpaces;
import com.github.sormuras.bach.project.ProjectVersion;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

/** Modular project model. */
public record Project(
    ProjectName name, ProjectVersion version, ProjectSpaces spaces, ProjectExternals externals) {

  /** {@return an {@code "unnamed 0-ea"} project with empty init, main, and test module spaces} */
  public static Project ofDefaults() {
    var name = new ProjectName("unnamed");
    var version = new ProjectVersion("0-ea", ZonedDateTime.now());
    var init = new ProjectSpace("init");
    var main = new ProjectSpace("main", "init");
    var test = new ProjectSpace("test", "main");
    var spaces = new ProjectSpaces(init, main, test);
    var externals = ProjectExternals.of();
    return new Project(name, version, spaces, externals);
  }

  @SuppressWarnings("PatternVariableHidesField")
  private Project with(ProjectComponent component) {
    return new Project(
        component instanceof ProjectName name ? name : name,
        component instanceof ProjectVersion version ? version : version,
        component instanceof ProjectSpaces spaces ? spaces : spaces,
        component instanceof ProjectExternals externals ? externals : externals);
  }

  public Project with(ProjectSpace space) {
    return with(spaces.with(space));
  }

  /** {@return new project instance with an additional external module locator} */
  public Project with(ExternalModuleLocator locator) {
    return with(externals.with(locator));
  }

  /** {@return new project instance using the given string as the project name} */
  public Project withName(String string) {
    return with(new ProjectName(string));
  }

  /** {@return new project instance using the given string as the project version} */
  public Project withVersion(String string) {
    return with(version.with(string));
  }

  /** {@return new project instance using the given string as the project version date} */
  public Project withVersionDate(String string) {
    return with(version.withDate(string));
  }

  /**
   * {@return new project instance setting main space's Java release feature number to the integer
   * value of the given string}
   */
  public Project withTargetsJava(String string) {
    return withTargetsJava(Integer.parseInt(string));
  }

  /** {@return new project instance setting main space's Java release feature number} */
  public Project withTargetsJava(int release) {
    return withTargetsJava("main", release);
  }

  /** {@return new project instance setting specified space's Java release feature number} */
  public Project withTargetsJava(String space, int release) {
    return withTargetsJava(spaces.space(space), release);
  }

  /** {@return new project instance setting specified space's Java release feature number} */
  public Project withTargetsJava(ProjectSpace space, int release) {
    return with(space.withTargetsJava(release));
  }

  /** {@return new project instance setting specified space's release argument and preview flag} */
  public Project withEnablePreviewFeatures(String space) {
    return withTargetsJava(space, Runtime.version().feature())
        .withTweak(
            space,
            "com.github.sormuras.bach/compile-classes::javac",
            javac -> javac.with("--enable-preview"));
  }

  /** {@return new project instance setting main space's launcher} */
  public Project withLauncher(String launcher) {
    return withLauncher("main", launcher);
  }

  /** {@return new project instance setting specified space's launcher} */
  public Project withLauncher(String space, String launcher) {
    return withLauncher(spaces.space(space), launcher);
  }

  /** {@return new project instance setting specified space's launcher} */
  public Project withLauncher(ProjectSpace space, String launcher) {
    return with(space.withLauncher(launcher));
  }

  public Project withModule(String module) {
    return withModule("main", module);
  }

  public Project withModule(String space, String module) {
    return withModule(space, ModuleDescriptor.newModule(module).build());
  }

  public Project withModule(String space, ModuleDescriptor module) {
    return withModule(
        spaces.space(space),
        new DeclaredModule(
            Path.of(module.name()),
            Path.of(module.name(), "src", space, "java", "module-info.java"),
            module,
            DeclaredFolders.of(Path.of(module.name(), "src", space, "java")),
            Map.of()));
  }

  /** {@return new project instance with a new module declaration added to the specified space} */
  public Project withModule(String space, Path root, String info) {
    return withModule(spaces.space(space), DeclaredModule.of(root, Path.of(info)));
  }

  /** {@return new project instance with a new module declaration added to the specified space} */
  public Project withModule(ProjectSpace space, DeclaredModule module) {
    return with(spaces.with(space.withModules(space.modules().with(module))));
  }

  /** {@return new project instance with one or more additional modular dependences} */
  public Project withRequiresModule(String name, String... more) {
    return with(externals.withRequires(name).withRequires(more));
  }

  /** {@return new project instance with an additional external module locator} */
  public Project withExternalModule(String name, String from) {
    var locator = new ExternalModuleLocator.SingleExternalModuleLocator(name.strip(), from.strip());
    return with(locator);
  }

  /** {@return new project instance with an additional external tool} */
  public Project withExternalTool(String name) {
    return with(externals.withExternalTool(name));
  }

  /** {@return new project instance with an additional tool call tweak for the main space} */
  public Project withTweak(String id, ToolCallTweak tweak) {
    return withTweak("main", id, tweak);
  }

  /** {@return new project instance with an additional tool call tweak for the specified space} */
  public Project withTweak(String space, String id, ToolCallTweak tweak) {
    return withTweak(spaces.space(space), id, tweak);
  }

  /** {@return new project instance with an additional tool call tweak for the specified space} */
  public Project withTweak(ProjectSpace space, String id, ToolCallTweak tweak) {
    return with(spaces.with(space.withTweak(id, tweak)));
  }

  /**
   * {@return new project instance configured by finding {@code module-info.java} files below the
   * specified root directory matching the given {@link
   * java.nio.file.FileSystem#getPathMatcher(String) syntaxAndPattern}}
   */
  public Project withWalkingDirectory(Path directory, String syntaxAndPattern) {
    var project = this;
    var name = directory.normalize().toAbsolutePath().getFileName();
    if (name != null) project = project.withName(name.toString());
    var externalModules = directory.resolve(".bach/external-modules");
    if (Files.isDirectory(externalModules)) {
      var matcher = externalModules.getFileSystem().getPathMatcher("glob:**.properties");
      try (var stream = Files.find(externalModules, 2, (p, a) -> matcher.matches(p))) {
        for (var path : stream.toList()) {
          var file = path.getFileName().toString();
          if (file.indexOf('_') >= 0) continue;
          project = project.with(ExternalModuleLocator.PropertiesBasedModuleLocator.of(path));
        }
      } catch (Exception exception) {
        throw new RuntimeException("Find files in %s failed".formatted(externalModules), exception);
      }
    }
    var externalTools = directory.resolve(".bach/external-tools");
    if (Files.isDirectory(externalTools)) {
      try (var stream = Files.newDirectoryStream(externalTools, "*.properties")) {
        for (var path : stream) {
          var tool = path.getFileName().toString().replace(".properties", "");
          project = project.withExternalTool(tool);
        }
      } catch (Exception exception) {
        throw new RuntimeException("Stream files in %s failed".formatted(externalTools), exception);
      }
    }
    var matcher = directory.getFileSystem().getPathMatcher(syntaxAndPattern);
    try (var stream = Files.find(directory, 9, (p, a) -> matcher.matches(p))) {
      for (var path : stream.toList()) {
        var uri = path.toUri().toString();
        if (uri.contains("/.bach/")) continue; // exclude project-local modules
        if (uri.matches(".*?/java-\\d+.*")) continue; // exclude non-base modules
        var module = DeclaredModule.of(directory, path);
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

  /** {@return a list of all modules declared by this project} */
  public List<DeclaredModule> modules() {
    return spaces.list().stream().flatMap(space -> space.modules().list().stream()).toList();
  }

  public void initializeInCurrentWorkingDirectory() {
    initializeInDirectory("");
  }

  public void initializeInDirectory(String root) {
    initializeInDirectory(Path.of(root));
  }

  public void initializeInDirectory(Path root) {
    if (modules().isEmpty()) throw new IllegalStateException("No module declared");
    try {
      var configurator = root.resolve(".bach/project-info/project/ProjectConfigurator.java");
      Files.createDirectories(configurator.getParent());
      Files.writeString(
          configurator,
          // language=java
          """
          package project;

          import com.github.sormuras.bach.Configurator;
          import com.github.sormuras.bach.Project;

          public class ProjectConfigurator implements Configurator {
            @Override
            public Project configureProject(Project project) {
              return project
                  .withName("%s")
                  .withVersion("%s");
            }
          }
          """
              .formatted(name.value(), version.value()));
      Files.writeString(
          root.resolve(".bach/project-info/module-info.java"),
          // language=java
          """
          module project {
            requires com.github.sormuras.bach;
            provides com.github.sormuras.bach.Configurator with
                project.ProjectConfigurator;
          }
          """);
      for (var space : spaces.list())
        for (var module : space.modules().list()) {
          var info = root.resolve(module.info());
          Files.createDirectories(info.getParent());
          Files.writeString(
              info,
              """
              module %s {
              // TODO exports
              // TODO requires
              }
              """
                  .formatted(module.name()));
        }
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }
}
