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

package de.sormuras.bach.api;

import de.sormuras.bach.internal.Modules;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** Bach's project model. */
public /*static*/ final class Project {

  /** Return a mutable builder for creating a project instance. */
  public static Builder builder() {
    return new Builder();
  }

  public static Scanner scanner(Path base) {
    return new Scanner(Paths.of(base));
  }

  private final String name;
  private final Version version;
  private final Structure structure;
  private final Library library;

  private Project(String name, Version version, Structure structure, Library library) {
    this.name = Objects.requireNonNull(name, "name");
    this.version = version;
    this.structure = Objects.requireNonNull(structure, "structure");
    this.library = Objects.requireNonNull(library, "library");
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

  public Library library() {
    return library;
  }

  public Paths paths() {
    return structure().paths();
  }

  public Tuner tuner() {
    return structure().tuner();
  }

  public Set<String> toDeclaredModuleNames() {
    return structure.units().stream()
        .map(Unit::name)
        .collect(Collectors.toCollection(TreeSet::new));
  }

  public Set<String> toRequiredModuleNames() {
    return Modules.required(structure.units().stream().map(Unit::descriptor));
  }

  public String toNameAndVersion() {
    if (version == null) return name;
    return name + ' ' + version;
  }

  /** Compose JAR file name by preferring passed argument values over project properties. */
  public String toJarName(Unit unit, String classifier) {
    var unitVersion = unit.descriptor().version();
    var version = unitVersion.isPresent() ? unitVersion : Optional.ofNullable(this.version);
    var versionSuffix = version.map(v -> "-" + v).orElse("");
    var classifierSuffix = classifier.isEmpty() ? "" : "-" + classifier;
    return unit.name() + versionSuffix + classifierSuffix + ".jar";
  }

  /** Compose path to the Java module specified by its realm and modular unit. */
  public Path toModularJar(Realm realm, Unit unit) {
    return paths().modules(realm).resolve(toJarName(unit, ""));
  }

  /** Return multi-line string representation of this project's components. */
  public List<String> toStrings() {
    var list = new ArrayList<String>();
    list.add("Project " + toNameAndVersion());
    list.add("\tname: " + name);
    list.add("\tversion: " + version);
    list.add("\trealms: " + structure.realms().size());
    list.add("\tunits: " + structure.units().size());
    for (var realm : structure.realms()) {
      list.add("\tRealm " + realm.title());
      list.add("\t\tflags: " + realm.flags());
      for (var unit : realm.units()) {
        list.add("\t\tUnit " + unit.name());
        list.add("\t\t\tmodule: " + unit.name());
        list.add("\t\t\tinfo: " + unit.info());
        list.add("\t\t\tmulti-release: " + unit.isMultiRelease());
        list.add("\t\t\tmain-class-present: " + unit.isMainClassPresent());
        var module = unit.descriptor();
        list.add("\t\t\tModule Descriptor " + module.toNameAndVersion());
        list.add("\t\t\t\tmain: " + module.mainClass().orElse("-"));
        list.add("\t\t\t\trequires: " + new TreeSet<>(module.requires()));
        for (var source : unit.sources()) {
          list.add("\t\t\tSource " + source.path().getFileName());
          list.add("\t\t\t\tpath: " + source.path());
          list.add("\t\t\t\trelease: " + source.release());
          list.add("\t\t\t\tflags: " + source.flags());
        }
      }
    }
    return list;
  }

  /** A mutable builder for a {@link Project}. */
  public static class Builder {

    private String name;
    private Version version;

    private Paths paths;
    private List<Unit> units;
    private List<Realm> realms;
    private Tuner tuner;

    private Set<String> requires;
    private List<Locator> locators;

    private Builder() {
      name(null);
      version((Version) null);
      base("");
      units(List.of());
      realms(List.of());
      tuner(new Tuner());
      requires(Set.of());
      locators(List.of());
    }

    public Project build() {
      var structure = new Structure(paths, units, realms, tuner);
      var library = new Library(new TreeSet<>(requires), locators);
      return new Project(name, version, structure, library);
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder version(Version version) {
      this.version = version;
      return this;
    }

    public Builder version(String version) {
      return version(Version.parse(version));
    }

    public Builder paths(Paths paths) {
      this.paths = paths;
      return this;
    }

    public Builder base(Path base) {
      return paths(Paths.of(base));
    }

    public Builder base(String base) {
      return base(Path.of(base));
    }

    public Builder units(List<Unit> units) {
      this.units = units;
      return this;
    }

    public Builder realms(List<Realm> realms) {
      this.realms = realms;
      return this;
    }

    public Builder tuner(Tuner tuner) {
      this.tuner = tuner;
      return this;
    }

    public Builder requires(Set<String> libraryRequires) {
      this.requires = libraryRequires;
      return this;
    }

    public Builder locators(List<Locator> libraryLocators) {
      this.locators = libraryLocators;
      return this;
    }
  }
}
