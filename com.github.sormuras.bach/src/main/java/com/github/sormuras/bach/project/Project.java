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
public record Project(Name name, Version version, Spaces spaces, Tools tools) {

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
    var name = new Name("unnamed");
    var version = new Version("0-ea", ZonedDateTime.now());
    var init = new Space("init");
    var main = new Space("main", "init");
    var test = new Space("test", "main");
    var spaces = new Spaces(init, main, test);
    var tools = new Tools(List.of());
    return new Project(name, version, spaces, tools);
  }

  public List<DeclaredModule> modules() {
    return spaces.list().stream().flatMap(space -> space.modules().stream()).toList();
  }

  sealed interface Component permits Name, Version, Spaces, Tools {}

  private Project with(Component component) {
    return new Project(
        component instanceof Name name ? name : name,
        component instanceof Version version ? version : version,
        component instanceof Spaces spaces ? spaces : spaces,
        component instanceof Tools tools ? tools : tools);
  }

  private Project with(Space space) {
    return with(spaces.with(space));
  }

  public Project withName(String string) {
    return with(new Name(string));
  }

  public Project withVersion(String string) {
    return with(version.with(string));
  }

  public Project withVersionDate(String string) {
    return with(version.withDate(string));
  }

  public Project withTargetsJava(String string) {
    return with(spaces.main().withTargetsJava(Integer.parseInt(string)));
  }

  public Project withLauncher(String string) {
    return with(spaces.main().withLauncher(string));
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
