package com.github.sormuras.bach.project;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.ZonedDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** Modular project model. */
public record Project(Name name, Version version, Spaces spaces) {

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
    return new Project(name, version, spaces);
  }

  sealed interface Component permits Name, Spaces, Version {}

  private Project with(Component component) {
    return new Project(
        component instanceof Name name ? name : name,
        component instanceof Version version ? version : version,
        component instanceof Spaces spaces ? spaces : spaces);
  }

  private Project with(Space space) {
    return with(spaces.with(space));
  }

  public Project withParsingDirectory(Path directory) {
    var project = this;
    var name = directory.normalize().toAbsolutePath().getFileName();
    if (name != null) project = project.with(new Name(name.toString()));
    try (var stream = Files.find(directory, 9, Project::isModuleInfoJavaFile)) {
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
      throw new RuntimeException("Find module-info.java files failed", exception);
    }
    return project;
  }

  public Project withParsingArguments(Path arguments) {
    if (Files.notExists(arguments)) return this;
    try {
      return withParsingArguments(Files.readAllLines(arguments));
    } catch (Exception exception) {
      throw new RuntimeException("Read all lines from file failed: " + arguments);
    }
  }

  public Project withParsingArguments(List<String> arguments) {
    if (arguments.size() == 0) return this;
    var remaining = new ArrayDeque<>(arguments);
    var project = this;
    while (!remaining.isEmpty()) {
      var argument = remaining.removeFirst().trim();
      // <- check for flags (key-only arguments) here
      var split = argument.indexOf('=');
      var key = split < 0 ? argument : argument.substring(0, split);
      var value = split < 0 ? remaining.removeFirst().trim() : argument.substring(split + 1);
      project =
          switch (key) {
            case "--project-name" -> project.with(new Name(value));
            case "--project-version" -> project.with(project.version.with(value));
            case "--project-version-date" -> project.with(project.version.withDate(value));
            default -> throw new IllegalArgumentException(key);
          };
    }
    return project;
  }

  private static boolean isModuleInfoJavaFile(Path path, BasicFileAttributes... attributes) {
    return "module-info.java".equals(path.getFileName().toString()) && Files.isRegularFile(path);
  }
}
