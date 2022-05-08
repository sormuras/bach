package com.github.sormuras.bach.project;

import com.github.sormuras.bach.Main;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

/** Modular project model. */
public record Project(
    ProjectName name, ProjectVersion version, ProjectSpaces spaces, ProjectExternals externals) {

  /** An interface for user-defined project configurator implementations. */
  @FunctionalInterface
  public interface Configurator extends UnaryOperator<Project> {
    static List<Configurator> load(ModuleLayer layer) {
      var loader = ServiceLoader.load(layer, Configurator.class);
      return loader.stream().map(ServiceLoader.Provider::get).toList();
    }
  }

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

  private Project with(ProjectSpace space) {
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

  /** {@return new project instance with applying all given configurators} */
  public Project withApplyingConfigurators(List<Configurator> configurators) {
    var project = this;
    for (var configurator : configurators) {
      project = configurator.apply(project);
    }
    return project;
  }

  /** {@return new project instance with applying all configurators loaded from the given layer} */
  public Project withApplyingConfigurators(ModuleLayer layer) {
    return withApplyingConfigurators(Configurator.load(layer));
  }

  /** {@return new project instance configured according to the given arguments} */
  public Project withApplyingArguments(Main.Arguments arguments) {
    var it = new AtomicReference<>(this);
    arguments.project_name().ifPresent(name -> it.set(it.get().withName(name)));
    arguments.project_version().ifPresent(version -> it.set(it.get().withVersion(version)));
    arguments.project_version_date().ifPresent(date -> it.set(it.get().withVersionDate(date)));
    arguments.project_targets_java().ifPresent(java -> it.set(it.get().withTargetsJava(java)));
    arguments.project_launcher().ifPresent(launcher -> it.set(it.get().withLauncher(launcher)));
    return it.get();
  }

  /**
   * {@return new project instance configured by finding all {@code module-info.java} files in the
   * given directory tree}
   */
  public Project withWalkingDirectory(Path directory) {
    return withWalkingDirectory(directory, "glob:**/module-info.java");
  }

  /**
   * {@return new project instance configured by finding {@code module-info.java} files in the
   * matching the given {@link java.nio.file.FileSystem#getPathMatcher(String) syntaxAndPattern} given
   * directory tree}
   */
  public Project withWalkingDirectory(Path directory, String syntaxAndPattern) {
    var project = this;
    var name = directory.normalize().toAbsolutePath().getFileName();
    if (name != null) project = project.withName(name.toString());
    var matcher = directory.getFileSystem().getPathMatcher(syntaxAndPattern);
    try (var stream = Files.find(directory, 9, (p, a) -> matcher.matches(p))) {
      var inits = DeclaredModules.of();
      var mains = DeclaredModules.of();
      var tests = DeclaredModules.of();
      for (var path : stream.toList()) {
        var uri = path.toUri().toString();
        var module = DeclaredModule.of(path);
        if (uri.contains("/init/")) {
          inits = inits.with(module);
          continue;
        }
        if (uri.contains("/test/")) {
          tests = tests.with(module);
          continue;
        }
        mains = mains.with(module);
      }
      project = project.with(project.spaces.init().withModules(inits));
      project = project.with(project.spaces.main().withModules(mains));
      project = project.with(project.spaces.test().withModules(tests));
    } catch (Exception exception) {
      throw new RuntimeException("Find with %s failed".formatted(syntaxAndPattern), exception);
    }
    return project;
  }
}
