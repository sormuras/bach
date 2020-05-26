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

import de.sormuras.bach.internal.Locators;
import de.sormuras.bach.internal.Modules;
import de.sormuras.bach.internal.ModulesMap;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** A project descriptor. */
public /*static*/ final class Project {

  public static Builder builder() {
    return new Builder();
  }

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
    list.add("\trealms: " + toRealmLabelNames() + " << " + toUnits().count() + " distinct unit(s)");
    list.add("\tdeclares: " + toDeclaredModuleNames());
    list.add("\trequires: " + toRequiredModuleNames());
    list.add("\texternal: " + toExternalModuleNames());
    for (var realm : realms()) {
      list.add("\tRealm " + realm.name());
      list.add("\t\tflags: " + realm.flags());
      list.add("\t\tupstreams: " + new TreeSet<>(realm.upstreams()));
      for (var unit : realm.units().values()) {
        list.add("\t\tUnit " + unit.toName());
        var module = unit.descriptor();
        list.add("\t\t\tModule Descriptor " + module.toNameAndVersion());
        list.add("\t\t\t\tmain-class: " + module.mainClass().orElse("-"));
        list.add("\t\t\t\trequires: " + unit.toRequiredNames());
        list.add("\t\t\tSources");
        for (var source : unit.sources()) {
          list.add("\t\t\t\tpath: " + source.path());
          list.add("\t\t\t\trelease: " + source.release());
        }
      }
    }
    return list;
  }

  public String toTitleAndVersion() {
    return info.title() + ' ' + info.version();
  }

  public Set<String> toDeclaredModuleNames() {
    return toUnits().map(Unit::toName).collect(Collectors.toCollection(TreeSet::new));
  }

  public Set<String> toExternalModuleNames() {
    var externals = new TreeSet<>(toRequiredModuleNames());
    externals.removeAll(toDeclaredModuleNames());
    externals.removeAll(Modules.declared(ModuleFinder.ofSystem()));
    return externals;
  }

  public Set<String> toRequiredModuleNames() {
    return Modules.required(toUnits().map(Unit::descriptor));
  }

  public Set<String> toRealmLabelNames() {
    return realms.stream().map(Realm::toLabelName).collect(Collectors.toCollection(TreeSet::new));
  }

  public Stream<Unit> toUnits() {
    return realms.stream().flatMap(realm -> realm.units().values().stream());
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

    public Path classes(String realm) {
      return workspace("classes", realm);
    }

    public Path classes(String realm, String module) {
      return workspace("classes", realm, module);
    }

    public Path classes(String realm, String module, int release) {
      return workspace("classes-versions", String.valueOf(release), realm, module);
    }

    public Path documentation() {
      return workspace("documentation");
    }

    public Path documentation(String first, String... more) {
      return documentation().resolve(Path.of(first, more));
    }

    public Path image() {
      return workspace("image");
    }

    public Path modules(String realm) {
      return workspace("modules", realm);
    }

    public Path sources(String realm) {
      return workspace("sources", realm);
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
    private final Set<String> required;
    private final Locator locator;

    public Library(Set<String> required, Locator locator) {
      this.required = required;
      this.locator = locator;
    }

    public Set<String> required() {
      return required;
    }

    public Locator locator() {
      return locator;
    }
  }

  /** A function that maps a module name to the string representation of a URI. */
  @FunctionalInterface
  public interface Locator extends UnaryOperator<String>, Consumer<Bach> {

    /** Allows access to the current execution environment. */
    @Override
    default void accept(Bach bach) {}

    /** Return mapped URI for the given module name, may be {@code null} if not mapped. */
    @Override
    String apply(String module);

    /** Create a module locator that is composed from a given sequence of module locators. */
    static Locator of(Locator... locators) {
      return new Locators.ComposedLocator(List.of(locators));
    }

    /** Create a direct map-based locator by referencing {@link Map#get(Object)}. */
    static Locator of(Map<String, String> map) {
      return map::get;
    }

    /** Create a Maven Central based locator parsing complete coordinates. */
    static Locator ofMaven(Map<String, String> coordinates) {
      return new Locators.MavenLocator(coordinates);
    }

    /** Create a Maven repository based locator parsing complete coordinates. */
    static Locator ofMaven(String repository, Map<String, String> coordinates) {
      return new Locators.MavenLocator(repository, coordinates);
    }

    /** Create a locator that loads mappings from {@code sormuras/modules} database. */
    static Locator ofSormurasModules(Map<String, String> versions) {
      return new Locators.SormurasModulesLocator(versions);
    }
  }

  /** A named set of module source units. */
  public static final class Realm {

    /** A flag on a realm. */
    public enum Flag {
      CREATE_API_DOCUMENTATION,
      CREATE_CUSTOM_RUNTIME_IMAGE,
      ENABLE_PREVIEW_LANGUAGE_FEATURES,
      LAUNCH_TESTS
    }

    private final String name;
    private final Set<Flag> flags;
    private final Map<String, Unit> units;
    private final Set<String> upstreams;

    public Realm(String name, Set<Flag> flags, Map<String, Unit> units, Set<String> upstreams) {
      this.name = Objects.requireNonNull(name, "name");
      this.flags = Set.copyOf(Objects.requireNonNull(flags, "flags"));
      this.units = Map.copyOf(Objects.requireNonNull(units, "units"));
      this.upstreams = Set.copyOf(Objects.requireNonNull(upstreams, "upstreams"));
    }

    public String name() {
      return name;
    }

    public Set<Flag> flags() {
      return flags;
    }

    public Map<String, Unit> units() {
      return units;
    }

    public Set<String> upstreams() {
      return upstreams;
    }

    public Optional<Unit> unit(String module) {
      return Optional.ofNullable(units.get(module));
    }

    public String toLabelName() {
      return name.isEmpty() ? "unnamed" : name;
    }
  }

  /** A module source unit. */
  public static final class Unit {

    private final ModuleDescriptor descriptor;
    private final List<Source> sources;
    private final List<Path> resources;

    public Unit(ModuleDescriptor descriptor, List<Source> sources, List<Path> resources) {
      this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
      this.sources = List.copyOf(Objects.requireNonNull(sources, "sources"));
      this.resources = List.copyOf(Objects.requireNonNull(resources, "resources"));
    }

    public ModuleDescriptor descriptor() {
      return descriptor;
    }

    public List<Source> sources() {
      return sources;
    }

    public List<Path> resources() {
      return resources;
    }

    public String toName() {
      return descriptor.name();
    }

    public Set<String> toRequiredNames() {
      var names = descriptor.requires().stream().map(ModuleDescriptor.Requires::name);
      return names.collect(Collectors.toCollection(TreeSet::new));
    }

    public boolean isMultiRelease() {
      if (sources.isEmpty()) return false;
      if (sources.size() == 1) return sources.get(0).isTargeted();
      return sources.stream().allMatch(Source::isTargeted);
    }
  }

  /** A source path with optional release directive. */
  public static final class Source {

    private final Path path;
    private final int release;

    public Source(Path path, int release) {
      this.path = Objects.requireNonNull(path, "path");
      this.release = Objects.checkIndex(release, Runtime.version().feature() + 1);
    }

    public Path path() {
      return path;
    }

    public int release() {
      return release;
    }

    public boolean isTargeted() {
      return release != 0;
    }
  }

  /** A builder for building {@link Project} objects. */
  public static class Builder {

    private Base base = null;
    private Path baseDirectory = Path.of("");
    private Path baseWorkspace = Bach.WORKSPACE;

    private Info info = null;
    private String infoTitle = "Untitled";
    private String infoVersion = "1-ea";

    private Library library = null;
    private final Set<String> libraryRequired = new TreeSet<>();
    private final Map<String, String> libraryMap = new ModulesMap();

    private List<Realm> realms = List.of();

    public Project newProject() {
      return new Project(
          base == null ? new Base(baseDirectory, baseWorkspace) : base,
          info == null ? new Info(infoTitle, Version.parse(infoVersion)) : info,
          library == null ? new Library(libraryRequired, Locator.of(libraryMap)) : library,
          realms == null ? List.of() : realms);
    }

    public Builder setBase(Base base) {
      this.base = base;
      return this;
    }

    public Builder setInfo(Info info) {
      this.info = info;
      return this;
    }

    public Builder setLibrary(Library library) {
      this.library = library;
      return this;
    }

    public Builder setRealms(List<Realm> realms) {
      this.realms = realms;
      return this;
    }

    public Builder base(String directory, String... more) {
      return base(Path.of(directory, more));
    }

    public Builder base(Path directory) {
      this.baseDirectory = directory;
      return this;
    }

    public Builder workspace(Path workspace) {
      this.baseWorkspace = workspace;
      return this;
    }

    public Builder title(String title) {
      this.infoTitle = title;
      return this;
    }

    public Builder version(String version) {
      this.infoVersion = version;
      return this;
    }

    public Builder requires(String module, String... more) {
      this.libraryRequired.add(module);
      this.libraryRequired.addAll(List.of(more));
      return this;
    }

    public Builder map(String module, String uri) {
      this.libraryMap.put(module, uri);
      return this;
    }
  }
}
