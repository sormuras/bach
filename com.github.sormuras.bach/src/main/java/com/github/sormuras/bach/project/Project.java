package com.github.sormuras.bach.project;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** Modular project model. */
public record Project(Name name, Version version, Spaces spaces, Tools tools) {

  @FunctionalInterface
  public interface Configurator {
    Project configure(Project project);
  }

  public static Project of() {
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

  public Project withParsingDirectory(Path directory) {
    return withParsingDirectory(directory, "glob:**/module-info.java");
  }

  public Project withParsingDirectory(Path directory, String syntaxAndPattern) {
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

  public Project withParsingArguments(Path arguments) {
    if (Files.notExists(arguments)) return this;
    try {
      return withParsingArguments(Files.readAllLines(arguments));
    } catch (Exception exception) {
      throw new RuntimeException("Read all lines from file failed: " + arguments, exception);
    }
  }

  public Project withParsingArguments(List<String> arguments) {
    if (arguments.size() == 0) return this;
    var remaining = new ArrayDeque<>(arguments);
    var project = this;
    while (!remaining.isEmpty()) {
      var argument = remaining.removeFirst().trim();
      if (argument.isBlank() || argument.startsWith("#")) continue;
      // <- parse flags (key-only arguments) here
      var split = argument.indexOf('=');
      var key = split < 0 ? argument : argument.substring(0, split);
      var value = split < 0 ? remaining.removeFirst().trim() : argument.substring(split + 1);
      project = project.withParsingArgument(key, value);
    }
    return project;
  }

  public Project withParsingArgument(String key, String value) {
    return switch (key) {
      case "--project-name" -> withName(value);
      case "--project-version" -> withVersion(value);
      case "--project-version-date" -> withVersionDate(value);
      case "--project-targets-java" -> withTargetsJava(value);
      case "--project-launcher" -> withLauncher(value);
      default -> throw new IllegalArgumentException(key);
    };
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
}
