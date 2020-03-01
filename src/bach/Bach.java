/*
 * Bach - Java Shell Builder
 * Copyright (C) 2020 Christian Stein
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
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;
/** Bach - Java Shell Builder. */
public class Bach {
  /** Version of Bach. */
  public static Version VERSION = Version.parse("11.0-ea");
  /** Main entry-point. */
  public static void main(String... args) {
    Main.main(args);
  }
  /** Build default project potentially modified by the passed project builder consumer. */
  public Object build(Consumer<ProjectBuilder> projectBuilderConsumer) {
    return build(project(projectBuilderConsumer));
  }
  /** Build the specified project. */
  public Object build(Project project) {
    return project;
  }
  /** Create new default project potentially modified by the passed project builder consumer. */
  Project project(Consumer<ProjectBuilder> projectBuilderConsumer) {
    // var projectBuilder = new ProjectScanner(paths).scan();
    var projectBuilder = new ProjectBuilder();
    projectBuilderConsumer.accept(projectBuilder);
    return projectBuilder.build();
  }
  // src/de.sormuras.bach/main/java/de/sormuras/bach/Main.java
  /** Bach's main program. */
  static class Main {
    public static void main(String... args) {
      System.out.println("Bach.java " + Bach.VERSION);
      new Bach().build(project -> project.name("project")).toString();
    }
  }
  // src/de.sormuras.bach/main/java/de/sormuras/bach/model/Paths.java
  /** Common project-related paths. */
  public static final class Paths {
    private static final Path CLASSES = Path.of("classes");
    private static final Path MODULES = Path.of("modules");
    private static final Path SOURCES = Path.of("sources");
    private static final Path DOCUMENTATION = Path.of("documentation");
    private static final Path JAVADOC = DOCUMENTATION.resolve("javadoc");
    /** Create default instance for the specified base directory. */
    public static Paths of(Path base) {
      return new Paths(base, base.resolve(".bach"), base.resolve("lib"));
    }
    private final Path base;
    private final Path out;
    private final Path lib;
    public Paths(Path base, Path out, Path lib) {
      this.base = base;
      this.out = out;
      this.lib = lib;
    }
    public Path base() {
      return base;
    }
    public Path out() {
      return out;
    }
    public Path lib() {
      return lib;
    }
    public Path out(String first, String... more) {
      var path = Path.of(first, more);
      return out.resolve(path);
    }
    public Path classes(String realm) {
      return out.resolve(CLASSES).resolve(realm);
    }
    public Path javadoc() {
      return out.resolve(JAVADOC);
    }
    public Path modules(String realm) {
      return out.resolve(MODULES).resolve(realm);
    }
    public Path sources(String realm) {
      return out.resolve(SOURCES).resolve(realm);
    }
  }
  // src/de.sormuras.bach/main/java/de/sormuras/bach/model/Project.java
  /** Bach's project model. */
  public static final class Project {
    private final String name;
    private final Version version;
    private final Structure structure;
    public Project(String name, Version version, Structure structure) {
      this.name = Objects.requireNonNull(name, "name");
      this.version = version;
      this.structure = Objects.requireNonNull(structure, "paths");
    }
    public String name() {
      return name;
    }
    public Version version() {
      return version;
    }
    public Structure structure() {
      return structure;
    }
    public Paths paths() {
      return structure().paths();
    }
  }
  // src/de.sormuras.bach/main/java/de/sormuras/bach/model/ProjectBuilder.java
  /** Project model builder. */
  public static final class ProjectBuilder {
    private String name;
    private Version version;
    private Paths paths = Paths.of(Path.of(""));
    public Project build() {
      var structure = new Structure(paths);
      return new Project(name, version, structure);
    }
    public ProjectBuilder name(String name) {
      this.name = name;
      return this;
    }
    public ProjectBuilder version(Version version) {
      this.version = version;
      return this;
    }
    public ProjectBuilder version(String version) {
      return version(Version.parse(version));
    }
    public ProjectBuilder paths(Paths paths) {
      this.paths = paths;
      return this;
    }
    public ProjectBuilder paths(String base) {
      return paths(Paths.of(Path.of(base)));
    }
  }
  // src/de.sormuras.bach/main/java/de/sormuras/bach/model/Structure.java
  /** Project structure. */
  public static final class Structure {
    private final Paths paths;
    public Structure(Paths paths) {
      this.paths = paths;
    }
    public Paths paths() {
      return paths;
    }
  }
}
