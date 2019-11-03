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

import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
    return ProjectBuilder.build(Path.of(""));
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
        var it = arguments.isEmpty() ? project : ProjectBuilder.build(Path.of(arguments.pop()));
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
  public static /*record*/ class Project {
    final String name;
    final Version version;

    public Project(String name, Version version) {
      this.name = name;
      this.version = version;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Project project = (Project) o;
      return name.equals(project.name) &&
             version.equals(project.version);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, version);
    }
  }

  /**
   * Mutable project builder.
   */
  public static class ProjectBuilder {

    static Project build(Path base) {
      return of(base).build();
    }

    static ProjectBuilder of(Path base) {
      if (!Files.isDirectory(base)) {
        throw new IllegalArgumentException("Not a directory: " + base);
      }
      var path = base.toAbsolutePath().normalize();
      var builder = new ProjectBuilder();
      builder.name = Optional.ofNullable(path.getFileName()).map(Path::toString).orElse("project");
      builder.version = System.getProperty(".bach/project.version", "0");
      return builder;
    }

    String name = "project";
    String version = "0";

    Project build() {
      return new Project(name, Version.parse(version));
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

    String $(Object object) {
      return object == null ? "null" : "\"" + object + "\"";
    }

    public List<String> generate(Project project) {
      var lines = new ArrayList<String>();
      lines.add(String.format("new Project(%s, Version.parse(%s))", $(project.name), $(project.version)));
      return lines;
    }
  }
}
