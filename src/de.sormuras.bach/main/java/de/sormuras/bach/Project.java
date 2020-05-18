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

package de.sormuras.bach;

import de.sormuras.bach.call.Javac;
import de.sormuras.bach.internal.Modules;
import de.sormuras.bach.internal.ModulesMap;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** A project descriptor. */
public /*static*/ final class Project {

  private final Base base;
  private final Info info;
  private final Library library;
  private final List<Realm> realms;

  public Project(Base base, Info info, Library library, List<Realm> realms) {
    this.base = Objects.requireNonNull(base, "base");
    this.info = Objects.requireNonNull(info, "info");
    this.library = Objects.requireNonNull(library, "library");
    this.realms = List.copyOf(Objects.requireNonNull(realms, "realms"));
  }

  public Base base() {
    return base;
  }

  public Info info() {
    return info;
  }

  public Library library() {
    return library;
  }

  public List<Realm> realms() {
    return realms;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Project.class.getSimpleName() + "[", "]")
        .add("base=" + base)
        .add("info=" + info)
        .add("library=" + library)
        .add("realms=" + realms)
        .toString();
  }

  /** Return multi-line string representation of this project's components. */
  public List<String> toStrings() {
    var list = new ArrayList<String>();
    list.add("Project");
    list.add("\ttitle: " + info.title());
    list.add("\tversion: " + info.version());
    list.add("\trealms: " + realms().size());
    list.add("\tunits: " + toUnits().count());
    for (var realm : realms()) {
      list.add("\tRealm " + realm.name());
      list.add("\t\tjavac: " + String.format("%.77s...", realm.javac().toLabel()));
      list.add("\t\ttasks: " + realm.tasks().size());
      for (var unit : realm.units()) {
        list.add("\t\tUnit " + unit.name());
        list.add("\t\t\ttasks: " + unit.tasks().size());
        var module = unit.descriptor();
        list.add("\t\t\tModule Descriptor " + module.toNameAndVersion());
        list.add("\t\t\t\tmain: " + module.mainClass().orElse("-"));
        list.add("\t\t\t\trequires: " + new TreeSet<>(module.requires()));
      }
    }
    return list;
  }

  public String toTitleAndVersion() {
    return info.title() + ' ' + info.version();
  }

  public Set<String> toDeclaredModuleNames() {
    return toUnits().map(Unit::name).collect(Collectors.toCollection(TreeSet::new));
  }

  public Set<String> toRequiredModuleNames() {
    return Modules.required(toUnits().map(Unit::descriptor));
  }

  public Stream<Unit> toUnits() {
    return realms.stream().flatMap(realm -> realm.units().stream());
  }

  /** A base directory with a set of derived directories, files, locations, and other assets. */
  public static final class Base {

    /** Create a base instance for the current working directory. */
    public static Base of() {
      return of(Path.of(""));
    }

    /** Create a base instance for the specified directory. */
    public static Base of(Path directory) {
      return new Base(directory, directory.resolve(Bach.WORKSPACE));
    }

    private final Path directory;
    private final Path workspace;

    Base(Path directory, Path workspace) {
      this.directory = Objects.requireNonNull(directory, "directory");
      this.workspace = Objects.requireNonNull(workspace, "workspace");
    }

    public Path directory() {
      return directory;
    }

    public Path workspace() {
      return workspace;
    }

    public Path path(String first, String... more) {
      return directory.resolve(Path.of(first, more));
    }

    public Path lib() {
      return path("lib");
    }

    public Path workspace(String first, String... more) {
      return workspace.resolve(Path.of(first, more));
    }

    public Path api() {
      return workspace("api");
    }

    public Path classes(String realm) {
      return workspace("classes", realm);
    }

    public Path classes(String realm, String module) {
      return workspace("classes", realm, module);
    }

    public Path image() {
      return workspace("image");
    }

    public Path modules(String realm) {
      return workspace("modules", realm);
    }

    public List<Path> modulePaths(Iterable<String> realms) {
      var paths = new ArrayList<Path>();
      for (var realm : realms) paths.add(modules(realm));
      paths.add(lib());
      return List.copyOf(paths);
    }
  }

  /** A basic information holder. */
  public static final class Info {

    private final String title;
    private final Version version;

    public Info(String title, Version version) {
      this.title = Objects.requireNonNull(title, "title");
      this.version = Objects.requireNonNull(version, "version");
    }

    public String title() {
      return title;
    }

    public Version version() {
      return version;
    }

    @Override
    public String toString() {
      return new StringJoiner(", ", Info.class.getSimpleName() + "[", "]")
          .add("title='" + title + "'")
          .add("version=" + version)
          .toString();
    }
  }

  /** An external modules management and lookup service. */
  public static final class Library {
    public static Library of() {
      return of(new ModulesMap());
    }

    public static Library of(Map<String, String> map) {
      return new Library(Set.of(), map::get);
    }

    private final Set<String> required;
    private final UnaryOperator<String> lookup;

    public Library(Set<String> required, UnaryOperator<String> lookup) {
      this.required = required;
      this.lookup = lookup;
    }

    public Set<String> required() {
      return required;
    }

    public UnaryOperator<String> lookup() {
      return lookup;
    }
  }

  /** A named set of module source units. */
  public static final class Realm {
    private final String name;
    private final List<Unit> units;
    private final Javac javac;
    private final List<Task> tasks;

    public Realm(String name, List<Unit> units, Javac javac, List<Task> tasks) {
      this.name = Objects.requireNonNull(name, "name");
      this.units = List.copyOf(Objects.requireNonNull(units, "units"));
      this.javac = Objects.requireNonNull(javac, "javac");
      this.tasks = List.copyOf(Objects.requireNonNull(tasks, "tasks"));
    }

    public String name() {
      return name;
    }

    public List<Unit> units() {
      return units;
    }

    public Javac javac() {
      return javac;
    }

    public List<Task> tasks() {
      return tasks;
    }
  }

  /** A module source unit. */
  public static final class Unit {

    private final ModuleDescriptor descriptor;
    private final List<Task> tasks;

    public Unit(ModuleDescriptor descriptor, List<Task> tasks) {
      this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
      this.tasks = List.copyOf(Objects.requireNonNull(tasks, "tasks"));
    }

    public ModuleDescriptor descriptor() {
      return descriptor;
    }

    public List<Task> tasks() {
      return tasks;
    }

    public String name() {
      return descriptor.name();
    }
  }
}
