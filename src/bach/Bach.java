// THIS FILE WAS GENERATED ON 2019-09-26T03:32:42.524083500Z
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

// default package


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.SourceVersion;

public class Bach {

  public static String VERSION = "2-ea";

  /**
   * Main entry-point.
   *
   * @param args List of API method or tool names.
   */
  public static void main(String... args) {
    var bach = new Bach();
    try {
      bach.main(args.length == 0 ? List.of("build") : List.of(args));
    } catch (Throwable throwable) {
      bach.err.printf("Bach.java (%s) failed: %s%n", VERSION, throwable.getMessage());
      if (bach.verbose) {
        throwable.printStackTrace(bach.err);
      }
    }
  }

  /** Text-output writer. */
  private final PrintWriter out, err;
  /** Be verbose. */
  private final boolean verbose;
  /** Project to be built. */
  private final Project project;

  /** Initialize default instance. */
  public Bach() {
    this(
        new PrintWriter(System.out, true),
        new PrintWriter(System.err, true),
        Boolean.getBoolean("ebug") || "".equals(System.getProperty("ebug")),
        Project.of(Path.of("")));
  }

  /** Initialize. */
  public Bach(PrintWriter out, PrintWriter err, boolean verbose, Project project) {
    this.out = Util.requireNonNull(out, "out");
    this.err = Util.requireNonNull(err, "err");
    this.verbose = verbose;
    this.project = project;
    log("New Bach.java (%s) instance initialized: %s", VERSION, this);
  }

  /** Print "debug" message to the standard output stream. */
  void log(String format, Object... args) {
    if (verbose) out.println(String.format(format, args));
  }

  /** Non-static entry-point used by {@link #main(String...)} and {@code BachToolProvider}. */
  void main(List<String> arguments) {
    var tasks = Util.requireNonEmpty(Task.of(this, arguments), "tasks");
    log("Running %d argument task(s): %s", tasks.size(), tasks);
    tasks.forEach(consumer -> consumer.accept(this));
  }

  /** Run the passed command. */
  void run(Command command) {
    var tool = ToolProvider.findFirst(command.getName());
    int code = run(tool.orElseThrow(), command.toStringArray());
    if (code != 0) {
      throw new AssertionError("Running command failed: " + command);
    }
  }

  /** Run the tool using the passed provider and arguments. */
  int run(ToolProvider tool, String... arguments) {
    log("Running %s %s", tool.name(), String.join(" ", arguments));
    return tool.run(out, err, arguments);
  }

  /** Get the {@code Bach.java} banner. */
  private String banner() {
    var module = getClass().getModule();
    try (var stream = module.getResourceAsStream("de/sormuras/bach/banner.txt")) {
      if (stream == null) {
        return String.format("Bach.java %s (member of %s)", VERSION, module);
      }
      var lines = new BufferedReader(new InputStreamReader(stream)).lines();
      var banner = lines.collect(Collectors.joining(System.lineSeparator()));
      return banner + " " + VERSION;
    } catch (IOException e) {
      throw new UncheckedIOException("Loading banner resource failed", e);
    }
  }

  /** Verbosity flag. */
  boolean verbose() {
    return verbose;
  }

  /** Print help text to the standard output stream. */
  public void help() {
    out.println(banner());
    out.println("Method API");
    Arrays.stream(getClass().getMethods())
        .filter(Util::isApiMethod)
        .map(m -> "  " + m.getName() + " (" + m.getDeclaringClass().getSimpleName() + ")")
        .sorted()
        .forEach(out::println);
    out.println("Provided tools");
    ServiceLoader.load(ToolProvider.class).stream()
        .map(provider -> "  " + provider.get().name())
        .sorted()
        .forEach(out::println);
  }

  /** Build. */
  public void build() {
    info();

    resolve();

    var main = project.realms.get(0);
    if (main.units.isEmpty()) {
      throw new AssertionError("No module declared in realm " + main.name);
    }

    var hydras = main.modules.getOrDefault("hydra", List.of());
    if (!hydras.isEmpty()) {
      new Hydra(this, project, main).compile(hydras);
    }

    var jigsaws = main.modules.getOrDefault("jigsaw", List.of());
    if (!jigsaws.isEmpty()) {
      new Jigsaw(this, project, main).compile(jigsaws);
    }

    new Scribe(this, project, main).document();

    // summary(main);
  }

  /** Print all "interesting" information. */
  public void info() {
    out.printf("Bach.java (%s)%n", VERSION);
    out.printf("Project '%s'%n", project.name);
  }

  /** Resolve missing modules. */
  public void resolve() {
    new Resolver(this).resolve();
  }

  /** Print Bach.java's version to the standard output stream. */
  public void version() {
    out.println(VERSION);
  }

  /** Command-line program argument list builder. */
  public static class Command {

    private final String name;
    private final List<String> arguments = new ArrayList<>();

    /** Initialize Command instance with zero or more arguments. */
    public Command(String name, Object... args) {
      this.name = name;
      addEach(args);
    }

    /** Add single argument by invoking {@link Object#toString()} on the given argument. */
    public Command add(Object argument) {
      arguments.add(argument.toString());
      return this;
    }

    /** Add two arguments by invoking {@link #add(Object)} for the key and value elements. */
    public Command add(Object key, Object value) {
      return add(key).add(value);
    }

    /** Add two (or zero) arguments, the key and the paths joined by system's path separator. */
    public Command add(Object key, Collection<Path> paths) {
      return add(key, paths.stream(), UnaryOperator.identity());
    }

    /** Add two (or zero) arguments, the key and the paths joined by system's path separator. */
    public Command add(Object key, Stream<Path> stream, UnaryOperator<String> operator) {
      var value = stream.map(Object::toString).collect(Collectors.joining(File.pathSeparator));
      if (value.isEmpty()) {
        return this;
      }
      return add(key, operator.apply(value));
    }

    /** Add all arguments by invoking {@link #add(Object)} for each element. */
    public Command addEach(Object... arguments) {
      return addEach(List.of(arguments));
    }

    /** Add all arguments by invoking {@link #add(Object)} for each element. */
    public Command addEach(Iterable<?> arguments) {
      arguments.forEach(this::add);
      return this;
    }

    /** Add all arguments by delegating to the passed visitor for each element. */
    public <T> Command addEach(Iterable<T> arguments, BiConsumer<Command, T> visitor) {
      arguments.forEach(argument -> visitor.accept(this, argument));
      return this;
    }

    /** Add a single argument iff the conditions is {@code true}. */
    public Command addIff(boolean condition, Object argument) {
      return condition ? add(argument) : this;
    }

    /** Add two arguments iff the conditions is {@code true}. */
    public Command addIff(boolean condition, Object key, Object value) {
      return condition ? add(key, value) : this;
    }

    /** Add two arguments iff the given optional value is present. */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public Command addIff(Object key, Optional<?> optionalValue) {
      return optionalValue.isPresent() ? add(key, optionalValue.get()) : this;
    }

    /** Let the consumer visit, usually modify, this instance iff the conditions is {@code true}. */
    public Command addIff(boolean condition, Consumer<Command> visitor) {
      if (condition) {
        visitor.accept(this);
      }
      return this;
    }

    /** Return the command's name. */
    public String getName() {
      return name;
    }

    /** Return the command's arguments. */
    public List<String> getArguments() {
      return arguments;
    }

    @Override
    public String toString() {
      var args = arguments.isEmpty() ? "<empty>" : "'" + String.join("', '", arguments) + "'";
      return "Command{name='" + name + "', args=[" + args + "]}";
    }

    /** Return an array of {@link String} containing all of the collected arguments. */
    public String[] toStringArray() {
      return arguments.toArray(String[]::new);
    }

    /** Return program's name and all arguments as single string using space as the delimiter. */
    public String toCommandLine() {
      return toCommandLine(" ");
    }

    /** Return program's name and all arguments as single string using passed delimiter. */
    public String toCommandLine(String delimiter) {
      if (arguments.isEmpty()) {
        return name;
      }
      return name + delimiter + String.join(delimiter, arguments);
    }
  }

  /** Modular project model. */
  public static class Project {

    /** Create default project parsing the passed base directory. */
    public static Project of(Path baseDirectory) {
      var main = new Realm("main", false, 0, "src/*/main/java", Map.of(), Map.of());
      var name = Optional.ofNullable(baseDirectory.toAbsolutePath().getFileName());
      return new Project(
          baseDirectory,
          baseDirectory.resolve("bin"),
          name.orElse(Path.of("project")).toString().toLowerCase(),
          Version.parse("0"),
          new Library(baseDirectory.resolve("lib")),
          List.of(main));
    }

    /** Base directory. */
    public final Path baseDirectory;
    /** Target directory. */
    public final Path targetDirectory;
    /** Name of the project. */
    public final String name;
    /** Version of the project. */
    public final Version version;
    /** Library. */
    public final Library library;
    /** Realms. */
    public final List<Realm> realms;

    public Project(
        Path baseDirectory,
        Path targetDirectory,
        String name,
        Version version,
        Library library,
        List<Realm> realms) {
      this.baseDirectory = Util.requireNonNull(baseDirectory, "base");
      this.targetDirectory = Util.requireNonNull(targetDirectory, "targetDirectory");
      this.version = Util.requireNonNull(version, "version");
      this.name = Util.requireNonNull(name, "name");
      this.library = Util.requireNonNull(library, "library");
      this.realms = List.copyOf(Util.requireNonEmpty(realms, "realms"));
    }

    public Target target(Realm realm) {
      return new Target(targetDirectory, realm);
    }

    /** Manage external 3rd-party modules. */
    public static class Library {
      /** List of library paths to external 3rd-party modules. */
      public final List<Path> modulePaths;
      /** Map external 3rd-party module names to their {@code URI}s. */
      public final Function<String, URI> moduleMapper;
      /** Map external 3rd-party module names to their Maven repository. */
      public final Function<String, URI> mavenRepositoryMapper;
      /** Map external 3rd-party module names to their colon-separated Maven Group and Artifact ID. */
      public final UnaryOperator<String> mavenGroupColonArtifactMapper;
      /** Map external 3rd-party module names to their Maven version. */
      public final UnaryOperator<String> mavenVersionMapper;

      public Library(Path lib) {
        this(
            List.of(lib),
            UnmappedModuleException::throwForURI,
            __ -> URI.create("https://repo1.maven.org/maven2"),
            UnmappedModuleException::throwForString,
            UnmappedModuleException::throwForString);
      }

      public Library(
          List<Path> modulePaths,
          Function<String, URI> moduleMapper,
          Function<String, URI> mavenRepositoryMapper,
          UnaryOperator<String> mavenGroupColonArtifactMapper,
          UnaryOperator<String> mavenVersionMapper) {
        this.modulePaths = List.copyOf(Util.requireNonEmpty(modulePaths, "modulePaths"));
        this.moduleMapper = moduleMapper;
        this.mavenRepositoryMapper = mavenRepositoryMapper;
        this.mavenGroupColonArtifactMapper = mavenGroupColonArtifactMapper;
        this.mavenVersionMapper = mavenVersionMapper;
      }
    }

    /** Java module source unit. */
    public static class ModuleUnit {
      /** Path to the backing {@code module-info.java} file. */
      public final Path info;
      /** Paths to the source directories. */
      public final List<Path> sources;
      /** Paths to the resource directories. */
      public final List<Path> resources;
      /** Associated module descriptor, normally parsed from module {@link #info} file. */
      public final ModuleDescriptor descriptor;

      public ModuleUnit(
          Path info, List<Path> sources, List<Path> resources, ModuleDescriptor descriptor) {
        this.info = info;
        this.sources = List.copyOf(sources);
        this.resources = List.copyOf(resources);
        this.descriptor = descriptor;
      }
    }

    /** Multi-release module source unit */
    public static class MultiReleaseUnit extends ModuleUnit {
      /** Feature release number to source path map. */
      public final Map<Integer, Path> releases;
      /** Copy this module descriptor to the root of the generated modular jar. */
      public final int copyModuleDescriptorToRootRelease;

      public MultiReleaseUnit(
          Path info,
          int copyModuleDescriptorToRootRelease,
          Map<Integer, Path> releases,
          List<Path> resources,
          ModuleDescriptor descriptor) {
        super(info, List.copyOf(new TreeMap<>(releases).values()), resources, descriptor);
        this.copyModuleDescriptorToRootRelease = copyModuleDescriptorToRootRelease;
        this.releases = releases;
      }
    }

    /** Main- and test realms. */
    public static class Realm {
      /** Name of the realm. */
      public final String name;
      /** Enable preview features. */
      public final boolean preview;
      /** Java feature release target number. */
      public final int release;
      /** Module source path specifies where to find input source files for multiple modules. */
      public final String moduleSourcePath;
      /** Map of all declared module source unit. */
      public final Map<String, ModuleUnit> units;
      /** Map of compiler-specific module names. */
      public final Map<String, List<String>> modules;
      /** List of required realms. */
      public final List<Realm> realms;

      public Realm(
          String name,
          boolean preview,
          int release,
          String moduleSourcePath,
          Map<String, List<String>> modules,
          Map<String, ModuleUnit> units,
          Realm... realms) {
        this.name = name;
        this.preview = preview;
        this.release = release;
        this.moduleSourcePath = moduleSourcePath;
        this.modules = Map.copyOf(modules);
        this.units = Map.copyOf(units);
        this.realms = List.of(realms);
      }
    }

    /** Collection of directories and other realm-specific assets. */
    public static class Target {
      public final Path directory;
      public final Path modules;

      private Target(Path projectTargetDirectory, Realm realm) {
        this.directory = projectTargetDirectory.resolve("realm").resolve(realm.name);
        this.modules = directory.resolve("modules");
      }
    }
  }

  public static class Jigsaw {

    private final Bach bach;
    private final Project project;
    private final Project.Realm realm;
    private final Project.Target target;

    public Jigsaw(Bach bach, Project project, Project.Realm realm) {
      this.bach = bach;
      this.project = project;
      this.realm = realm;
      this.target = project.target(realm);
    }

    public void compile(Collection<String> modules) {
      bach.log("Compiling %s realm jigsaw modules: %s", realm.name, modules);
      var classes = target.directory.resolve("jigsaw").resolve("classes");
      var modulePath = new ArrayList<Path>();
      if (Files.isDirectory(target.modules)) {
        modulePath.add(target.modules);
      }
      for (var other : realm.realms) {
        var otherTarget = project.target(other);
        if (Files.isDirectory(otherTarget.modules)) {
          modulePath.add(otherTarget.modules);
        }
      }
      modulePath.addAll(project.library.modulePaths);
      bach.run(
          new Command("javac")
              .add("-d", classes)
              .addIff(realm.preview, "--enable-preview")
              .addIff(realm.release != 0, "--release", realm.release)
              .add("--module-path", modulePath)
              .add("--module-source-path", realm.moduleSourcePath)
              .add("--module-version", project.version)
              .add("--module", String.join(",", modules)));
      for (var module : modules) {
        var unit = realm.units.get(module);
        jarModule(unit, classes);
        jarSources(unit);
      }
    }

    private void jarModule(Project.ModuleUnit unit, Path classes) {
      var module = unit.descriptor.name();
      var version = unit.descriptor.version();
      var file = module + "-" + version.orElse(project.version);
      var jar = Util.treeCreate(target.modules).resolve(file + ".jar");

      bach.run(
          new Command("jar")
              .add("--create")
              .add("--file", jar)
              .addIff(bach.verbose(), "--verbose")
              .addIff("--module-version", version)
              .addIff("--main-class", unit.descriptor.mainClass())
              .add("-C", classes.resolve(module))
              .add(".")
              .addEach(unit.resources, (cmd, path) -> cmd.add("-C", path).add(".")));

      if (bach.verbose()) {
        bach.run(new Command("jar", "--describe-module", "--file", jar));
        var runtimeModulePath = new ArrayList<>(List.of(target.modules));
        for (var other : realm.realms) {
          var otherTarget = project.target(other);
          if (Files.isDirectory(otherTarget.modules)) {
            runtimeModulePath.add(otherTarget.modules);
          }
        }
        runtimeModulePath.addAll(project.library.modulePaths);
        bach.run(
            new Command("jdeps")
                .add("--module-path", runtimeModulePath)
                .add("--multi-release", "BASE")
                .add("--check", module));
      }
    }

    private void jarSources(Project.ModuleUnit unit) {
      var version = unit.descriptor.version();
      var file = unit.descriptor.name() + "-" + version.orElse(project.version);

      bach.run(
          new Command("jar")
              .add("--create")
              .add("--file", target.directory.resolve(file + "-sources.jar"))
              .addIff(bach.verbose(), "--verbose")
              .add("--no-manifest")
              .addEach(unit.sources, (cmd, path) -> cmd.add("-C", path).add("."))
              .addEach(unit.resources, (cmd, path) -> cmd.add("-C", path).add(".")));
    }
  }

  /** Multi-release module compiler. */
  public static class Hydra {

    private final Bach bach;
    private final Project project;
    private final Project.Realm realm;
    private final Project.Target target;

    public Hydra(Bach bach, Project project, Project.Realm realm) {
      this.bach = bach;
      this.project = project;
      this.realm = realm;
      this.target = project.target(realm);
    }

    public void compile(Collection<String> modules) {
      bach.log("Generating commands for %s realm multi-release modules(s): %s", realm.name, modules);
      for (var module : modules) {
        var unit = (Project.MultiReleaseUnit) realm.units.get(module);
        compile(unit);
      }
    }

    private void compile(Project.MultiReleaseUnit unit) {
      var sorted = new TreeSet<>(unit.releases.keySet());
      int base = sorted.first();
      bach.log("Base feature release number is: %d", base);
      var classes = target.directory.resolve("hydra").resolve("classes");
      for (int release : sorted) {
        compileRelease(unit, base, release, classes);
      }
      jarModule(unit, classes);
      jarSources(unit);
    }

    private void compileRelease(Project.MultiReleaseUnit unit, int base, int release, Path classes) {
      var module = unit.descriptor.name();
      var source = unit.releases.get(release);
      var destination = classes.resolve(source.getFileName());
      var baseClasses = classes.resolve(unit.releases.get(base).getFileName()).resolve(module);
      var javac = new Command("javac").addIff(false, "-verbose").add("--release", release);
      if (Util.isModuleInfo(source.resolve("module-info.java"))) {
        javac.add("-d", destination);
        javac.add("--module-version", project.version);
        javac.add("--module-path", project.library.modulePaths);
        javac.add("--module-source-path", realm.moduleSourcePath);
        if (base != release) {
          javac.add("--patch-module", module + '=' + baseClasses);
        }
        javac.add("--module", module);
      } else {
        javac.add("-d", destination.resolve(module));
        var classPath = new ArrayList<Path>();
        if (base != release) {
          classPath.add(baseClasses);
        }
        if (Files.isDirectory(target.modules)) {
          classPath.addAll(Util.list(target.modules, Util::isJarFile));
        }
        for (var path : Util.findExisting(project.library.modulePaths)) {
          if (Util.isJarFile(path)) {
            classPath.add(path);
            continue;
          }
          classPath.addAll(Util.list(path, Util::isJarFile));
        }
        javac.add("--class-path", classPath);
        javac.addEach(Util.find(List.of(source), Util::isJavaFile));
      }
      bach.run(javac);
    }

    private void jarModule(Project.MultiReleaseUnit unit, Path classes) {
      var releases = new ArrayDeque<>(new TreeSet<>(unit.releases.keySet()));
      var module = unit.descriptor.name();
      var version = unit.descriptor.version();
      var file = module + "-" + version.orElse(project.version);
      var modularJar = Util.treeCreate(target.modules).resolve(file + ".jar");
      var base = unit.releases.get(releases.pop()).getFileName();
      var jar =
          new Command("jar")
              .add("--create")
              .add("--file", modularJar)
              .addIff(bach.verbose(), "--verbose")
              .add("-C", classes.resolve(base).resolve(module))
              .add(".")
              .addEach(unit.resources, (cmd, path) -> cmd.add("-C", path).add("."));
      for (var release : releases) {
        var path = unit.releases.get(release).getFileName();
        var released = classes.resolve(path).resolve(module);
        if (unit.copyModuleDescriptorToRootRelease == release) {
          jar.add("-C", released);
          jar.add("module-info.class");
        }
        jar.add("--release", release);
        jar.add("-C", released);
        jar.add(".");
      }
      bach.run(jar);
      if (bach.verbose()) {
        bach.run(new Command("jar", "--describe-module", "--file", modularJar));
      }
    }

    private void jarSources(Project.MultiReleaseUnit unit) {
      var releases = new ArrayDeque<>(new TreeMap<>(unit.releases).entrySet());
      var module = unit.descriptor.name();
      var version = unit.descriptor.version();
      var file = module + "-" + version.orElse(project.version);
      var jar =
          new Command("jar")
              .add("--create")
              .add("--file", target.directory.resolve(file + "-sources.jar"))
              .addIff(bach.verbose(), "--verbose")
              .add("--no-manifest")
              .add("-C", releases.removeFirst().getValue())
              .add(".")
              .addEach(unit.resources, (cmd, path) -> cmd.add("-C", path).add("."));
      for (var release : releases) {
        jar.add("--release", release.getKey());
        jar.add("-C", release.getValue());
        jar.add(".");
      }
      bach.run(jar);
    }
  }

  /** 3rd-party module resolver. */
  public static class Resolver {

    /** Command-line argument factory. */
    public static Scanner scan(Collection<String> declaredModules, Iterable<String> requires) {
      var map = new TreeMap<String, Set<Version>>();
      for (var string : requires) {
        var versionMarkerIndex = string.indexOf('@');
        var any = versionMarkerIndex == -1;
        var module = any ? string : string.substring(0, versionMarkerIndex);
        var version = any ? null : Version.parse(string.substring(versionMarkerIndex + 1));
        map.merge(module, any ? Set.of() : Set.of(version), Util::concat);
      }
      return new Scanner(new TreeSet<>(declaredModules), map);
    }

    public static Scanner scan(ModuleFinder finder) {
      var declaredModules = new TreeSet<String>();
      var requiredModules = new TreeMap<String, Set<Version>>();
      finder.findAll().stream()
          .map(ModuleReference::descriptor)
          .peek(descriptor -> declaredModules.add(descriptor.name()))
          .map(ModuleDescriptor::requires)
          .flatMap(Set::stream)
          .filter(r -> !r.modifiers().contains(ModuleDescriptor.Requires.Modifier.MANDATED))
          .filter(r -> !r.modifiers().contains(ModuleDescriptor.Requires.Modifier.STATIC))
          .distinct()
          .forEach(
              requires ->
                  requiredModules.merge(
                      requires.name(),
                      requires.compiledVersion().map(Set::of).orElse(Set.of()),
                      Util::concat));
      return new Scanner(declaredModules, requiredModules);
    }

    public static Scanner scan(String... sources) {
      var declaredModules = new TreeSet<String>();
      var map = new TreeMap<String, Set<Version>>();
      for (var source : sources) {
        var nameMatcher = Scanner.MODULE_NAME_PATTERN.matcher(source);
        if (!nameMatcher.find()) {
          throw new IllegalArgumentException("Expected module-info.java source, but got: " + source);
        }
        declaredModules.add(nameMatcher.group(1).trim());
        var requiresMatcher = Scanner.MODULE_REQUIRES_PATTERN.matcher(source);
        while (requiresMatcher.find()) {
          var name = requiresMatcher.group(1);
          var version = requiresMatcher.group(2);
          map.merge(name, version == null ? Set.of() : Set.of(Version.parse(version)), Util::concat);
        }
      }
      return new Scanner(declaredModules, map);
    }

    public static Scanner scan(Collection<Path> paths) {
      var sources = new ArrayList<String>();
      for (var path : paths) {
        if (Files.isDirectory(path)) {
          path = path.resolve("module-info.java");
        }
        try {
          sources.add(Files.readString(path));
        } catch (IOException e) {
          throw new UncheckedIOException("find or read failed: " + path, e);
        }
      }
      return scan(sources.toArray(new String[0]));
    }

    private final Bach bach;
    private final Project project;

    Resolver(Bach bach) {
      this.bach = bach;
      this.project = bach.project;
    }

    public void resolve() {
      var entries = project.library.modulePaths.toArray(Path[]::new);
      var library = scan(ModuleFinder.of(entries));
      bach.log("Library of -> %s", project.library.modulePaths);
      bach.log("  modules  -> " + library.modules);
      bach.log("  requires -> " + library.requires);

      var units = new ArrayList<Path>();
      for (var realm : project.realms) {
        for (var unit : realm.units.values()) {
          units.add(unit.info);
        }
      }
      var sources = scan(units);
      bach.log("Sources of -> %s", units);
      bach.log("  modules  -> " + sources.modules);
      bach.log("  requires -> " + sources.requires);

      var systems = scan(ModuleFinder.ofSystem());
      bach.log("System contains %d modules.", systems.modules.size());

      var missing = new TreeMap<String, Set<Version>>();
      missing.putAll(sources.requires);
      missing.putAll(library.requires);
      sources.getDeclaredModules().forEach(missing::remove);
      library.getDeclaredModules().forEach(missing::remove);
      systems.getDeclaredModules().forEach(missing::remove);
      if (missing.isEmpty()) {
        return;
      }

      var downloader = new Util.Downloader(bach.out, bach.err);
      var worker = new Scanner.Worker(project, downloader);
      do {
        bach.log("Loading missing modules: %s", missing);
        var items = new ArrayList<Util.Downloader.Item>();
        for (var entry : missing.entrySet()) {
          var module = entry.getKey();
          var versions = entry.getValue();
          items.add(worker.toTransferItem(module, versions));
        }
        var lib = project.library.modulePaths.get(0);
        downloader.download(lib, items);
        library = scan(ModuleFinder.of(entries));
        missing = new TreeMap<>(library.requires);
        library.getDeclaredModules().forEach(missing::remove);
        systems.getDeclaredModules().forEach(missing::remove);
      } while (!missing.isEmpty());
    }

    /** Module Scanner. */
    public static class Scanner {

      private static final Pattern MODULE_NAME_PATTERN =
          Pattern.compile(
              "(?:module)" // key word
                  + "\\s+([\\w.]+)" // module name
                  + "\\s+\\{"); // end marker
      private static final Pattern MODULE_REQUIRES_PATTERN =
          Pattern.compile(
              "(?:requires)" // key word
                  + "(?:\\s+[\\w.]+)?" // optional modifiers
                  + "\\s+([\\w.]+)" // module name
                  + "(?:\\s*/\\*\\s*([\\w.\\-+]+)\\s*\\*/\\s*)?" // optional '/*' version '*/'
                  + ";"); // end marker

      private final Set<String> modules;
      final Map<String, Set<Version>> requires;

      public Scanner(Set<String> modules, Map<String, Set<Version>> requires) {
        this.modules = modules;
        this.requires = requires;
      }

      public Set<String> getDeclaredModules() {
        return modules;
      }

      public Set<String> getRequiredModules() {
        return requires.keySet();
      }

      public Optional<Version> getRequiredVersion(String requiredModule) {
        var versions = requires.get(requiredModule);
        if (versions == null) {
          throw new NoSuchElementException("Module " + requiredModule + " is not mapped");
        }
        if (versions.size() > 1) {
          throw new IllegalStateException("Multiple versions: " + requiredModule + " -> " + versions);
        }
        return versions.stream().findFirst();
      }

      static class Worker {

        static class Lookup {

          final String name;
          final Properties properties;
          final Set<Pattern> patterns;
          final UnaryOperator<String> custom;

          Lookup(Util.Downloader downloader, Path lib, String name, UnaryOperator<String> custom) {
            this.name = name;
            var uri = "https://github.com/sormuras/modules/raw/master/" + name;
            var modules = Path.of(System.getProperty("user.home")).resolve(".bach/modules");
            try {
              Files.createDirectories(modules);
            } catch (IOException e) {
              throw new UncheckedIOException("Creating directories failed: " + modules, e);
            }
            var defaultModules = downloader.download(URI.create(uri), modules.resolve(name));
            var defaults = Util.load(new Properties(), defaultModules);
            this.properties = Util.load(new Properties(defaults), lib.resolve(name));
            this.patterns =
                properties.keySet().stream()
                    .map(Object::toString)
                    .filter(key -> !SourceVersion.isName(key))
                    .map(Pattern::compile)
                    .collect(Collectors.toSet());
            this.custom = custom;
          }

          String get(String key) {
            try {
              return custom.apply(key);
            } catch (UnmappedModuleException e) {
              // fall-through
            }
            var value = properties.getProperty(key);
            if (value != null) {
              return value;
            }
            for (var pattern : patterns) {
              if (pattern.matcher(key).matches()) {
                return properties.getProperty(pattern.pattern());
              }
            }
            throw new IllegalStateException("No lookup value mapped for: " + key);
          }

          @Override
          public String toString() {
            var size = properties.size();
            var names = properties.stringPropertyNames().size();
            return String.format(
                "module properties {name: %s, size: %d, names: %d}", name, size, names);
          }
        }

        final Project project;
        final Properties moduleUri;
        final Lookup moduleMaven, moduleVersion;

        Worker(Project project, Util.Downloader transfer) {
          this.project = project;
          var lib = project.library.modulePaths.get(0);
          this.moduleUri = Util.load(new Properties(), lib.resolve("module-uri.properties"));
          this.moduleMaven =
              new Lookup(
                  transfer,
                  lib,
                  "module-maven.properties",
                  project.library.mavenGroupColonArtifactMapper);
          this.moduleVersion =
              new Lookup(
                  transfer, lib, "module-version.properties", project.library.mavenVersionMapper);
        }

        private URI getModuleUri(String module) {
          try {
            return project.library.moduleMapper.apply(module);
          } catch (UnmappedModuleException e) {
            var uri = moduleUri.getProperty(module);
            if (uri == null) {
              return null;
            }
            return URI.create(uri);
          }
        }

        Util.Downloader.Item toTransferItem(String module, Set<Version> set) {
          var uri = getModuleUri(module);
          if (uri != null) {
            var file = Util.findFileName(uri);
            var version = Util.findVersion(file.orElse(""));
            return Util.Downloader.Item.of(
                uri, module + version.map(v -> '-' + v).orElse("") + ".jar");
          }
          var repository = project.library.mavenRepositoryMapper.apply(module);
          var maven = moduleMaven.get(module).split(":");
          var group = maven[0];
          var artifact = maven[1];
          var version = Util.singleton(set).map(Object::toString).orElse(moduleVersion.get(module));
          var mappedUri = toUri(repository.toString(), group, artifact, version);
          return Util.Downloader.Item.of(mappedUri, module + '-' + version + ".jar");
        }

        private URI toUri(String repository, String group, String artifact, String version) {
          var file = artifact + '-' + version + ".jar";
          var uri = String.join("/", repository, group.replace('.', '/'), artifact, version, file);
          return URI.create(uri);
        }
      }
    }
  }

  /** Create API documentation. */
  public static class Scribe {

    private final Bach bach;
    private final Project project;
    private final Project.Realm realm;
    private final Project.Target target;

    public Scribe(Bach bach, Project project, Project.Realm realm) {
      this.bach = bach;
      this.project = project;
      this.realm = realm;
      this.target = project.target(realm);
    }

    public void document() {
      document(new TreeSet<>(realm.units.keySet()));
    }

    public void document(Collection<String> modules) {
      bach.log("Compiling %s realm's documentation: %s", realm.name, modules);
      var destination = target.directory.resolve("javadoc");
      var javadoc =
          new Command("javadoc")
              .add("-d", destination)
              .add("-encoding", "UTF-8")
              .addIff(!bach.verbose(), "-quiet")
              .add("-Xdoclint:-missing")
              .add("--module-path", project.library.modulePaths)
              .add("--module-source-path", realm.moduleSourcePath);

      for (var module : realm.modules.getOrDefault("hydra", List.of())) {
        var unit = (Project.MultiReleaseUnit) realm.units.get(module);
        var base = unit.sources.get(0);
        if (!unit.info.startsWith(base)) {
          javadoc.add("--patch-module", module + "=" + base);
        }
      }

      javadoc.add("--module", String.join(",", modules));
      bach.run(javadoc);

      var nameDashVersion = project.name + '-' + project.version;
      bach.run(
          new Command("jar")
              .add("--create")
              .add("--file", target.directory.resolve(nameDashVersion + "-javadoc.jar"))
              .addIff(bach.verbose(), "--verbose")
              .add("--no-manifest")
              .add("-C", destination)
              .add("."));
    }
  }

  /** Bach consuming task. */
  public interface Task extends Consumer<Bach> {

    /** Parse passed arguments and convert them into a list of tasks. */
    static List<Task> of(Bach bach, Collection<String> args) {
      bach.log("Parsing argument(s): %s", args);
      var arguments = new ArrayDeque<>(args);
      var tasks = new ArrayList<Task>();
      var lookup = MethodHandles.publicLookup();
      var type = MethodType.methodType(void.class);
      while (!arguments.isEmpty()) {
        var name = arguments.pop();
        // Try Bach API method w/o parameter -- single argument is consumed
        try {
          try {
            lookup.findVirtual(Object.class, name, type);
          } catch (NoSuchMethodException e) {
            var handle = lookup.findVirtual(bach.getClass(), name, type);
            tasks.add(new Task.MethodHandler(name, handle));
            continue;
          }
        } catch (ReflectiveOperationException e) {
          // fall through
        }
        // Try provided tool -- all remaining arguments are consumed
        var tool = ToolProvider.findFirst(name);
        if (tool.isPresent()) {
          tasks.add(new Task.ToolRunner(tool.get(), arguments));
          break;
        }
        throw new IllegalArgumentException("Unsupported task named: " + name);
      }
      return List.copyOf(tasks);
    }

    /** MethodHandler invoking task. */
    class MethodHandler implements Task {
      private final String name;
      private final MethodHandle handle;

      MethodHandler(String name, MethodHandle handle) {
        this.name = name;
        this.handle = handle;
      }

      @Override
      public void accept(Bach bach) {
        try {
          bach.log("Invoking %s()...", name);
          handle.invokeExact(bach);
        } catch (Throwable t) {
          throw new AssertionError("Running method failed: " + handle, t);
        }
      }

      @Override
      public String toString() {
        return "MethodHandler[name=" + name + "]";
      }
    }

    /** ToolProvider running task. */
    class ToolRunner implements Task {

      private final ToolProvider tool;
      private final String name;
      private final String[] arguments;

      ToolRunner(ToolProvider tool, Collection<?> arguments) {
        this.tool = tool;
        this.name = tool.name();
        this.arguments = arguments.stream().map(Object::toString).toArray(String[]::new);
      }

      @Override
      public void accept(Bach bach) {
        var code = bach.run(tool, arguments);
        if (code != 0) {
          throw new AssertionError(name + " returned non-zero exit code: " + code);
        }
      }

      @Override
      public String toString() {
        return "ToolRunner[name=" + name + ", arguments=" + List.of(arguments) + "]";
      }
    }
  }

  /** Static helpers. */
  static class Util {

    static <E extends Comparable<E>> Set<E> concat(Set<E> one, Set<E> two) {
      return Stream.concat(one.stream(), two.stream()).collect(Collectors.toCollection(TreeSet::new));
    }

    static Optional<Method> findApiMethod(Class<?> container, String name) {
      try {
        var method = container.getMethod(name);
        return isApiMethod(method) ? Optional.of(method) : Optional.empty();
      } catch (NoSuchMethodException e) {
        return Optional.empty();
      }
    }

    static List<Path> findExisting(Collection<Path> paths) {
      return paths.stream().filter(Files::exists).collect(Collectors.toList());
    }

    static List<Path> findExistingDirectories(Collection<Path> directories) {
      return directories.stream().filter(Files::isDirectory).collect(Collectors.toList());
    }

    static boolean isApiMethod(Method method) {
      if (method.getDeclaringClass().equals(Object.class)) return false;
      if (Modifier.isStatic(method.getModifiers())) return false;
      return method.getParameterCount() == 0;
    }

    /** List all paths matching the given filter starting at given root paths. */
    static List<Path> find(Collection<Path> roots, Predicate<Path> filter) {
      var files = new ArrayList<Path>();
      for (var root : roots) {
        try (var stream = Files.walk(root)) {
          stream.filter(filter).forEach(files::add);
        } catch (Exception e) {
          throw new Error("Walking directory '" + root + "' failed: " + e, e);
        }
      }
      return List.copyOf(files);
    }

    /** Test supplied path for pointing to a Java source compilation unit. */
    static boolean isJavaFile(Path path) {
      if (Files.isRegularFile(path)) {
        var name = path.getFileName().toString();
        if (name.endsWith(".java")) {
          return name.indexOf('.') == name.length() - 5; // single dot in filename
        }
      }
      return false;
    }

    /** Test supplied path for pointing to a Java source compilation unit. */
    static boolean isJarFile(Path path) {
      return Files.isRegularFile(path) && path.getFileName().toString().endsWith(".jar");
    }

    static boolean isModuleInfo(Path path) {
      return Files.isRegularFile(path) && path.getFileName().toString().equals("module-info.java");
    }

    static List<Path> list(Path directory) {
      return list(directory, __ -> true);
    }

    static List<Path> list(Path directory, Predicate<Path> filter) {
      try (var stream = Files.list(directory)) {
        return stream.filter(filter).sorted().collect(Collectors.toList());
      } catch (IOException e) {
        throw new UncheckedIOException("list directory failed: " + directory, e);
      }
    }

    static Properties load(Properties properties, Path path) {
      if (Files.isRegularFile(path)) {
        try (var reader = Files.newBufferedReader(path)) {
          properties.load(reader);
        } catch (IOException e) {
          throw new UncheckedIOException("Reading properties failed: " + path, e);
        }
      }
      return properties;
    }

    /** Extract last path element from the supplied uri. */
    static Optional<String> findFileName(URI uri) {
      var path = uri.getPath();
      return path == null ? Optional.empty() : Optional.of(path.substring(path.lastIndexOf('/') + 1));
    }

    static Optional<String> findVersion(String jarFileName) {
      if (!jarFileName.endsWith(".jar")) return Optional.empty();
      var name = jarFileName.substring(0, jarFileName.length() - 4);
      var matcher = Pattern.compile("-(\\d+(\\.|$))").matcher(name);
      return (matcher.find()) ? Optional.of(name.substring(matcher.start() + 1)) : Optional.empty();
    }

    static <C extends Collection<?>> C requireNonEmpty(C collection, String name) {
      if (requireNonNull(collection, name + " must not be null").isEmpty()) {
        throw new IllegalArgumentException(name + " must not be empty");
      }
      return collection;
    }

    static <T> T requireNonNull(T object, String name) {
      return Objects.requireNonNull(object, name + " must not be null");
    }

    static <T> Optional<T> singleton(Collection<T> collection) {
      if (collection.isEmpty()) {
        return Optional.empty();
      }
      if (collection.size() != 1) {
        throw new IllegalStateException("Too many elements: " + collection);
      }
      return Optional.of(collection.iterator().next());
    }

    /** Sleep and silently clear current thread's interrupted status. */
    static void sleep(long millis) {
      try {
        Thread.sleep(millis);
      } catch (InterruptedException e) {
        Thread.interrupted();
      }
    }

    /** @see Files#createDirectories(Path, FileAttribute[]) */
    static Path treeCreate(Path path) {
      try {
        Files.createDirectories(path);
      } catch (IOException e) {
        throw new UncheckedIOException("create directories failed: " + path, e);
      }
      return path;
    }

    /** Delete all files and directories from and including the root directory. */
    static void treeDelete(Path root) {
      treeDelete(root, __ -> true);
    }

    /** Delete selected files and directories from and including the root directory. */
    static void treeDelete(Path root, Predicate<Path> filter) {
      if (filter.test(root)) { // trivial case: delete existing empty directory or single file
        try {
          Files.deleteIfExists(root);
          return;
        } catch (IOException ignored) {
          // fall-through
        }
      }
      try (var stream = Files.walk(root)) { // default case: walk the tree...
        var selected = stream.filter(filter).sorted((p, q) -> -p.compareTo(q));
        for (var path : selected.collect(Collectors.toList())) {
          Files.deleteIfExists(path);
        }
      } catch (IOException e) {
        throw new UncheckedIOException("tree delete failed: " + root, e);
      }
    }

    /** File transfer utility. */
    static class Downloader {

      static class Item {

        static Item of(URI uri, String file) {
          return new Item(uri, file);
        }

        private final URI uri;
        private final String file;

        private Item(URI uri, String file) {
          this.uri = uri;
          this.file = file;
        }
      }

      private final PrintWriter out, err;
      private final HttpClient client;

      Downloader(PrintWriter out, PrintWriter err) {
        this(out, err, HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build());
      }

      private Downloader(PrintWriter out, PrintWriter err, HttpClient client) {
        this.out = out;
        this.err = err;
        this.client = client;
      }

      Set<Path> download(Path directory, Collection<Item> items) {
        Util.treeCreate(directory);
        return items.stream()
            .parallel()
            .map(item -> download(item.uri, directory.resolve(item.file)))
            .collect(Collectors.toCollection(TreeSet::new));
      }

      Path download(URI uri, Path path) {
        if ("file".equals(uri.getScheme())) {
          try {
            return Files.copy(Path.of(uri), path, StandardCopyOption.REPLACE_EXISTING);
          } catch (Exception e) {
            throw new IllegalArgumentException("copy file failed:" + uri, e);
          }
        }
        var request = HttpRequest.newBuilder(uri).GET();
        if (Files.exists(path)) {
          try {
            var etagBytes = (byte[]) Files.getAttribute(path, "user:etag");
            var etag = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(etagBytes)).toString();
            request.setHeader("If-None-Match", etag);
          } catch (Exception e) {
            err.println("Couldn't get 'user:etag' file attribute: " + e);
          }
        }
        try {
          var handler = HttpResponse.BodyHandlers.ofFile(path);
          var response = client.send(request.build(), handler);
          if (response.statusCode() == 200) {
            var etagHeader = response.headers().firstValue("etag");
            if (etagHeader.isPresent()) {
              try {
                var etag = etagHeader.get();
                Files.setAttribute(path, "user:etag", StandardCharsets.UTF_8.encode(etag));
              } catch (Exception e) {
                err.println("Couldn't set 'user:etag' file attribute: " + e);
              }
            }
            var lastModifiedHeader = response.headers().firstValue("last-modified");
            if (lastModifiedHeader.isPresent()) {
              try {
                var format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
                var millis = format.parse(lastModifiedHeader.get()).getTime(); // 0 means "unknown"
                var fileTime = FileTime.fromMillis(millis == 0 ? System.currentTimeMillis() : millis);
                Files.setLastModifiedTime(path, fileTime);
              } catch (Exception e) {
                err.println("Couldn't set last modified file attribute: " + e);
              }
            }
            synchronized (out) {
              out.println(path + " <- " + uri);
            }
          }
        } catch (IOException | InterruptedException e) {
          err.println("Failed to load: " + uri + " -> " + e);
          e.printStackTrace(err);
        }
        return path;
      }
    }
  }

  /** Unchecked exception thrown when a module name is not mapped. */
  public static class UnmappedModuleException extends IllegalStateException {

    public static String throwForString(String module) {
      throw new UnmappedModuleException(module);
    }

    public static URI throwForURI(String module) {
      throw new UnmappedModuleException(module);
    }

    private static final long serialVersionUID = 6985648789039587477L;

    public UnmappedModuleException(String module) {
      super("Module " + module + " is not mapped");
    }
  }
}
