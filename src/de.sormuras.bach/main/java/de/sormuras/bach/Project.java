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
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** A project descriptor. */
public final class Project {

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

  /**
   * Return multi-line string representation of this project's components.
   *
   * @return the list of strings
   */
  public List<String> toStrings() {
    var list = new ArrayList<String>();
    list.add("Project");
    list.add("\ttitle: " + info.title());
    list.add("\tversion: " + info.version());
    list.add("\tLibrary");
    list.add("\t\trequires: " + library().required());
    list.add("\t\tlocator: " + library().locator().getClass().getSimpleName());
    list.add("\tAll realms");
    list.add("\t\tnames: " + toRealmLabelNames());
    list.add("\t\tunits: " + toUnits().count());
    list.add("\t\tdeclares: " + toDeclaredModuleNames());
    list.add("\t\trequires: " + toRequiredModuleNames());
    list.add("\t\texternal: " + toExternalModuleNames());
    for (var realm : realms()) {
      list.add("\tRealm " + realm.name());
      list.add("\t\tflags: " + realm.flags());
      list.add("\t\tupstreams: " + new TreeSet<>(realm.upstreams()));
      for (var unit : new TreeSet<>(realm.units().values())) {
        list.add("\t\tUnit " + unit.toName());
        var module = unit.descriptor();
        list.add("\t\t\tModule Descriptor " + module.toNameAndVersion());
        module.mainClass().ifPresent(name -> list.add("\t\t\t\tmain-class: " + name));
        list.add("\t\t\t\trequires: " + Modules.required(Stream.of(unit.descriptor())));
        list.add("\t\t\tSources");
        for (var source : unit.sources()) {
          list.add("\t\t\t\tpath: " + source.path());
          if (source.isTargeted()) list.add("\t\t\t\trelease: " + source.release());
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

  public Path toJar(String realm, String module) {
    return base().modules(realm).resolve(module + "@" + info().version() + ".jar");
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

    /**
     * Create a base instance for the current working directory.
     *
     * @return a new {@link Base} instance for the current working directory
     */
    public static Base of() {
      return of(Path.of(""));
    }

    /**
     * Create a base instance for the specified directory.
     *
     * @param path the base directory
     * @return a new {@link Base} instance for the specified directory
     */
    public static Base of(Path path) {
      return new Base(path, path.resolve(Bach.LIBRARIES), path.resolve(Bach.WORKSPACE));
    }

    private final Path directory;
    private final Path libraries;
    private final Path workspace;

    Base(Path directory, Path libraries, Path workspace) {
      this.directory = Objects.requireNonNull(directory, "directory");
      this.libraries = Objects.requireNonNull(libraries, "libraries");
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

    public Path libraries() {
      return libraries;
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

    private final int compileForJavaRelease;
    private final boolean terminateCompilationIfWarningsOccur;

    public Info(
        String title,
        Version version,
        int compileForJavaRelease,
        boolean terminateCompilationIfWarningsOccur) {
      this.title = Objects.requireNonNull(title, "title");
      this.version = Objects.requireNonNull(version, "version");
      this.compileForJavaRelease = compileForJavaRelease;
      this.terminateCompilationIfWarningsOccur = terminateCompilationIfWarningsOccur;
    }

    public String title() {
      return title;
    }

    public Version version() {
      return version;
    }

    public int compileForJavaRelease() {
      return compileForJavaRelease;
    }

    public boolean terminateCompilationIfWarningsOccur() {
      return terminateCompilationIfWarningsOccur;
    }

    @Override
    public String toString() {
      return new StringJoiner(", ", Info.class.getSimpleName() + "[", "]")
          .add("title='" + title + "'")
          .add("version=" + version)
          .add("compileForJavaRelease=" + compileForJavaRelease)
          .add("terminateCompilationIfWarningsOccur=" + terminateCompilationIfWarningsOccur)
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
  public interface Locator {

    /**
     * Allows access to the current execution environment.
     *
     * @param bach the {@link Bach} instance to operate on
     */
    default void accept(Bach bach) {}

    /**
     * Return mapped string representation of a URI for the given module name.
     *
     * @param module the name of the module to locate
     * @return the mapped URI or empty {@code Optional} if the location is unknown
     */
    Optional<String> locate(String module);

    /**
     * Create a module locator that is composed from a given sequence of module locators.
     *
     * @param locators a possibly-empty array of locators to combine and query in order
     * @return a {@link Locator} instance delegating {@link Locator#locate(String)} queries
     */
    static Locator of(Locator... locators) {
      return new Locators.ComposedLocator(List.of(locators));
    }

    /**
     * Create a map-based locator.
     *
     * @param map the map that contains module names to module URI mappings
     * @return a {@link Locator} instance referencing {@link Map#get(Object)} directly
     */
    static Locator of(Map<String, String> map) {
      return module -> Optional.ofNullable(map.get(module));
    }

    /**
     * Create a Maven Central based locator parsing complete coordinates.
     *
     * @param coordinates the map that contains Maven Central coordinates
     * @return a {@link Locator} instance
     */
    static Locator ofMaven(Map<String, String> coordinates) {
      return new Locators.MavenLocator(coordinates);
    }

    /**
     * Create a Maven repository based locator parsing complete coordinates.
     *
     * @param repository the host and path to a Maven repository
     * @param coordinates the map that contains Maven coordinates
     * @return a {@link Locator} instance
     */
    static Locator ofMaven(String repository, Map<String, String> coordinates) {
      return new Locators.MavenLocator(repository, coordinates);
    }

    /**
     * Create a locator that loads mappings from {@code sormuras/modules} database.
     *
     * @param versions the map that contains version overrides
     * @return a {@link Locator} instance *
     */
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
  public static final class Unit implements Comparable<Unit> {

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

    @Override
    public int compareTo(Unit o) {
      return descriptor.compareTo(o.descriptor);
    }

    public String toName() {
      return descriptor.name();
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
    private Path baseLibraries = Bach.LIBRARIES;
    private Path baseWorkspace = Bach.WORKSPACE;

    private Info info = null;
    private String infoTitle = "Untitled";
    private String infoVersion = "1-ea";
    private int infoCompileForJavaRelease = 0;
    private boolean infoTerminateCompilationIfWarningsOccur = false;

    private Library library = null;
    private final Set<String> libraryRequired = new TreeSet<>();
    private Locator locator = null;
    private final Map<String, String> locatorMap = new ModulesMap();

    private List<Realm> realms = List.of();

    public Project newProject() {
      return new Project(
          base != null ? base : new Base(baseDirectory, baseLibraries, baseWorkspace),
          info(),
          library(),
          realms != null ? realms : List.of());
    }

    private Info info() {
      if (info != null) return info;
      return new Info(
          infoTitle,
          Version.parse(infoVersion),
          infoCompileForJavaRelease,
          infoTerminateCompilationIfWarningsOccur);
    }

    private Library library() {
      if (library != null) return library;
      var composed = Locator.of(Locator.of(locatorMap), Locator.ofSormurasModules(Map.of()));
      return new Library(libraryRequired, locator != null ? locator : composed);
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

    public Builder setLocator(Locator locator) {
      this.locator = locator;
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

    public Builder libraries(Path libraries) {
      this.baseLibraries = libraries;
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

    public Builder compileForJavaRelease(int release) {
      this.infoCompileForJavaRelease = release;
      return this;
    }

    public Builder terminateCompilationIfWarningsOccur(boolean terminate) {
      this.infoTerminateCompilationIfWarningsOccur = terminate;
      return this;
    }

    public Builder requires(String module, String... more) {
      this.libraryRequired.add(module);
      this.libraryRequired.addAll(List.of(more));
      return this;
    }

    public Builder map(String module, String uri) {
      this.locatorMap.put(module, uri);
      return this;
    }
  }
}
