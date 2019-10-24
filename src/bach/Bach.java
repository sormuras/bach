// THIS FILE WAS GENERATED ON 2019-10-24T06:21:17.959859300Z
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
import java.lang.module.ModuleDescriptor.Requires;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.lang.model.SourceVersion;

public class Bach {

  public static String VERSION = "1.9-ea";

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
      } else {
        var causes = new ArrayDeque<>();
        var cause = throwable;
        while (cause != null && !causes.contains(cause)) {
          causes.add(cause);
          cause = cause.getCause();
        }
        bach.err.println(causes.getLast());
      }
    }
  }

  /** Text-output writer. */
  private final PrintWriter out, err;
  /** Be verbose. */
  private final boolean verbose;
  /** Project to be built. */
  private final Project project;
  /** Summary. */
  final Summary summary;

  /** Initialize default instance. */
  public Bach() {
    this(Log.ofSystem(), Project.of(Path.of("")));
  }

  /** Initialize. */
  public Bach(Log log, Project project) {
    this(log.out, log.err, log.verbose, project);
  }

  /** Initialize. */
  public Bach(PrintWriter out, PrintWriter err, boolean verbose, Project project) {
    this.out = Util.requireNonNull(out, "out");
    this.err = Util.requireNonNull(err, "err");
    this.verbose = verbose;
    this.project = project;
    this.summary = new Summary();
    log("New Bach.java (%s) instance initialized: %s", VERSION, this);
  }

  /** Print "debug" message to the standard output stream. */
  void log(String format, Object... args) {
    if (verbose) out.println(String.format(format, args));
  }

  /** Print "warning" message to the error output stream. */
  void warn(String format, Object... args) {
    err.println(String.format(format, args));
  }

  /** Non-static entry-point used by {@link #main(String...)} and {@code BachToolProvider}. */
  void main(List<String> arguments) {
    var tasks = Util.requireNonEmpty(Task.of(this, arguments), "tasks");
    log("Running %d argument task(s): %s", tasks.size(), tasks);
    tasks.forEach(consumer -> consumer.accept(this));
  }

  /** Run the passed command. */
  void run(Command command) {
    var name = command.getName();
    var tool = ToolProvider.findFirst(name);
    int code = run(tool.orElseThrow(() -> new RuntimeException(name)), command.toStringArray());
    if (code != 0) {
      throw new AssertionError("Running command failed: " + command);
    }
  }

  /** Run the tool using the passed provider and arguments. */
  int run(ToolProvider tool, String... arguments) {
    var line = tool.name() + (arguments.length == 0 ? "" : ' ' + String.join(" ", arguments));
    log("Running %s", line);
    var start = Instant.now();
    int code = tool.run(out, err, arguments);
    var duration = Duration.between(start, Instant.now());
    summary.runs.add(String.format("%d %6s ms %s", code, duration.toMillis(), line));
    return code;
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
  public void build() throws Exception {
    info();

    resolve();

    var units = project.realms.stream().map(realm -> realm.units).mapToLong(Collection::size).sum();
    if (units == 0) {
      throw new AssertionError("No units declared: " + project.realms);
    }

    // compile := javac + jar
    var realms = new ArrayDeque<>(project.realms);
    var main = realms.removeFirst();
    compile(main);
    for (var remaining : realms) {
      compile(remaining);
    }

    // test
    for (var remaining : realms) {
      new Tester(this, remaining).test();
    }

    // document := javadoc + deploy
    if (!main.units.isEmpty()) {
      var scribe = new Scribe(this, project, main);
      scribe.document();
      scribe.generateMavenInstallScript();
      if (main.toolArguments.deployment().isPresent()) {
        scribe.generateMavenDeployScript();
      }
    }

    summary(main);
  }

  private void compile(Project.Realm realm) {
    if (realm.units.isEmpty()) {
      return;
    }
    var hydras = realm.names(true);
    if (!hydras.isEmpty()) {
      new Hydra(this, project, realm).compile(hydras);
    }
    var jigsaws = realm.names(false);
    if (!jigsaws.isEmpty()) {
      new Jigsaw(this, project, realm).compile(jigsaws);
    }
  }

  /** Print summary. */
  public void summary(Project.Realm realm) throws Exception {
    out.println();
    out.printf("+---%n");
    out.printf("| Summary for project %s %s%n", project.name, project.version);
    out.printf("+---%n");
    out.println();
    var target = project.target(realm);
    var modulePath = project.modulePaths(target);
    var names = String.join(",", realm.names());
    var deps = new Command("jdeps").add("--module-path", modulePath).add("--multi-release", "BASE");
    run(
        deps.clone()
            .add("-summary")
            .add("--dot-output", target.directory.resolve("jdeps"))
            .add("--add-modules", names));
    if (verbose) {
      run(deps.clone().add("--check", names));
    }
    out.printf("Commands%n");
    var summaryBatch = ("summary-commands-" + Instant.now() + ".bat").replace(':', '-');
    Files.write(project.targetDirectory.resolve(summaryBatch), summary.runs);
    summary.runs.forEach(line -> out.printf("%.120s%s%n", line, line.length() <= 120 ? "" : "..."));
    out.println();
    out.printf("Modules%n");
    var jars = Util.list(target.modules, Util::isJarFile);
    out.printf("%d jar(s) found in: %s%n", jars.size(), target.modules.toUri());
    for (var jar : jars) {
      out.printf("%,11d %s%n", Files.size(jar), jar.getFileName());
    }
  }

  /** Print all "interesting" information. */
  public void info() {
    out.printf("Bach.java (%s)%n", VERSION);
    out.printf("+---%n");
    out.printf("| Project %s %s%n", project.name, project.version);
    out.printf("+---%n");
    try {
      for (var field : project.getClass().getFields()) {
        out.printf("  %s = %s%n", field.getName(), field.get(project));
      }
      for (var realm : project.realms) {
        out.printf("+ Realm %s%n", realm.name);
        for (var field : realm.getClass().getFields()) {
          out.printf("  %s.%s = %s%n", realm.name, field.getName(), field.get(realm));
        }
        for (var unit : realm.units) {
          out.printf("- ModuleUnit %s%n", unit.name());
          for (var field : unit.getClass().getFields()) {
            out.printf("  (%s).%s = %s%n", unit.name(), field.getName(), field.get(unit));
          }
        }
      }
    } catch (ReflectiveOperationException e) {
      e.printStackTrace(err);
    }
  }

  /** Resolve missing modules. */
  public void resolve() throws Exception {
    new Resolver(this).resolve();
  }

  /** Print Bach.java's version to the standard output stream. */
  public void version() {
    out.println(VERSION);
  }

  private static class Summary {
    final List<String> runs = new ArrayList<>();
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

    /** Initialize Command instance with zero or more arguments. */
    public Command(String name, Iterable<?> arguments) {
      this.name = name;
      addEach(arguments);
    }

    @Override
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public Command clone() {
      return new Command(name, arguments);
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

    /** Add all arguments by invoking {@link #add(Object)} for each element. */
    public Command addEach(Stream<?> arguments) {
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
    public static Project of(Path base) {
      return ProjectBuilder.build(base);
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

    /** Compute module path for the passed realm. */
    public List<Path> modulePaths(Target target, Path... initialPaths) {
      var paths = new ArrayList<>(List.of(initialPaths));
      if (Files.isDirectory(target.modules)) {
        paths.add(target.modules);
      }
      for (var other : target.realm.realms) {
        var otherTarget = target(other);
        if (Files.isDirectory(otherTarget.modules)) {
          paths.add(otherTarget.modules);
        }
      }
      paths.addAll(Util.findExistingDirectories(library.modulePaths));
      return List.copyOf(paths);
    }

    /** Collection of directories and other realm-specific assets. */
    public class Target {
      /** Associated realm. */
      public final Realm realm;
      /** Base target directory of the realm. */
      public final Path directory;
      /** Directory of modular JAR files. */
      public final Path modules;

      private Target(Realm realm) {
        this.realm = realm;
        this.directory = targetDirectory.resolve("realm").resolve(realm.name);
        this.modules = directory.resolve("modules");
      }

      /** Return base file name for the passed module unit. */
      public String file(ModuleUnit unit) {
        var descriptor = unit.info.descriptor();
        return descriptor.name() + "-" + descriptor.version().orElse(version);
      }

      /** Return file name for the passed module unit. */
      public String file(ModuleUnit unit, String extension) {
        return file(unit) + extension;
      }

      /** Return modular JAR file path for the passed module unit. */
      public Path modularJar(ModuleUnit unit) {
        return modules.resolve(file(unit, ".jar"));
      }

      /** Return sources JAR file path for the passed module unit. */
      public Path sourcesJar(ModuleUnit unit) {
        return directory.resolve(file(unit, "-sources.jar"));
      }
    }

    public Target target(Realm realm) {
      return new Target(realm);
    }

    /** Manage external 3rd-party modules. */
    public static class Library {

      public static String defaultRepository(String group, String version) {
        return version.endsWith("SNAPSHOT")
            ? "https://oss.sonatype.org/content/repositories/snapshots"
            : "https://repo1.maven.org/maven2";
      }

      /** List of library paths to external 3rd-party modules. */
      public final List<Path> modulePaths;
      /** Map external 3rd-party module names to their {@code URI}s. */
      public final Function<String, URI> moduleMapper;
      /** Map Maven group ID and version to their Maven repository. */
      public final BinaryOperator<String> mavenRepositoryMapper;
      /** Map external 3rd-party module names to their colon-separated Maven Group and Artifact ID. */
      public final UnaryOperator<String> mavenGroupColonArtifactMapper;
      /** Map external 3rd-party module names to their Maven version. */
      public final UnaryOperator<String> mavenVersionMapper;

      public Library(Path lib) {
        this(
            List.of(lib),
            UnmappedModuleException::throwForURI,
            Library::defaultRepository,
            UnmappedModuleException::throwForString,
            UnmappedModuleException::throwForString);
      }

      public Library(
          List<Path> modulePaths,
          Function<String, URI> moduleMapper,
          BinaryOperator<String> mavenRepositoryMapper,
          UnaryOperator<String> mavenGroupColonArtifactMapper,
          UnaryOperator<String> mavenVersionMapper) {
        this.modulePaths = List.copyOf(Util.requireNonEmpty(modulePaths, "modulePaths"));
        this.moduleMapper = moduleMapper;
        this.mavenRepositoryMapper = mavenRepositoryMapper;
        this.mavenGroupColonArtifactMapper = mavenGroupColonArtifactMapper;
        this.mavenVersionMapper = mavenVersionMapper;
      }
    }

    /** Source-based module reference. */
    public static class ModuleInfo extends ModuleReference {

      /** Module compilation unit parser. */
      public static ModuleInfo of(Path info) {
        if (!Util.isModuleInfo(info)) {
          throw new IllegalArgumentException("Expected module-info.java path, but got: " + info);
        }
        try {
          return new ModuleInfo(Modules.describe(Files.readString(info)), info);
        } catch (IOException e) {
          throw new UncheckedIOException("Reading module declaration failed: " + info, e);
        }
      }

      /** Path to the backing {@code module-info.java} file. */
      public final Path path;
      /** Module source path. */
      public final String moduleSourcePath;

      private ModuleInfo(ModuleDescriptor descriptor, Path path) {
        super(descriptor, path.toUri());
        this.path = path;
        this.moduleSourcePath = Modules.moduleSourcePath(path, descriptor.name());
      }

      @Override
      public ModuleReader open() {
        throw new UnsupportedOperationException("Can't open a module-info.java file for reading");
      }
    }

    /** Single source path with optional release directive. */
    public static class Source {
      /** Source-specific flag. */
      public enum Flag {
        /** Store binary assets in {@code META-INF/versions/${release}/} directory of the jar. */
        VERSIONED
      }

      /** Create default non-targeted source for the specified path. */
      public static Source of(Path path) {
        return new Source(path, 0, Set.of());
      }

      /** Create targeted source for the specified path, the release, and optional flags. */
      public static Source of(Path path, int release, Flag... flags) {
        return new Source(path, release, Util.concat(Set.of(Flag.VERSIONED), Set.of(flags)));
      }

      /** Source path. */
      public final Path path;
      /** Java feature release target number, with zero indicating the current runtime release. */
      public final int release;
      /** Optional flags. */
      public final Set<Flag> flags;

      public Source(Path path, int release, Set<Flag> flags) {
        this.path = path;
        this.release = release;
        this.flags = Set.copyOf(flags);
      }

      public boolean isTargeted() {
        return release != 0;
      }

      public boolean isVersioned() {
        return flags.contains(Flag.VERSIONED);
      }
    }

    /** Java module source unit. */
    public static class ModuleUnit {

      /** Create default unit for the specified path. */
      public static ModuleUnit of(Path path) {
        var info = ModuleInfo.of(path.resolve("module-info.java"));
        var sources = List.of(Source.of(path));
        var parent = path.getParent();
        var resources = Util.findExistingDirectories(List.of(parent.resolve("resources")));
        var pom = parent.resolve("maven").resolve("pom.xml");
        return new ModuleUnit(info, sources, resources, pom);
      }

      /** Source-based module reference. */
      public final ModuleInfo info;
      /** Paths to the source directories. */
      public final List<Source> sources;
      /** Paths to the resource directories. */
      public final List<Path> resources;
      /** Path to the associated Maven POM file. */
      public final Path mavenPom;

      public ModuleUnit(ModuleInfo info, List<Source> sources, List<Path> resources, Path mavenPom) {
        this.info = Util.requireNonNull(info, "info");
        this.sources = List.copyOf(sources);
        this.resources = List.copyOf(resources);
        this.mavenPom = mavenPom;
      }

      public boolean isMultiRelease() {
        return sources.stream().allMatch(Source::isTargeted);
      }

      public String name() {
        return info.descriptor().name();
      }

      public String path() {
        return info.moduleSourcePath;
      }

      public Optional<Path> mavenPom() {
        return Files.isRegularFile(mavenPom) ? Optional.of(mavenPom) : Optional.empty();
      }
    }

    /** Realm-specific tool argument collector. */
    public static class ToolArguments {

      public static final List<String> JAVAC =
          List.of("-encoding", "UTF-8", "-parameters", "-Werror", "-Xlint");

      public static final List<String> JAVAC_PREVIEW =
          List.of("-encoding", "UTF-8", "-parameters", "-Werror", "-Xlint:-preview");

      public static ToolArguments of() {
        return new ToolArguments(JAVAC, null);
      }

      /** Option values passed to all {@code javac} calls. */
      public final List<String> javac;
      /** Arguments used for uploading modules, may be {@code null}. */
      public final Deployment deployment;

      public ToolArguments(List<String> javac, Deployment deployment) {
        this.javac = List.copyOf(javac);
        this.deployment = deployment;
      }

      public Optional<Deployment> deployment() {
        return Optional.ofNullable(deployment);
      }
    }

    /** Properties used to upload compiled modules. */
    public static class Deployment {
      /** Maven repository id. */
      public final String mavenRepositoryId;
      /** Maven URL as an URI. */
      public final URI mavenUri;

      public Deployment(String mavenRepositoryId, URI mavenUri) {
        this.mavenRepositoryId = mavenRepositoryId;
        this.mavenUri = mavenUri;
      }
    }

    /** Main- and test realms. */
    public static class Realm {

      /** Single module realm factory. */
      public static Realm of(String name, ModuleUnit unit, Realm... realms) {
        var moduleSourcePath = unit.info.moduleSourcePath;
        return new Realm(name, false, 0, moduleSourcePath, ToolArguments.of(), List.of(unit), realms);
      }

      /** Multi-module realm factory. */
      public static Realm of(String name, List<ModuleUnit> units, Realm... realms) {
        var distinctPaths = units.stream().map(ModuleUnit::path).distinct();
        var moduleSourcePath = distinctPaths.collect(Collectors.joining(File.pathSeparator));
        return new Realm(name, false, 0, moduleSourcePath, ToolArguments.of(), units, realms);
      }

      /** Name of the realm. */
      public final String name;
      /** Enable preview features. */
      public final boolean preview;
      /** Java feature release target number, with zero indicating the current runtime release. */
      public final int release;
      /** Module source path specifies where to find input source files for multiple modules. */
      public final String moduleSourcePath;
      /** Option values passed to various tools. */
      public final ToolArguments toolArguments;
      /** Map of all declared module source unit. */
      public final List<ModuleUnit> units;
      /** List of required realms. */
      public final List<Realm> realms;

      public Realm(
          String name,
          boolean preview,
          int release,
          String moduleSourcePath,
          ToolArguments toolArguments,
          List<ModuleUnit> units,
          Realm... realms) {
        this.name = name;
        this.preview = preview;
        this.release = release;
        this.moduleSourcePath = moduleSourcePath;
        this.toolArguments = toolArguments;
        this.units = units;
        this.realms = List.of(realms);
      }

      Optional<ModuleUnit> unit(String name) {
        return units.stream().filter(unit -> unit.name().equals(name)).findAny();
      }

      /** Names of all modules declared in this realm. */
      List<String> names() {
        return units.stream().map(ModuleUnit::name).collect(Collectors.toList());
      }

      /** Names of modules declared in this realm of the passed type. */
      List<String> names(boolean multiRelease) {
        return units.stream()
            .filter(unit -> unit.isMultiRelease() == multiRelease)
            .map(ModuleUnit::name)
            .collect(Collectors.toList());
      }

      public List<ModuleUnit> units(Predicate<ModuleUnit> filter) {
        return units.stream().filter(filter).collect(Collectors.toList());
      }
    }
  }

  /** Build project. */
  public static class ProjectBuilder {

    /** Supported properties. */
    public enum Property {
      /** Name of the project. */
      NAME("project"),

      /** Version of the project, consumable by {@link Version#parse(String)}. */
      VERSION("0"),

      /** Directory that contains all modules. */
      SRC_PATH("src");

      public final String key;
      public final String defaultValue;

      Property(String defaultValue) {
        this.key = name().replace('_', '-').toLowerCase();
        this.defaultValue = defaultValue;
      }
    }

    /** Create default project scanning the passed base directory. */
    public static Project build(Path base) {
      if (!Files.isDirectory(base)) {
        throw new IllegalArgumentException("Expected a directory but got: " + base);
      }
      return new Scanner(base).project();
    }

    static class Scanner {

      private final Path base;
      private final Properties properties;

      Scanner(Path base) {
        this.base = base;
        this.properties = Util.load(new Properties(), base.resolve(".bach").resolve(".properties"));
      }

      String get(Property property) {
        return get(property, property.defaultValue);
      }

      String get(Property property, String defaultValue) {
        return System.getProperty(property.key, properties.getProperty(property.key, defaultValue));
      }

      Project.ModuleInfo info(Path path) {
        for (var directory : List.of("java", "module")) {
          var info = path.resolve(directory).resolve("module-info.java");
          if (Util.isModuleInfo(info)) {
            return Project.ModuleInfo.of(info);
          }
        }
        throw new IllegalArgumentException("Couldn't find module-info.java file in: " + path);
      }

      List<Project.ModuleUnit> units(Path src, String realm) {
        var units = new ArrayList<Project.ModuleUnit>();
        for (var module : Util.list(src, Files::isDirectory)) {
          var path = module.resolve(realm);
          if (Files.notExists(path)) {
            continue;
          }
          // jigsaw
          if (Files.isDirectory(path.resolve("java"))) {
            var info = info(path);
            var sources = List.of(Project.Source.of(path.resolve("java")));
            var resources = Util.findExistingDirectories(List.of(path.resolve("resources")));
            var mavenPom = path.resolve("maven").resolve("pom.xml");
            units.add(new Project.ModuleUnit(info, sources, resources, mavenPom));
            continue;
          }
          // multi-release
          if (!Util.list(path, "java-*").isEmpty()) {
            Project.ModuleInfo info = null;
            var sources = new ArrayList<Project.Source>();
            for (int feature = 7; feature <= Runtime.version().feature(); feature++) {
              var sourced = path.resolve("java-" + feature);
              if (Files.notExists(sourced)) {
                continue;
              }
              sources.add(Project.Source.of(sourced, feature));
              var infoPath = sourced.resolve("module-info.java");
              if (info == null && Util.isModuleInfo(infoPath)) { // select first
                info = Project.ModuleInfo.of(infoPath);
              }
            }
            var resources = Util.findExistingDirectories(List.of(path.resolve("resources")));
            var mavenPom = path.resolve("maven").resolve("pom.xml");
            units.add(new Project.ModuleUnit(info, sources, resources, mavenPom));
            continue;
          }
          throw new IllegalStateException("Failed to scan module: " + module);
        }
        return units;
      }

      Project.Realm realm(String name, Project.Realm... realms) {
        var units = units(base.resolve(get(Property.SRC_PATH)), name);
        return Project.Realm.of(name, units, realms);
      }

      Project project() {
        var main = realm("main");
        var test = realm("test", main);
        return new Project(
            base,
            base.resolve("bin"),
            get(Property.NAME, Util.findFileName(base).orElse(Property.NAME.defaultValue)),
            Version.parse(get(Property.VERSION)),
            new Project.Library(base.resolve("lib")),
            List.of(main, test));
      }
    }
  }

  /** Simplistic logging support. */
  public static class Log {

    /** Create new Log instance using system default text output streams. */
    public static Log ofSystem() {
      var verbose = Boolean.getBoolean("verbose");
      var debug = Boolean.getBoolean("ebug") || "".equals(System.getProperty("ebug"));
      return ofSystem(verbose || debug);
    }

    /** Create new Log instance using system default text output streams. */
    public static Log ofSystem(boolean verbose) {
      return new Log(new PrintWriter(System.out, true), new PrintWriter(System.err, true), verbose);
    }

    /** Text-output writer. */
    private final PrintWriter out, err;
    /** Be verbose. */
    private final boolean verbose;

    public Log(PrintWriter out, PrintWriter err, boolean verbose) {
      this.out = out;
      this.err = err;
      this.verbose = verbose;
    }

    /** Print "debug" message to the standard output stream. */
    public void debug(String format, Object... args) {
      if (verbose) out.println(String.format(format, args));
    }

    /** Print "information" message to the standard output stream. */
    public void info(String format, Object... args) {
      out.println(String.format(format, args));
    }

    /** Print "warn" message to the error output stream. */
    public void warn(String format, Object... args) {
      err.println(String.format(format, args));
    }
  }

  /** Maven 2 repository support. */
  public static class Maven {

    public static class Lookup implements UnaryOperator<String> {

      final UnaryOperator<String> custom;
      final Map<String, String> library;
      final Set<Pattern> libraryPatterns;
      final Map<String, String> fallback;

      public Lookup(
          UnaryOperator<String> custom, Map<String, String> library, Map<String, String> fallback) {
        this.custom = custom;
        this.library = library;
        this.fallback = fallback;
        this.libraryPatterns =
            library.keySet().stream()
                .map(Object::toString)
                .filter(key -> !SourceVersion.isName(key))
                .map(Pattern::compile)
                .collect(Collectors.toSet());
      }

      @Override
      public String apply(String module) {
        try {
          var custom = this.custom.apply(module);
          if (custom != null) {
            return custom;
          }
        } catch (UnmappedModuleException e) {
          // fall-through
        }
        var library = this.library.get(module);
        if (library != null) {
          return library;
        }
        if (libraryPatterns.size() > 0) {
          for (var pattern : libraryPatterns) {
            if (pattern.matcher(module).matches()) {
              return this.library.get(pattern.pattern());
            }
          }
        }
        var fallback = this.fallback.get(module);
        if (fallback != null) {
          return fallback;
        }
        throw new UnmappedModuleException(module);
      }
    }

    private final Log log;
    private final Resources resources;
    private final Lookup groupArtifacts;
    private final Lookup versions;

    public Maven(Log log, Resources resources, Lookup groupArtifacts, Lookup versions) {
      this.log = log;
      this.resources = resources;
      this.groupArtifacts = groupArtifacts;
      this.versions = versions;
    }

    public String lookup(String module) {
      return lookup(module, versions.apply(module));
    }

    public String lookup(String module, String version) {
      return groupArtifacts.apply(module) + ':' + version;
    }

    public URI toUri(String repository, String group, String artifact, String version) {
      return toUri(repository, group, artifact, version, "", "jar");
    }

    public URI toUri(
        String repository,
        String group,
        String artifact,
        String version,
        String classifier,
        String type) {
      var versionAndClassifier = classifier.isBlank() ? version : version + '-' + classifier;
      var file = artifact + '-' + versionAndClassifier + '.' + type;
      if (version.endsWith("SNAPSHOT")) {
        var base = String.join("/", repository, group.replace('.', '/'), artifact, version);
        var xml = URI.create(base + "/maven-metadata.xml");
        try {
          var meta = resources.read(xml);
          var timestamp = substring(meta, "<timestamp>", "<");
          var buildNumber = substring(meta, "<buildNumber>", "<");
          var replacement = timestamp + '-' + buildNumber;
          log.debug("%s:%s:%s -> %s", group, artifact, version, replacement);
          file = file.replace("SNAPSHOT", replacement);
        } catch (Exception e) {
          log.warn("Maven metadata extraction from %s failed: %s", xml, e);
        }
      }
      var uri = String.join("/", repository, group.replace('.', '/'), artifact, version, file);
      return URI.create(uri);
    }

    /** Extract substring between begin and end tags. */
    static String substring(String string, String beginTag, String endTag) {
      int beginIndex = string.indexOf(beginTag) + beginTag.length();
      int endIndex = string.indexOf(endTag, beginIndex);
      return string.substring(beginIndex, endIndex).trim();
    }
  }

  /** Static helper for modules and their friends. */
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

    private Modules() {}

    /** Module descriptor parser. */
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

    /** Compute module's source path. */
    public static String moduleSourcePath(Path path, String module) {
      var directory = Files.isDirectory(path) ? path : Objects.requireNonNull(path.getParent());
      if (Files.notExists(directory.resolve("module-info.java"))) {
        throw new IllegalArgumentException("No 'module-info.java' file found in: " + directory);
      }
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
        return String.join(File.separator, elements.collect(Collectors.toList()));
      }
      throw new IllegalArgumentException("Ambiguous module source path: " + path);
    }
  }

  public static class Jigsaw {

    private final Bach bach;
    private final Project project;
    private final Project.Realm realm;
    private final Project.Target target;
    private final Path classes;

    public Jigsaw(Bach bach, Project project, Project.Realm realm) {
      this.bach = bach;
      this.project = project;
      this.realm = realm;
      this.target = project.target(realm);
      this.classes = target.directory.resolve("jigsaw").resolve("classes");
    }

    public void compile(Collection<String> modules) {
      bach.log("Compiling %s realm jigsaw modules: %s", realm.name, modules);
      bach.run(
          new Command("javac")
              .addEach(realm.toolArguments.javac)
              .add("-d", classes)
              .addIff(realm.preview, "--enable-preview")
              .addIff(realm.release != 0, "--release", realm.release)
              .add("--module-path", project.modulePaths(target))
              .add("--module-source-path", realm.moduleSourcePath)
              .add("--module-version", project.version)
              .addEach(patches(modules))
              .add("--module", String.join(",", modules)) //
          );
      for (var module : modules) {
        var unit = realm.unit(module).orElseThrow();
        jarModule(unit);
        jarSources(unit);
      }
    }

    private List<String> patches(Collection<String> modules) {
      var patches = new Command("<patches>");
      for (var module : modules) {
        var other =
            realm.realms.stream()
                .flatMap(r -> r.units.stream())
                .filter(u -> u.name().equals(module))
                .findFirst();
        other.ifPresent(
            unit ->
                patches.add(
                    "--patch-module", unit.sources.stream().map(s -> s.path), v -> module + "=" + v));
      }
      return patches.getArguments();
    }

    private void jarModule(Project.ModuleUnit unit) {
      var descriptor = unit.info.descriptor();
      bach.run(
          new Command("jar")
              .add("--create")
              .add("--file", Util.treeCreate(target.modules).resolve(target.file(unit, ".jar")))
              .addIff(bach.verbose(), "--verbose")
              .addIff("--module-version", descriptor.version())
              .addIff("--main-class", descriptor.mainClass())
              .add("-C", classes.resolve(descriptor.name()))
              .add(".")
              .addEach(unit.resources, (cmd, path) -> cmd.add("-C", path).add(".")));

      if (bach.verbose()) {
        bach.run(new Command("jar", "--describe-module", "--file", target.modularJar(unit)));
      }
    }

    private void jarSources(Project.ModuleUnit unit) {
      bach.run(
          new Command("jar")
              .add("--create")
              .add("--file", target.sourcesJar(unit))
              .addIff(bach.verbose(), "--verbose")
              .add("--no-manifest")
              .addEach(unit.sources, (cmd, source) -> cmd.add("-C", source.path).add("."))
              .addEach(unit.resources, (cmd, path) -> cmd.add("-C", path).add(".")));
    }
  }

  /** Multi-release module compiler. */
  public static class Hydra {

    private final Bach bach;
    private final Project project;
    private final Project.Realm realm;
    private final Project.Target target;
    private final Path classes;

    public Hydra(Bach bach, Project project, Project.Realm realm) {
      this.bach = bach;
      this.project = project;
      this.realm = realm;
      this.target = project.target(realm);
      this.classes = target.directory.resolve("hydra").resolve("classes");
    }

    public void compile(Collection<String> modules) {
      bach.log("Generating commands for %s realm multi-release modules(s): %s", realm.name, modules);
      for (var module : modules) {
        compile(realm.unit(module).orElseThrow());
      }
    }

    private void compile(Project.ModuleUnit unit) {
      var base = unit.sources.get(0);
      bach.log("Base feature release number is: %d", base.release);

      for (var source : unit.sources) {
        compile(unit, base, source);
      }
      jarModule(unit);
      jarSources(unit);
    }

    private void compile(Project.ModuleUnit unit, Project.Source base, Project.Source source) {
      var module = unit.info.descriptor().name();
      var baseClasses = classes.resolve(base.path.getFileName()).resolve(module);
      var destination = classes.resolve(source.path.getFileName());
      var javac = new Command("javac").add("--release", source.release);
      if (Util.isModuleInfo(source.path.resolve("module-info.java"))) {
        javac
            .addEach(realm.toolArguments.javac)
            .add("-d", destination)
            .add("--module-version", project.version)
            .add("--module-path", project.modulePaths(target))
            .add("--module-source-path", realm.moduleSourcePath);
        if (base != source) {
          javac.add("--patch-module", module + '=' + baseClasses);
        }
        javac.add("--module", module);
      } else {
        javac.add("-d", destination.resolve(module));
        var classPath = new ArrayList<Path>();
        if (base != source) {
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
        javac.addEach(Util.find(List.of(source.path), Util::isJavaFile));
      }
      bach.run(javac);
    }

    private void jarModule(Project.ModuleUnit unit) {
      var sources = new ArrayDeque<>(unit.sources);
      var base = sources.pop().path.getFileName();
      var module = unit.info.descriptor().name();
      var jar =
          new Command("jar")
              .add("--create")
              .add("--file", Util.treeCreate(target.modules).resolve(target.file(unit, ".jar")))
              .addIff(bach.verbose(), "--verbose")
              .add("-C", classes.resolve(base).resolve(module))
              .add(".")
              .addEach(unit.resources, (cmd, path) -> cmd.add("-C", path).add("."));
      for (var source : sources) {
        var path = source.path.getFileName();
        var released = classes.resolve(path).resolve(module);
        if (source.isVersioned()) {
          jar.add("--release", source.release);
        }
        jar.add("-C", released);
        jar.add(".");
      }
      bach.run(jar);
      if (bach.verbose()) {
        bach.run(new Command("jar", "--describe-module", "--file", target.modularJar(unit)));
      }
    }

    private void jarSources(Project.ModuleUnit unit) {
      var sources = new ArrayDeque<>(unit.sources);
      var jar =
          new Command("jar")
              .add("--create")
              .add("--file", target.sourcesJar(unit))
              .addIff(bach.verbose(), "--verbose")
              .add("--no-manifest")
              .add("-C", sources.removeFirst().path)
              .add(".")
              .addEach(unit.resources, (cmd, path) -> cmd.add("-C", path).add("."));
      for (var source : sources) {
        jar.add("--release", source.release);
        jar.add("-C", source.path);
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
      var stream =
          finder.findAll().stream()
              .map(ModuleReference::descriptor)
              .peek(descriptor -> declaredModules.add(descriptor.name()))
              .map(ModuleDescriptor::requires)
              .flatMap(Set::stream)
              .filter(r -> !r.modifiers().contains(Requires.Modifier.STATIC));
      merge(requiredModules, stream);
      return new Scanner(declaredModules, requiredModules);
    }

    public static Scanner scan(String... sources) {
      var declaredModules = new TreeSet<String>();
      var requiredModules = new TreeMap<String, Set<Version>>();
      for (var source : sources) {
        var descriptor = Modules.describe(source);
        declaredModules.add(descriptor.name());
        merge(requiredModules, descriptor.requires().stream());
      }
      return new Scanner(declaredModules, requiredModules);
    }

    private static void merge(Map<String, Set<Version>> requiredModules, Stream<Requires> stream) {
      stream
          .filter(requires -> !requires.modifiers().contains(Requires.Modifier.MANDATED))
          .forEach(
              requires ->
                  requiredModules.merge(
                      requires.name(),
                      requires.compiledVersion().map(Set::of).orElse(Set.of()),
                      Util::concat));
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

    public void resolve() throws Exception {
      var entries = project.library.modulePaths.toArray(Path[]::new);
      var library = scan(ModuleFinder.of(entries));
      bach.log("Library of -> %s", project.library.modulePaths);
      bach.log("  modules  -> " + library.modules);
      bach.log("  requires -> " + library.requires);

      var units = new ArrayList<Path>();
      for (var realm : project.realms) {
        for (var unit : realm.units) {
          units.add(unit.info.path);
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
      addMissingTestEngines(missing);
      addMissingConsoleLauncher(missing);
      sources.getDeclaredModules().forEach(missing::remove);
      library.getDeclaredModules().forEach(missing::remove);
      systems.getDeclaredModules().forEach(missing::remove);
      if (missing.isEmpty()) {
        return;
      }

      var lib = project.library.modulePaths.get(0);
      var uris = Util.load(new Properties(), lib.resolve("module-uri.properties"));
      var http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
      var log = new Log(bach.out, bach.err, bach.verbose());
      var resources = new Resources(log, http);

      var cache =
          Files.createDirectories(Path.of(System.getProperty("user.home")).resolve(".bach/modules"));
      var artifactPath =
          resources.copy(
              URI.create("https://github.com/sormuras/modules/raw/master/module-maven.properties"),
              cache.resolve("module-maven.properties"),
              StandardCopyOption.COPY_ATTRIBUTES);

      var artifactLookup =
          new Maven.Lookup(
              project.library.mavenGroupColonArtifactMapper,
              Util.map(Util.load(new Properties(), lib.resolve("module-maven.properties"))),
              Util.map(Util.load(new Properties(), artifactPath)));

      var versionPath =
          resources.copy(
              URI.create("https://github.com/sormuras/modules/raw/master/module-version.properties"),
              cache.resolve("module-version.properties"),
              StandardCopyOption.COPY_ATTRIBUTES);

      var versionLookup =
          new Maven.Lookup(
              project.library.mavenVersionMapper,
              Util.map(Util.load(new Properties(), lib.resolve("module-version.properties"))),
              Util.map(Util.load(new Properties(), versionPath)));
      var maven = new Maven(log, resources, artifactLookup, versionLookup);

      do {
        bach.log("Loading missing modules: %s", missing);
        for (var entry : missing.entrySet()) {
          var module = entry.getKey();
          var direct = uris.getProperty(module);
          if (direct != null) {
            var uri = URI.create(direct);
            var jar = lib.resolve(module + ".jar");
            resources.copy(uri, jar, StandardCopyOption.COPY_ATTRIBUTES);
            continue;
          }
          var versions = entry.getValue();
          var version =
              Util.singleton(versions).map(Object::toString).orElse(versionLookup.apply(module));
          var ga = maven.lookup(module, version).split(":");
          var group = ga[0];
          var artifact = ga[1];
          var repository = project.library.mavenRepositoryMapper.apply(group, version);
          resources.copy(
              maven.toUri(repository, group, artifact, version),
              lib.resolve(module + '-' + version + ".jar"),
              StandardCopyOption.COPY_ATTRIBUTES);
        }
        library = scan(ModuleFinder.of(entries));
        missing = new TreeMap<>(library.requires);
        library.getDeclaredModules().forEach(missing::remove);
        systems.getDeclaredModules().forEach(missing::remove);
      } while (!missing.isEmpty());
    }

    private void addMissingTestEngines(Map<String, Set<Version>> map) {
      if (map.containsKey("org.junit.jupiter") || map.containsKey("org.junit.jupiter.api")) {
        map.putIfAbsent("org.junit.jupiter.engine", Set.of());
      }
      if (map.containsKey("junit")) {
        map.putIfAbsent("org.junit.vintage", Set.of());
      }
    }

    private void addMissingConsoleLauncher(Map<String, Set<Version>> map) {
      if (map.containsKey("org.junit.jupiter.engine") || map.containsKey("org.junit.vintage")) {
        map.putIfAbsent("org.junit.platform.console", Set.of());
      }
    }

    /** Module Scanner. */
    public static class Scanner {

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
          throw new UnmappedModuleException(requiredModule);
        }
        if (versions.size() > 1) {
          throw new IllegalStateException("Multiple versions: " + requiredModule + " -> " + versions);
        }
        return versions.stream().findFirst();
      }
    }
  }

  /** Uniform Resource Identifier ({@link java.net.URI}) read and download support. */
  public static class Resources {

    public static Resources ofSystem() {
      var log = Log.ofSystem();
      var httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
      return new Resources(log, httpClient);
    }

    private final Log log;
    private final HttpClient http;

    public Resources(Log log, HttpClient http) {
      this.log = log;
      this.http = http;
    }

    public HttpResponse<Void> head(URI uri, int timeout) throws IOException, InterruptedException {
      var nobody = HttpRequest.BodyPublishers.noBody();
      var duration = Duration.ofSeconds(timeout);
      var request = HttpRequest.newBuilder(uri).method("HEAD", nobody).timeout(duration).build();
      return http.send(request, HttpResponse.BodyHandlers.discarding());
    }

    /** Copy all content from a uri to a target file. */
    public Path copy(URI uri, Path path, CopyOption... options) throws Exception {
      log.debug("Copy %s to %s", uri, path);
      Files.createDirectories(path.getParent());
      if ("file".equals(uri.getScheme())) {
        try {
          return Files.copy(Path.of(uri), path, options);
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
          log.warn("Couldn't get 'user:etag' file attribute: %s", e);
        }
      }
      var handler = HttpResponse.BodyHandlers.ofFile(path);
      var response = http.send(request.build(), handler);
      if (response.statusCode() == 200) {
        if (Set.of(options).contains(StandardCopyOption.COPY_ATTRIBUTES)) {
          var etagHeader = response.headers().firstValue("etag");
          if (etagHeader.isPresent()) {
            try {
              var etag = etagHeader.get();
              Files.setAttribute(path, "user:etag", StandardCharsets.UTF_8.encode(etag));
            } catch (Exception e) {
              log.warn("Couldn't set 'user:etag' file attribute: %s", e);
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
              log.warn("Couldn't set last modified file attribute: %s", e);
            }
          }
        }
        log.debug("%s <- %s", path, uri);
      }
      return path;
    }

    /** Read all content from a uri into a string. */
    public String read(URI uri) throws IOException, InterruptedException {
      log.debug("Read %s", uri);
      if ("file".equals(uri.getScheme())) {
        return Files.readString(Path.of(uri));
      }
      var request = HttpRequest.newBuilder(uri).GET();
      return http.send(request.build(), HttpResponse.BodyHandlers.ofString()).body();
    }
  }

  /** Create API documentation. */
  public static class Scribe {

    enum ScriptType {
      BASH(".sh", '\''),
      WIN(".bat", '"') {
        @Override
        List<String> lines(List<String> lines) {
          return lines.stream().map(line -> "call " + line).collect(Collectors.toList());
        }
      };

      final String extension;
      final char quote;

      ScriptType(String extension, char quote) {
        this.extension = extension;
        this.quote = quote;
      }

      String quote(Object object) {
        return quote + object.toString() + quote;
      }

      List<String> lines(List<String> lines) {
        return lines;
      }
    }

    private final Bach bach;
    private final Project project;
    private final Project.Realm realm;
    private final Project.Target target;
    private final Path javadocJar;

    public Scribe(Bach bach, Project project, Project.Realm realm) {
      this.bach = bach;
      this.project = project;
      this.realm = realm;
      this.target = project.target(realm);

      var nameDashVersion = project.name + '-' + project.version;
      this.javadocJar = target.directory.resolve(nameDashVersion + "-javadoc.jar");
    }

    public void document() {
      document(realm.names());
    }

    public void document(Iterable<String> modules) {
      bach.log("Compiling %s realm's documentation: %s", realm.name, modules);
      var destination = target.directory.resolve("javadoc");
      var javadoc =
          new Command("javadoc")
              .add("-d", destination)
              .add("-encoding", "UTF-8")
              .add("-locale", "en")
              .addIff(!bach.verbose(), "-quiet")
              .add("-Xdoclint:-missing")
              .add("--module-path", project.library.modulePaths)
              .add("--module-source-path", realm.moduleSourcePath);

      for (var unit : realm.units(Project.ModuleUnit::isMultiRelease)) {
        var base = unit.sources.get(0);
        if (!unit.info.path.startsWith(base.path)) {
          javadoc.add("--patch-module", unit.name() + "=" + base.path);
        }
      }

      javadoc.add("--module", String.join(",", modules));
      bach.run(javadoc);

      bach.run(
          new Command("jar")
              .add("--create")
              .add("--file", javadocJar)
              .addIff(bach.verbose(), "--verbose")
              .add("--no-manifest")
              .add("-C", destination)
              .add("."));
    }

    public void generateMavenInstallScript() {
      for (var type : ScriptType.values()) {
        generateMavenInstallScript(type);
      }
    }

    private void generateMavenInstallScript(ScriptType type) {
      var plugin = "install:install-file";
      var maven = String.join(" ", "mvn", "--batch-mode", "--no-transfer-progress", plugin);
      var lines = new ArrayList<String>();
      for (var unit : realm.units) {
        if (unit.mavenPom().isPresent()) {
          lines.add(String.join(" ", maven, generateMavenArtifactLine(unit, type)));
        }
      }
      if (lines.isEmpty()) {
        bach.log("No maven-install script lines generated.");
        return;
      }
      try {
        var script = bach.project.targetDirectory.resolve("maven-install" + type.extension);
        Files.write(script, type.lines(lines));
      } catch (IOException e) {
        throw new UncheckedIOException("Generating install script failed: " + e.getMessage(), e);
      }
    }

    public void generateMavenDeployScript() {
      for (var type : ScriptType.values()) {
        generateMavenDeployScript(type);
      }
    }

    private void generateMavenDeployScript(ScriptType type) {
      var deployment = realm.toolArguments.deployment().orElseThrow();
      var plugin = "org.apache.maven.plugins:maven-deploy-plugin:3.0.0-M1:deploy-file";
      var repository = "repositoryId=" + type.quote(deployment.mavenRepositoryId);
      var url = "url=" + type.quote(deployment.mavenUri);
      var maven = String.join(" ", "mvn", "--batch-mode", plugin);
      var repoAndUrl = String.join(" ", "-D" + repository, "-D" + url);
      var lines = new ArrayList<String>();
      for (var unit : realm.units) {
        lines.add(String.join(" ", maven, repoAndUrl, generateMavenArtifactLine(unit, type)));
      }
      try {
        var name = "maven-deploy-" + deployment.mavenRepositoryId;
        var script = bach.project.targetDirectory.resolve(name + type.extension);
        Files.write(script, type.lines(lines));
      } catch (IOException e) {
        throw new UncheckedIOException("Deploy failed: " + e.getMessage(), e);
      }
    }

    private String generateMavenArtifactLine(Project.ModuleUnit unit, ScriptType type) {
      var pom = "pomFile=" + type.quote(Util.require(unit.mavenPom().orElseThrow(), Files::exists));
      var file = "file=" + type.quote(Util.require(target.modularJar(unit), Util::isJarFile));
      var sources = "sources=" + type.quote(Util.require(target.sourcesJar(unit), Util::isJarFile));
      var javadoc = "javadoc=" + type.quote(Util.require(javadocJar, Util::isJarFile));
      return String.join(" ", "-D" + pom, "-D" + file, "-D" + sources, "-D" + javadoc);
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
          throw new AssertionError("Running method failed: " + name, t);
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

  /** Launch JUnit Platform. */
  static class Tester {

    private final Bach bach;
    private final Project.Realm test;

    Tester(Bach bach, Project.Realm test) {
      this.bach = bach;
      this.test = test;
    }

    void test() {
      bach.log("Launching all test modules in realm: %s", test.name);
      test(test.names());
    }

    void test(Iterable<String> modules) {
      bach.log("Launching all tests in realm " + test);
      for (var module : modules) {
        bach.log("%n%n%n%s%n%n%n", module);
        var unit = test.unit(module);
        if (unit.isEmpty()) {
          bach.warn("No test module unit available for: %s", module);
          continue;
        }
        test(unit.get());
      }
    }

    private void test(Project.ModuleUnit unit) {
      var target = bach.project.target(test);
      var modulePath = bach.project.modulePaths(target, target.modularJar(unit));
      var layer = layer(modulePath, unit.name());

      var errors = new StringBuilder();
      errors.append(new ToolProviderTester(layer, unit).test());
      errors.append(new JUnitConsoleTester(layer, unit).test());
      if (errors.toString().replace('0', ' ').isBlank()) {
        return;
      }
      throw new AssertionError("Test run failed!");
    }

    private ModuleLayer layer(List<Path> modulePath, String module) {
      bach.log("Module path:");
      for (var element : modulePath) {
        bach.log("  -> %s", element);
      }
      var finder = ModuleFinder.of(modulePath.toArray(Path[]::new));
      bach.log("Finder finds module(s):");
      finder.findAll().stream()
          .sorted(Comparator.comparing(ModuleReference::descriptor))
          .forEach(reference -> bach.log("  -> %s", reference));
      var roots = List.of(module);
      bach.log("Root module(s):");
      for (var root : roots) {
        bach.log("  -> %s", root);
      }
      var boot = ModuleLayer.boot();
      var configuration = boot.configuration().resolveAndBind(finder, ModuleFinder.of(), roots);
      var loader = ClassLoader.getPlatformClassLoader();
      var controller = ModuleLayer.defineModulesWithOneLoader(configuration, List.of(boot), loader);
      return controller.layer();
    }

    private int run(ToolProvider tool, String... args) {
      var toolLoader = tool.getClass().getClassLoader();
      var currentThread = Thread.currentThread();
      var currentContextLoader = currentThread.getContextClassLoader();
      currentThread.setContextClassLoader(toolLoader);
      var parent = toolLoader;
      while (parent != null) {
        parent.setDefaultAssertionStatus(true);
        parent = parent.getParent();
      }
      try {
        return bach.run(tool, args);
      } finally {
        currentThread.setContextClassLoader(currentContextLoader);
      }
    }

    class ToolProviderTester {

      private final ModuleLayer layer;
      private final Project.ModuleUnit unit;

      ToolProviderTester(ModuleLayer layer, Project.ModuleUnit unit) {
        this.layer = layer;
        this.unit = unit;
      }

      int test() {
        var key = "test(" + unit.name() + ")";
        var serviceLoader = ServiceLoader.load(layer, ToolProvider.class);
        var tools =
            StreamSupport.stream(serviceLoader.spliterator(), false)
                .filter(provider -> provider.name().equals(key))
                .collect(Collectors.toList());
        if (tools.isEmpty()) {
          bach.log("No tool provider named '%s' found in: %s", key, layer);
          return 0;
        }
        int sum = 0;
        for (var tool : tools) {
          sum += run(tool);
        }
        return sum;
      }
    }

    class JUnitConsoleTester {

      private final ModuleLayer layer;
      private final Project.ModuleUnit unit;

      JUnitConsoleTester(ModuleLayer layer, Project.ModuleUnit unit) {
        this.layer = layer;
        this.unit = unit;
      }

      int test() {
        var serviceLoader = ServiceLoader.load(layer, ToolProvider.class);
        var junit =
            StreamSupport.stream(serviceLoader.spliterator(), false)
                .filter(provider -> provider.name().equals("junit"))
                .findFirst();
        if (junit.isEmpty()) {
          bach.warn("No tool provider named 'junit' for %s found in: %s", unit.name(), layer);
          return 0;
        }
        return run(junit.get(), "--select-module", unit.name());
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

    static boolean isWindows() {
      return System.getProperty("os.name", "?").toLowerCase().contains("win");
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

    static List<Path> list(Path directory, String glob) {
      try (var items = Files.newDirectoryStream(directory, glob)) {
        return StreamSupport.stream(items.spliterator(), false).sorted().collect(Collectors.toList());
      } catch (IOException e) {
        throw new UncheckedIOException("list directory using glob failed: " + directory, e);
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

    /** Convert all {@link String}-based properties in an instance of {@code Map<String, String>}. */
    static Map<String, String> map(Properties properties) {
      var map = new HashMap<String, String>();
      for (var name : properties.stringPropertyNames()) {
        map.put(name, properties.getProperty(name));
      }
      return Map.copyOf(map);
    }

    /** Extract last path element from the supplied uri. */
    static Optional<String> findFileName(URI uri) {
      var path = uri.getPath();
      return path == null ? Optional.empty() : Optional.of(path.substring(path.lastIndexOf('/') + 1));
    }

    /** Null-safe file name getter. */
    static Optional<String> findFileName(Path path) {
      return Optional.ofNullable(path.toAbsolutePath().getFileName()).map(Path::toString);
    }

    static Optional<String> findVersion(String jarFileName) {
      if (!jarFileName.endsWith(".jar")) return Optional.empty();
      var name = jarFileName.substring(0, jarFileName.length() - 4);
      var matcher = Pattern.compile("-(\\d+(\\.|$))").matcher(name);
      return (matcher.find()) ? Optional.of(name.substring(matcher.start() + 1)) : Optional.empty();
    }

    static Path require(Path path, Predicate<Path> predicate) {
      if (predicate.test(path)) {
        return path;
      }
      throw new IllegalArgumentException("Path failed test: " + path);
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
  }

  /** Unchecked exception thrown when a module name is not mapped. */
  public static class UnmappedModuleException extends RuntimeException {

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
