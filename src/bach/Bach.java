/*
 * Bach - Java Shell Builder
 * Copyright (C) 2019 Christian Stein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Build modular Java project.
 */
public class Bach {

  /**
   * Bach.java version.
   */
  public static String VERSION = "2.0-ea";

  /**
   * Create project instance to build.
   */
  private static Project project() {
    return Project.Builder.build(Path.of(""));
  }

  /**
   * Main entry-point.
   */
  public static void main(String... args) {
    var project = project();
    if (args.length == 0) {
      System.out.println("Usage: java Bach.java action [args...]");
      System.out.println("Available actions:");
      System.out.println(" build");
      System.out.println("   Build project " + project.name + " " + project.version);
      System.out.println(" project [path]");
      System.out.println("   Print source representation of the project to be built");
      System.out.println(" version");
      System.out.println("   Print version of Bach.java: " + VERSION);
      System.out.println();
      return;
    }
    var arguments = new ArrayDeque<>(List.of(args));
    switch (arguments.pop()) {
      case "build":
        var bach = new Bach(project);
        bach.build();
        return;
      case "project":
        var it = arguments.isEmpty() ? project : Project.Builder.build(Path.of(arguments.pop()));
        new SourceGenerator().generate(it).forEach(System.out::println);
        return;
      case "version":
        System.out.println(VERSION);
        return;
      default:
        throw new Error("Unknown action: " + String.join(" ", args));
    }
  }

  private final Project project;

  public Bach(Project project) {
    this.project = project;
  }

  public void build() {
    System.out.printf("Building project %s %s...%n", project.name, project.version);
  }

  /**
   * Project model.
   */
  public static class Project {

    final String name;
    final Version version;
    final List<Unit> units;

    public Project(String name, Version version, List<Unit> units) {
      this.name = name;
      this.version = version;
      this.units = units;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      var other = (Project) o;
      return name.equals(other.name) &&
             version.equals(other.version) &&
             units.equals(other.units);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, version, units);
    }

    /**
     * Mutable project builder.
     */
    public static class Builder {

      static Project build(Path base) {
        return of(base).build();
      }

      static Builder of(Path base) {
        if (!Files.isDirectory(base)) {
          throw new IllegalArgumentException("Not a directory: " + base);
        }
        var path = base.toAbsolutePath().normalize();
        var builder = new Builder();
        builder.name = Optional.ofNullable(path.getFileName()).map(Path::toString).orElse("project");
        builder.version = System.getProperty(".bach/project.version", "0");
        try (var stream = Files.find(base, 10, (p, __) -> p.endsWith("module-info.java"))) {
          stream.sorted().forEach(info -> builder.units.add(Unit.of(info)));
        } catch (Exception e) {
          throw new Error("Finding module-info.java files failed", e);
        }
        return builder;
      }

      String name = "project";
      String version = "0";
      List<Unit> units = new ArrayList<>();

      Project build() {
        return new Project(name, Version.parse(version), units);
      }
    }

    public static /*record*/ class Unit {

      public static Unit of(Path info) {
        try {
          var descriptor = Modules.describe(Files.readString(info));
          var moduleSourcePath = Modules.moduleSourcePath(info, descriptor.name());
          return new Unit(info, descriptor, moduleSourcePath);
        } catch (Exception e) {
          throw new Error("Reading module declaration failed: " + info, e);
        }
      }

      /**
       * Path to the backing {@code module-info.java} file.
       */
      final Path info;
      /**
       * Underlying module descriptor.
       */
      final ModuleDescriptor descriptor;
      /**
       * Module source path.
       */
      final String moduleSourcePath;

      public Unit(Path info, ModuleDescriptor descriptor, String moduleSourcePath) {
        this.info = info;
        this.descriptor = descriptor;
        this.moduleSourcePath = moduleSourcePath;
      }

      @Override
      public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var other = (Unit) o;
        return info.equals(other.info) &&
               descriptor.equals(other.descriptor) &&
               moduleSourcePath.equals(other.moduleSourcePath);
      }

      @Override
      public int hashCode() {
        return Objects.hash(info, descriptor, moduleSourcePath);
      }
    }
  }

  /**
   * Static helper for modules and their friends.
   */
  public static class Modules {

    private static final Pattern MAIN_CLASS = Pattern.compile("//\\s*(?:--main-class)\\s+([\\w.]+)");

    private static final Pattern MODULE_NAME_PATTERN =
        Pattern.compile(
            "(?:module)" // key word
            + "\\s+([\\w.]+)" // module name
            + "(?:\\s*/\\*.*\\*/\\s*)?" // optional multi-line comment
            + "\\s*\\{"); // end marker

    private static final Pattern MODULE_REQUIRES_PATTERN =
        Pattern.compile(
            "(?:requires)" // key word
            + "(?:\\s+[\\w.]+)?" // optional modifiers
            + "\\s+([\\w.]+)" // module name
            + "(?:\\s*/\\*\\s*([\\w.\\-+]+)\\s*\\*/\\s*)?" // optional '/*' version '*/'
            + "\\s*;"); // end marker

    private static final Pattern MODULE_PROVIDES_PATTERN =
        Pattern.compile(
            "(?:provides)" // key word
            + "\\s+([\\w.]+)" // service name
            + "\\s+with" // separator
            + "\\s+([\\w.,\\s]+)" // comma separated list of type names
            + "\\s*;"); // end marker

    private Modules() {
    }

    /**
     * Module descriptor parser.
     */
    public static ModuleDescriptor describe(String source) {
      // "module name {"
      var nameMatcher = MODULE_NAME_PATTERN.matcher(source);
      if (!nameMatcher.find()) {
        throw new IllegalArgumentException("Expected Java module source unit, but got: " + source);
      }
      var name = nameMatcher.group(1).trim();
      var builder = ModuleDescriptor.newModule(name);
      // "// --main-class name"
      var mainClassMatcher = MAIN_CLASS.matcher(source);
      if (mainClassMatcher.find()) {
        var mainClass = mainClassMatcher.group(1);
        builder.mainClass(mainClass);
      }
      // "requires module /*version*/;"
      var requiresMatcher = MODULE_REQUIRES_PATTERN.matcher(source);
      while (requiresMatcher.find()) {
        var requiredName = requiresMatcher.group(1);
        Optional.ofNullable(requiresMatcher.group(2))
            .ifPresentOrElse(
                version -> builder.requires(Set.of(), requiredName, Version.parse(version)),
                () -> builder.requires(requiredName));
      }
      // "provides service with type, type, ...;"
      var providesMatcher = MODULE_PROVIDES_PATTERN.matcher(source);
      while (providesMatcher.find()) {
        var providesService = providesMatcher.group(1);
        var providesTypes = providesMatcher.group(2);
        builder.provides(providesService, List.of(providesTypes.trim().split("\\s*,\\s*")));
      }
      return builder.build();
    }

    /**
     * Compute module's source path.
     */
    public static String moduleSourcePath(Path path, String module) {
      var directory = path.endsWith("module-info.java") ? path.getParent() : path;
      var names = new ArrayList<String>();
      directory.forEach(element -> names.add(element.toString()));
      int frequency = Collections.frequency(names, module);
      if (frequency == 0) {
        return directory.toString();
      }
      if (frequency == 1) {
        if (directory.endsWith(module)) {
          return Optional.ofNullable(directory.getParent()).map(Path::toString).orElse(".");
        }
        var elements = names.stream().map(name -> name.equals(module) ? "*" : name);
        return elements.collect(Collectors.joining(File.separator));
      }
      throw new IllegalArgumentException("Ambiguous module source path: " + path);
    }
  }

  /**
   * Command.
   */
  public static class Command implements Cloneable {
    final String name;
    final List<String> arguments;

    public Command(String name, String... args) {
      this.name = name;
      this.arguments = new ArrayList<>(List.of(args));
    }

    public Command add(Object object) {
      arguments.add(object.toString());
      return this;
    }

    public Command add(String key, Object value) {
      return add(key).add(value.toString());
    }

    @Override
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public Command clone() {
      return new Command(name, toStringArray());
    }

    public String[] toStringArray() {
      return arguments.toArray(String[]::new);
    }
  }

  /**
   * Java source generator.
   */
  public static class SourceGenerator {

    static String $(Object object) {
      return object == null ? "null" : "\"" + object + "\"";
    }

    static String $(Path path) {
      return path == null ? "null" : "Path.of(\"" + path.toString().replace('\\', '/') + "\")";
    }

    static String $(List<?> objects) {
      return String.join(", ", objects.stream().map(SourceGenerator::$).toArray(String[]::new));
    }

    public List<String> generate(Project project) {
      if (project.units.isEmpty()) {
        var line = "new Project(%s, Version.parse(%s), List.of())";
        return List.of(String.format(line, $(project.name), $(project.version)));
      }
      var lines = new ArrayList<String>();
      lines.add("new Project(");
      lines.add("    " + $(project.name) + ",");
      lines.add("    Version.parse(" + $(project.version) + "),");
      lines.add("    List.of(");
      var last = project.units.get(project.units.size() -1);
      for (var unit : project.units) {
        var comma = unit == last ? "" : ",";
        lines.add("        Project.Unit.of(" + $(unit.info) + ")" + comma);
      }
      lines.add("    )");
      lines.add(")");
      return lines;
    }

    public List<String> generate(Command command) {
      var lines = new ArrayList<String>();
      var tool = "java.util.spi.ToolProvider.findFirst(%s).orElseThrow()";
      var args = command.arguments.isEmpty() ? "" : ", " + $(List.of(command.toStringArray()));
      lines.add(String.format(tool + ".run(System.out, System.err%s)", $(command.name), args));
      return lines;
    }
  }
}
