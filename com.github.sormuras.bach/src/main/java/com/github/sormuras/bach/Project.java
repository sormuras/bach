package com.github.sormuras.bach;

import com.github.sormuras.bach.project.DeclaredFolders;
import com.github.sormuras.bach.project.DeclaredModule;
import com.github.sormuras.bach.project.ExternalModuleLocator;
import com.github.sormuras.bach.project.ExternalTool;
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
import java.util.Formattable;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Modular project model. */
public record Project(
    ProjectName name, ProjectVersion version, ProjectSpaces spaces, ProjectExternals externals)
    implements Formattable {

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

  /** {@return new project instance with an additional external tool} */
  public Project with(ExternalTool tool) {
    return with(externals.with(tool));
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

  /**
   * {@return new project instance with an additional external module library locator}
   *
   * @see <a href="https://github.com/sormuras/bach-external-modules">bach-external-modules</a>
   */
  public Project withExternalModules(String library, String version, String... classifiers) {
    var locator =
        ExternalModuleLocator.SormurasBachExternalModulesProperties.of(
            library, version, classifiers);
    return with(locator);
  }

  /** {@return new project instance with an additional external tool} */
  public Project withExternalTool(String name, String from) {
    return with(new ExternalTool(name.strip(), Optional.of(from.strip()), List.of()));
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
      var configurator = root.resolve(".bach/project-info/project/Configurator.java");
      Files.createDirectories(configurator.getParent());
      Files.writeString(
          configurator,
          // language=java
          """
          package project;

          import com.github.sormuras.bach.Project;

          public class Configurator implements com.github.sormuras.bach.Configurator {
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
                project.Configurator;
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

  @Override
  public void formatTo(Formatter formatter, int flags, int width, int precision) {
    formatter.format("Project%n");
    formatter.format("%20s = %s%n", "name", name);
    formatter.format("%20s = %s%n", "version", version.value());
    formatter.format("%20s = %s%n", "version.date", version.date());
    formatter.format("%20s = %s%n", "modules #", modules().size());
    formatter.format("%20s = %s%n", "init modules", spaces.init().modules().names());
    formatter.format("%20s = %s%n", "main modules", spaces.main().modules().names());
    formatter.format("%20s = %s%n", "test modules", spaces.test().modules().names());
    formatter.format("%s", "%s".formatted(spaces.init()).indent(2));
    formatter.format("%s", "%s".formatted(spaces.main()).indent(2));
    formatter.format("%s", "%s".formatted(spaces.test()).indent(2));
  }
}
