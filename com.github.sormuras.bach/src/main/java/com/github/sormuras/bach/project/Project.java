package com.github.sormuras.bach.project;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

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

  /** {@return a list of all modules declared by this project} */
  public List<DeclaredModule> modules() {
    return spaces.list().stream().flatMap(space -> space.modules().list().stream()).toList();
  }

  sealed interface Component permits ProjectExternals, ProjectName, ProjectSpaces, ProjectVersion {}

  private Project with(Component component) {
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

  /** {@return new project instance with a new module declaration added to the main space} */
  public Project withModule(String info) {
    return withModule("main", info);
  }

  /** {@return new project instance with a new module declaration added to the specified space} */
  public Project withModule(String space, String info) {
    return withModule(spaces.space(space), DeclaredModule.of(info));
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
}
