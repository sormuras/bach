package com.github.sormuras.bach.project;

import com.github.sormuras.bach.Main;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

/** Modular project model. */
public record Project(
    ProjectName name, ProjectVersion version, ProjectSpaces spaces, ProjectExternals externals) {

  @FunctionalInterface
  public interface Configurator extends UnaryOperator<Project> {
    static Project apply(Project project, ModuleLayer layer) {
      var configured = project;
      for (var configurator : ServiceLoader.load(layer, Configurator.class)) {
        configured = configurator.apply(configured);
      }
      return configured;
    }
  }

  public static Project ofDefaults() {
    var name = new ProjectName("unnamed");
    var version = new ProjectVersion("0-ea", ZonedDateTime.now());
    var init = new ProjectSpace("init");
    var main = new ProjectSpace("main", "init");
    var test = new ProjectSpace("test", "main");
    var spaces = new ProjectSpaces(init, main, test);
    var tools = new ProjectExternals(List.of());
    return new Project(name, version, spaces, tools);
  }

  public List<DeclaredModule> modules() {
    return spaces.list().stream().flatMap(space -> space.modules().stream()).toList();
  }

  sealed interface Component permits ProjectName, ProjectVersion, ProjectSpaces, ProjectExternals {}

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

  public Project withName(String string) {
    return with(new ProjectName(string));
  }

  public Project withVersion(String string) {
    return with(version.with(string));
  }

  public Project withVersionDate(String string) {
    return with(version.withDate(string));
  }

  public Project withTargetsJava(String string) {
    return withTargetsJava(Integer.parseInt(string));
  }

  public Project withTargetsJava(int release) {
    return withTargetsJava("main", release);
  }

  public Project withTargetsJava(String space, int release) {
    return withTargetsJava(spaces.space(space), release);
  }

  public Project withTargetsJava(ProjectSpace space, int release) {
    return with(space.withTargetsJava(release));
  }

  public Project withLauncher(String launcher) {
    return withLauncher("main", launcher);
  }

  public Project withLauncher(String space, String launcher) {
    return withLauncher(spaces.space(space), launcher);
  }

  public Project withLauncher(ProjectSpace space, String launcher) {
    return with(space.withLauncher(launcher));
  }

  public Project withApplyingConfigurators(ModuleLayer layer) {
    return Configurator.apply(this, layer);
  }

  public Project withParsingArguments(Main.Arguments arguments) {
    var it = new AtomicReference<>(this);
    arguments.project_name().ifPresent(name -> it.set(it.get().withName(name)));
    arguments.project_version().ifPresent(version -> it.set(it.get().withVersion(version)));
    arguments.project_version_date().ifPresent(date -> it.set(it.get().withVersionDate(date)));
    arguments.project_targets_java().ifPresent(java -> it.set(it.get().withTargetsJava(java)));
    arguments.project_launcher().ifPresent(launcher -> it.set(it.get().withLauncher(launcher)));
    return it.get();
  }

  public Project withWalkingDirectory(Path directory) {
    return withWalkingDirectory(directory, "glob:**/module-info.java");
  }

  public Project withWalkingDirectory(Path directory, String syntaxAndPattern) {
    var project = this;
    var name = directory.normalize().toAbsolutePath().getFileName();
    if (name != null) project = project.withName(name.toString());
    var matcher = directory.getFileSystem().getPathMatcher(syntaxAndPattern);
    try (var stream = Files.find(directory, 9, (p, a) -> matcher.matches(p))) {
      var inits = new ArrayList<DeclaredModule>();
      var mains = new ArrayList<DeclaredModule>();
      var tests = new ArrayList<DeclaredModule>();
      for (var path : stream.toList()) {
        var uri = path.toUri().toString();
        var list = uri.contains("/init/") ? inits : uri.contains("/test/") ? tests : mains;
        var module = DeclaredModule.of(path);
        list.add(module);
      }
      project = project.with(project.spaces.init().withModules(List.copyOf(inits)));
      project = project.with(project.spaces.main().withModules(List.copyOf(mains)));
      project = project.with(project.spaces.test().withModules(List.copyOf(tests)));
    } catch (Exception exception) {
      throw new RuntimeException("Find with %s failed".formatted(syntaxAndPattern), exception);
    }
    return project;
  }
}
