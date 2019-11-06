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
import java.io.PrintWriter;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;

/** Build modular Java project. */
public class Bach {

  /** Create project instance to build. */
  public static Project project() {
    return Project.Builder.build(Path.of(""));
  }

  @SuppressWarnings("unused")
  public static void versions(String... args) {
    var tools = new Tools(Log.ofSystem(true));
    tools.map.values().forEach(tool -> tools.run(new Command(tool.name()).add("--version")));
  }

  /** Main entry-point. */
  public static void main(String... args) throws Exception {
    var project = project();
    if (args.length == 0) {
      System.out.println();
      System.out.println("Usage: java Bach.java <action> [args...]");
      System.out.println();
      System.out.println("Available actions:");
      System.out.println("build");
      System.out.println("  Build project " + project.name + " " + project.version);
      System.out.println("call <entry-point> [args...]");
      System.out.println("  Invoke the named entry-point method. The method signature must match");
      System.out.println("  the Java main default: public static void <name>(String... args) and");
      System.out.println("  it may throw any exception type. Valid calls are:");
      Arrays.stream(Bach.class.getMethods())
          .filter(m -> Arrays.equals(m.getParameterTypes(), new Class[] {String[].class}))
          .map(Method::getName)
          .sorted()
          .forEach(name -> System.out.println("    java Bach.java call " + name + " [args...]"));
      System.out.println("project [path]");
      System.out.println("  Print source representation of the project to be built");
      System.out.println("version");
      System.out.println("  Print version of Bach.java: " + VERSION);
      System.out.println();
      return;
    }
    var arguments = new ArrayDeque<>(List.of(args));
    switch (arguments.pop()) {
      case "build":
        new Bach(Log.ofSystem(), project).build();
        return;
      case "call":
        var strings = arguments.toArray(String[]::new);
        Bach.class.getMethod(arguments.pop(), String[].class).invoke(null, (Object) strings);
        return;
      case "project":
        var it = arguments.isEmpty() ? project : Project.Builder.build(Path.of(arguments.pop()));
        it.toSourceLines().forEach(System.out::println);
        return;
      case "version":
        System.out.println(VERSION);
        return;
      default:
        throw new Error("Unknown action: " + String.join(" ", args));
    }
  }

  private final Log log;
  private final Project project;
  private final Tools tools;

  public Bach(Log log, Project project) {
    this.log = log;
    this.project = project;
    this.tools = new Tools(log);
  }

  public void build() throws Exception {
    buildInfo();
    if (project.units.isEmpty()) {
      log.warn("Not a single module unit declared, no build.");
      return;
    }
    var start = Instant.now();
    for (var realm : project.realms) {
      var units = realm.units(project.units);
      if (units.isEmpty()) continue;
      new Jigsaw(realm).compile(units);
    }
    buildSummary(start);
  }

  private void buildInfo() {
    log.info("Build of project %s %s started...", project.name, project.version);
    if (log.verbose) {
      log.debug("%nRuntime information");
      log.debug("  - java.version = " + System.getProperty("java.version"));
      log.debug("  - user.dir = " + System.getProperty("user.dir"));
      log.debug("%nProject information");
      project.toSourceLines().forEach(log::debug);
      log.debug("%nTools of the trade");
      tools.map.values().forEach(t -> log.debug("  - %8s [%s] %s", t.name(), Modules.origin(t), t));
      log.debug("");
    }
  }

  /** Run and record the given command instance. */
  public void run(Command command) {
    var start = Instant.now();
    int code = tools.run(command);
    log.record(code, Duration.between(start, Instant.now()), command);
    if (code != 0) {
      throw new RuntimeException("Non-zero exit code: " + code);
    }
  }

  private void buildSummary(Instant start) throws Exception {
    var duration = Duration.between(start, Instant.now());
    var lines = new ArrayList<String>();
    lines.add(String.format("# Build of %s %s", project.name, project.version));
    lines.add("- started: " + start);
    lines.add("- duration: " + duration);
    lines.add("");
    lines.add("## Project Source");
    lines.add("```");
    lines.addAll(project.toSourceLines());
    lines.add("```");
    lines.add("## Tools");
    tools.map.values().forEach(t -> lines.add("- `" + t.name() + "` from " + Modules.origin(t)));
    log.debug("");
    lines.add("## Commands");
    lines.add("```");
    log.commands.stream().map(Command::toSource).forEach(lines::add);
    lines.add("```");
    lines.add("");
    lines.add("## System Properties");
    System.getProperties().stringPropertyNames().stream()
        .sorted()
        .forEach(key -> lines.add("- `" + key + "`: `" + $(System.getProperty(key)) + "`"));

    var withStart = ("build-summary-" + start + ".md").replace(':', '-');
    var file = Files.write(Resources.createParents(project.paths.log(withStart)), lines);
    Files.copy(file, project.paths.out("build-summary.md"), StandardCopyOption.REPLACE_EXISTING);
    if (log.verbose) lines.forEach(log::debug);

    log.info("%nCommand history");
    log.records.forEach(log::info);
    log.info("%nBuild %d took millis.", duration.toMillis());
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

    /** Recorded command history. */
    private final List<Command> commands;
    /** Recorded command history. */
    private final List<String> records;

    /** Text-output writer. */
    private final PrintWriter out, err;
    /** Be verbose. */
    private final boolean verbose;

    public Log(PrintWriter out, PrintWriter err, boolean verbose) {
      this.out = out;
      this.err = err;
      this.verbose = verbose;
      this.commands = new ArrayList<>();
      this.records = new ArrayList<>();
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

    public void record(int code, Duration duration, Command command) {
      commands.add(command);
      var args = command.arguments.isEmpty() ? "" : " " + String.join(" ", command.arguments);
      records.add(String.format("%3d %5d ms %s%s", code, duration.toMillis(), command.name, args));
    }
  }

  /** Project model. */
  public static class Project {

    final Path base;
    final Paths paths;
    final String name;
    final Version version;
    final List<Realm> realms;
    final List<Unit> units;

    public Project(Path base, String name, Version version, List<Realm> realms, List<Unit> units) {
      this.base = base;
      this.paths = new Paths(base);
      this.name = name;
      this.version = version;
      this.realms = List.copyOf(realms);
      this.units = List.copyOf(units);
    }

    @Override
    public boolean equals(Object that) {
      if (this == that) return true;
      if (that == null || getClass() != that.getClass()) return false;
      return this.hashCode() == that.hashCode();
    }

    @Override
    public int hashCode() {
      return Objects.hash(base, name, version, realms, units);
    }

    Version version(Unit unit) {
      return unit.descriptor.version().orElse(version);
    }

    public List<String> toSourceLines() {
      if (realms.isEmpty() && units.isEmpty()) {
        var line = "new Project(%s, %s, Version.parse(%s), List.of(), List.of())";
        return List.of(String.format(line, $(base), $(name), $(version)));
      }
      var lines = new ArrayList<String>();
      lines.add("new Project(");
      lines.add("    " + $(base) + ",");
      lines.add("    " + $(name) + ",");
      lines.add("    Version.parse(" + $(version) + "),");
      lines.add("    List.of(");
      var lastRealm = realms.get(realms.size() - 1);
      for (var realm : realms) {
        var comma = realm == lastRealm ? "" : ",";
        lines.add(
            "        new Project.Realm("
                + $(realm.name)
                + ", "
                + $(realm.sourcePaths)
                + ", "
                + $(realm.modulePaths)
                + ")"
                + comma);
      }
      lines.add("    ),");
      lines.add("    List.of(");
      var lastUnit = units.get(units.size() - 1);
      for (var unit : units) {
        var descriptor = "ModuleDescriptor.newModule(" + $(unit.name()) + ").build()";
        var comma = unit == lastUnit ? "" : ",";
        lines.add(
            "        new Project.Unit("
                + $(unit.root)
                + ", "
                + $(unit.realm)
                + ", "
                + descriptor
                + ")"
                + comma);
      }
      lines.add("    )");
      lines.add(")");
      return lines;
    }

    /** Mutable project builder. */
    public static class Builder {

      static Project build(Path base) {
        return of(base).build();
      }

      static Builder of(Path base) {
        if (!Files.isDirectory(base)) {
          throw new IllegalArgumentException("Not a directory: " + base);
        }
        var name =
            Optional.ofNullable(base.toAbsolutePath().normalize().getFileName())
                .map(Path::toString)
                .orElse("project");
        var src = Path.of(System.getProperty(".bach/project.path.src", "src"));
        var paths = new Paths(base);
        var builder =
            new Builder()
                .base(base)
                .name(name)
                .version(System.getProperty(".bach/project.version", "0"))
                .realm("main", List.of(src.resolve("{MODULE}/main/java")), List.of(paths.lib()))
                .realm(
                    "test",
                    List.of(src.resolve("{MODULE}/test/java"), src.resolve("{MODULE}/test/module")),
                    List.of(paths.modules("main"), paths.lib()));
        try (var directories = Files.newDirectoryStream(base.resolve(src), Files::isDirectory)) {
          for (var directory : directories) { // directory = "src/{MODULE}"
            for (var realm : List.of("main", "test")) {
              var info = directory.resolve(realm).resolve("java/module-info.java");
              if (Files.isRegularFile(info)) {
                builder.unit(directory, realm, Modules.describe(Files.readString(info)));
              }
            }
          }
        } catch (Exception e) {
          throw new Error("Parsing directory for Java modules failed: " + base, e);
        }
        return builder;
      }

      Path base = Path.of("");
      String name = "project";
      Version version = Version.parse("0");
      List<Realm> realms = new ArrayList<>();
      List<Unit> units = new ArrayList<>();

      Builder base(Path base) {
        this.base = base;
        return this;
      }

      Builder name(String name) {
        this.name = name;
        return this;
      }

      Builder version(String version) {
        this.version = Version.parse(version);
        return this;
      }

      Builder realm(String name, List<Path> moduleSourcePaths, List<Path> modulePaths) {
        realms.add(new Realm(name, moduleSourcePaths, modulePaths));
        return this;
      }

      Builder unit(Path root, String realm, ModuleDescriptor descriptor) {
        units.add(new Unit(root, realm, descriptor));
        return this;
      }

      Project build() {
        return new Project(base, name, version, realms, units);
      }
    }

    /** Paths registry. */
    static class Paths {

      final Path base;

      Paths(Path base) {
        this.base = base;
      }

      Path resolve(Path path, String[] more) {
        if (more.length == 0) return path;
        return path.resolve(String.join(File.separator, more));
      }

      public Path out(String... more) {
        return resolve(base.resolve(".bach/out"), more);
      }

      public Path lib(String... more) {
        return resolve(base.resolve("lib"), more);
      }

      public Path log(String... more) {
        return resolve(out().resolve("log"), more);
      }

      public Path realm(String realm, String... more) {
        return resolve(out().resolve(realm), more);
      }

      public Path modules(String realm, String... more) {
        return resolve(realm(realm).resolve("modules"), more);
      }
    }

    public static /*record*/ class Realm {
      final String name;
      final List<Path> sourcePaths;
      final List<Path> modulePaths;

      public Realm(String name, List<Path> sourcePaths, List<Path> modulePaths) {
        this.name = name;
        this.sourcePaths = sourcePaths;
        this.modulePaths = modulePaths;
      }

      @Override
      public boolean equals(Object that) {
        if (this == that) return true;
        if (that == null || getClass() != that.getClass()) return false;
        return this.hashCode() == that.hashCode();
      }

      @Override
      public int hashCode() {
        return Objects.hash(name, sourcePaths, modulePaths);
      }

      List<Unit> units(List<Unit> units) {
        return units.stream().filter(unit -> name.equals(unit.realm)).collect(Collectors.toList());
      }

      String moduleSourcePath() {
        return sourcePaths.stream()
            .map(Path::toString)
            .map(path -> path.replace("{MODULE}", "*"))
            .collect(Collectors.joining(File.pathSeparator));
      }

      @Override
      public String toString() {
        return "Realm{"
            + "name='"
            + name
            + '\''
            + ", sourcePaths="
            + sourcePaths
            + ", modulePaths="
            + modulePaths
            + '}';
      }
    }

    public static /*record*/ class Unit {
      final Path root; // "src/foo.bar"
      final String realm; // "main"
      final ModuleDescriptor descriptor; // module foo.bar {...}
      final List<Path> sources;
      final List<Path> resources;

      public Unit(Path root, String realm, ModuleDescriptor descriptor) {
        this.root = root;
        this.realm = realm;
        this.sources = List.of(root.resolve(realm).resolve("java"));
        this.resources = List.of(root.resolve(realm).resolve("resources"));
        this.descriptor = descriptor;
      }

      @Override
      public boolean equals(Object that) {
        if (this == that) return true;
        if (that == null || getClass() != that.getClass()) return false;
        return this.hashCode() == that.hashCode();
      }

      @Override
      public int hashCode() {
        return Objects.hash(root, realm, descriptor, sources, resources);
      }

      String name() {
        return descriptor.name();
      }
    }
  }

  /** Static helper for modules and their friends. */
  public static class Modules {

    private static final Pattern MAIN_CLASS =
        Pattern.compile("//\\s*(?:--main-class)\\s+([\\w.]+)");

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

    public static String origin(Object object) {
      var module = object.getClass().getModule();
      if (module.isNamed()) {
        return module.getDescriptor().toNameAndVersion();
      }
      try {
        var uri = object.getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
        return Path.of(uri).getFileName().toString();
      } catch (NullPointerException | URISyntaxException ignore) {
        return module.toString();
      }
    }
  }

  /** Command. */
  public static class Command {
    final String name;
    final List<String> arguments;
    final List<String> additions;

    public Command(String name) {
      this.name = name;
      this.arguments = new ArrayList<>();
      this.additions = new ArrayList<>();
    }

    private Command arg(Object object) {
      arguments.add(object.toString());
      return this;
    }

    public Command add(String string) {
      additions.add(String.format(".add(%s)", $(string)));
      return arg(string);
    }

    public Command add(Path path) {
      additions.add(String.format(".add(%s)", $(path)));
      return arg(path);
    }

    public Command add(Number number) {
      additions.add(String.format(".add(%s)", number));
      return arg(number);
    }

    public Command add(String key, String string) {
      additions.add(String.format(".add(%s, %s)", $(key), $(string)));
      return arg(key).arg(string);
    }

    public Command add(String key, Path path) {
      additions.add(String.format(".add(%s, %s)", $(key), $(path)));
      return arg(key).arg(path);
    }

    public Command add(String key, Number number) {
      additions.add(String.format(".add(%s, %s)", $(key), number));
      return arg(key).arg(number);
    }

    public Command add(String key, List<Path> paths) {
      if (paths.isEmpty()) return this;
      var p = String.join(", ", paths.stream().map(Bach::$).toArray(String[]::new));
      additions.add(String.format(".add(%s, %s)", $(key), p));
      var strings = paths.stream().map(Path::toString);
      return arg(key).arg(strings.collect(Collectors.joining(File.pathSeparator)));
    }

    public <T> Command forEach(Iterable<T> arguments, BiConsumer<Command, T> visitor) {
      arguments.forEach(argument -> visitor.accept(this, argument));
      return this;
    }

    public Command iff(boolean predicate, Consumer<Command> visitor) {
      if (predicate) visitor.accept(this);
      return this;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public <T> Command iff(Optional<T> optional, BiConsumer<Command, T> visitor) {
      optional.ifPresent(value -> visitor.accept(this, value));
      return this;
    }

    public String toSource() {
      return String.format("new Command(%s)%s", $(name), String.join("", additions));
    }

    public String[] toStringArray() {
      return arguments.toArray(String[]::new);
    }
  }

  /** {@link Path}-related helpers. */
  public static class Resources {
    private Resources() {}

    public static List<Path> filter(List<Path> paths, Predicate<Path> filter) {
      return paths.stream().filter(filter).collect(Collectors.toList());
    }

    public static List<Path> filterExisting(List<Path> paths) {
      return filter(paths, Files::exists);
    }

    public static Path createDirectories(Path directory) {
      try {
        Files.createDirectories(directory);
      } catch (Exception e) {
        throw new RuntimeException("Create directories failed: " + directory, e);
      }
      return directory;
    }

    public static Path createParents(Path file) {
      createDirectories(file.getParent());
      return file;
    }
  }

  /** Tool registry and command runner support. */
  public static class Tools {
    final Log log;
    final Map<String, ToolProvider> map;

    public Tools(Log log) {
      this.log = log;
      this.map = new TreeMap<>();
      ServiceLoader.load(ToolProvider.class).stream()
          .map(ServiceLoader.Provider::get)
          .forEach(provider -> map.putIfAbsent(provider.name(), provider));
    }

    public ToolProvider get(String name) {
      var tool = map.get(name);
      if (tool == null) {
        throw new NoSuchElementException("No such tool: " + name);
      }
      return tool;
    }

    public int run(Command command) {
      log.debug("| %s(%s)", command.name, String.join(", ", command.arguments));
      return get(command.name).run(log.out, log.err, command.toStringArray());
    }
  }

  private class Jigsaw {

    final Project.Realm realm;
    final Path work;
    final Path classes;
    final Path modules;

    Jigsaw(Project.Realm realm) {
      this.realm = realm;
      this.work = project.paths.realm(realm.name);
      this.classes = work.resolve("classes/jigsaw");
      this.modules = Resources.createDirectories(project.paths.modules(realm.name));
    }

    void compile(List<Project.Unit> units) {
      var moduleNames = units.stream().map(Project.Unit::name).collect(Collectors.toList());
      var modulePaths = Resources.filterExisting(List.of(project.paths.lib()));
      run(
          new Command("javac")
              .add("-d", classes)
              .add("--module", String.join(",", moduleNames))
              // .addEach(realm.toolArguments.javac)
              // .iff(realm.preview, c -> c.add("--enable-preview"))
              // .iff(realm.release != 0, c -> c.add("--release", realm.release))
              .add("--module-path", modulePaths)
              .add("--module-source-path", realm.moduleSourcePath())
              .add("--module-version", project.version.toString())
          // .addEach(patches(modules))
          );
      for (var unit : units) {
        jarSources(unit);
        jarModule(unit);
      }
    }

    //    private List<String> patches(Collection<String> modules) {
    //      var patches = new Command("<patches>");
    //      for (var module : modules) {
    //        var other =
    //            realm.realms.stream()
    //                .flatMap(r -> r.units.stream())
    //                .filter(u -> u.name().equals(module))
    //                .findFirst();
    //        other.ifPresent(
    //            unit ->
    //                patches.add(
    //                    "--patch-module",
    //                    unit.sources.stream().map(s -> s.path),
    //                    v -> module + "=" + v));
    //      }
    //      return patches.getArguments();
    //    }

    private void jarModule(Project.Unit unit) {
      var jar = modules.resolve(unit.name() + '-' + project.version(unit) + ".jar");
      var resources = Resources.filterExisting(unit.resources);
      run(
          new Command("jar")
              .add("--create")
              .add("--file", jar) // "jar" doesn't create parent directories
              .iff(log.verbose, c -> c.add("--verbose"))
              .iff(unit.descriptor.version(), (c, v) -> c.add("--module-version", v.toString()))
              .iff(unit.descriptor.mainClass(), (c, m) -> c.add("--main-class", m))
              .add("-C", classes.resolve(unit.name()))
              .add(".")
              .forEach(resources, (cmd, path) -> cmd.add("-C", path).add(".")));
      if (log.verbose) {
        run(new Command("jar").add("--describe-module").add("--file", jar));
      }
    }

    private void jarSources(Project.Unit unit) {
      var jar = work.resolve(unit.name() + '-' + project.version(unit) + "-sources.jar");
      var sources = Resources.filterExisting(unit.sources);
      var resources = Resources.filterExisting(unit.resources);
      run(
          new Command("jar")
              .add("--create")
              .add("--file", jar)
              .iff(log.verbose, c -> c.add("--verbose"))
              .add("--no-manifest")
              .forEach(sources, (cmd, path) -> cmd.add("-C", path).add("."))
              .forEach(resources, (cmd, path) -> cmd.add("-C", path).add(".")));
    }
  }

  static final String VERSION = "2.0-ea";

  static String $(Object object) {
    if (object == null) return "null";
    var string = object.toString();
    var escaped = new StringBuilder();
    for (int i = 0; i < string.length(); i++) {
      char c = string.charAt(i);
      switch (c) {
        case '\t':
          escaped.append("\\t");
          break;
        case '\b':
          escaped.append("\\b");
          break;
        case '\n':
          escaped.append("\\n");
          break;
        case '\r':
          escaped.append("\\r");
          break;
        case '\f':
          escaped.append("\\f");
          break;
          // case '\'': escaped.append("\\'"); break; // not needed
        case '\"':
          escaped.append("\\\"");
          break;
        case '\\':
          escaped.append("\\\\");
          break;
        default:
          escaped.append(c);
      }
    }
    return "\"" + escaped + "\"";
  }

  static String $(Path path) {
    return path == null ? "null" : "Path.of(" + $(path.toString().replace('\\', '/')) + ")";
  }

  static String $(List<Path> paths) {
    return "List.of(" + paths.stream().map(Bach::$).collect(Collectors.joining(", ")) + ")";
  }
}
