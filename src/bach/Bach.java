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
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
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
import java.util.stream.StreamSupport;
import javax.lang.model.SourceVersion;

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
        System.out.println(it);
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
    log.record("Build");
    buildInfo();
    if (project.units.isEmpty()) {
      log.warn("Not a single module unit declared, no build.");
      return;
    }
    log.record("Assemble");
    // TODO Load missing modules
    tools.maven("--version");
    log.record("Compile");
    var start = Instant.now();
    for (var realm : project.realms) {
      var units = realm.units(project.units);
      if (units.isEmpty()) continue;
      new Jigsaw(realm).compile(units);
    }
    log.record("Test");
    for (var realm : project.realms) {
      if (!realm.modifiers.contains(Project.Realm.Modifier.TEST)) continue;
      var units = realm.units(project.units);
      if (units.isEmpty()) continue;
      new Tester(realm).test(units);
    }
    log.record("Summary");
    buildSummary(start);
  }

  private void buildInfo() {
    log.info("Build of project %s %s started...", project.name, project.version);
    if (log.verbose) {
      log.debug("%nRuntime information");
      log.debug("  - java.version = " + System.getProperty("java.version"));
      log.debug("  - user.dir = " + System.getProperty("user.dir"));
      log.debug("%nProject information");
      project.toString().lines().forEach(log::debug);
      log.debug("%nTools of the trade");
      tools.map.values().forEach(t -> log.debug("  - %8s [%s] %s", t.name(), Modules.origin(t), t));
      log.debug("");
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
    project.toString().lines().forEach(lines::add);
    lines.add("```");
    lines.add("## Tools");
    tools.map.values().forEach(t -> lines.add("- `" + t.name() + "` from " + Modules.origin(t)));
    log.debug("");
    lines.add("## Commands");
    lines.add("```");
    log.commands.stream().map(Command::toString).forEach(lines::add);
    lines.add("```");
    lines.add("");
    lines.add("## System Properties");
    System.getProperties().stringPropertyNames().stream()
        .sorted()
        .forEach(key -> lines.add("- `" + key + "`: `" + System.getProperty(key) + "`"));

    var withStart = ("build-summary-" + start + ".md").replace(':', '-');
    var file = Files.write(Paths.createParents(project.paths.log(withStart)), lines);
    Files.copy(file, project.paths.out("build-summary.md"), StandardCopyOption.REPLACE_EXISTING);
    if (log.verbose) lines.forEach(log::debug);

    var main = project.realms.stream().filter(r -> r.name.equals("main")).findFirst().orElseThrow();
    var names =
        main.units(project.units).stream().map(Project.Unit::name).collect(Collectors.joining(","));
    var test = project.realms.stream().filter(r -> r.name.equals("test")).findFirst().orElseThrow();
    var deps =
        new Command("jdeps").add("--module-path", test.modulePaths).add("--multi-release", "BASE");
    run(
        deps.clone()
            .add("-summary")
            .add("--dot-output", project.paths.realm("main"))
            .add("--add-modules", names));
    if (log.verbose) {
      run(deps.clone().add("--check", names));
    }

    log.info("%nCommand history");
    log.records.forEach(log::info);

    log.info("%nModules");
    var modules = project.paths.modules("main");
    var jars = Paths.list(modules, path -> path.getFileName().toString().endsWith(".jar"));
    log.info("%d jar(s) found in: %s", jars.size(), modules.toUri());
    for (var jar : jars) {
      log.info("%,11d %s", Files.size(jar), jar.getFileName());
    }

    log.info("%nBuild %d took millis.", duration.toMillis());
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

    public void record(String line) {
      records.add(line);
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
                .realm(
                    "main",
                    Set.of(Realm.Modifier.DOCUMENT),
                    List.of(src.resolve("{MODULE}/main/java")),
                    List.of(paths.lib()))
                .realm(
                    "test",
                    Set.of(Realm.Modifier.TEST),
                    List.of(src.resolve("{MODULE}/test/java"), src.resolve("{MODULE}/test/module")),
                    List.of(paths.modules("main"), paths.lib()));
        var modules = new TreeMap<String, List<String>>();
        for (var root : Bach.Paths.list(base.resolve(src), Files::isDirectory)) {
          var module = root.getFileName().toString();
          if (!SourceVersion.isName(module.replace(".", ""))) {
            continue;
          }
          realm:
          for (var realm : List.of("main", "test")) {
            modules.putIfAbsent(realm, new ArrayList<>());
            for (var zone : List.of("java", "module")) {
              var info = root.resolve(realm).resolve(zone).resolve("module-info.java");
              if (Files.isRegularFile(info)) {
                var patches = new ArrayList<Path>();
                if (realm.equals("test") && modules.get("main").contains(module)) {
                  patches.add(src.resolve(module).resolve("main/java"));
                }
                builder.unit(Modules.describe(Bach.Paths.readString(info)), root, realm, patches);
                modules.get(realm).add(module);
                continue realm; // first zone hit wins
              }
            }
          }
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

      Builder realm(
          String name,
          Set<Realm.Modifier> modifiers,
          List<Path> moduleSourcePaths,
          List<Path> modulePaths) {
        realms.add(new Realm(name, modifiers, moduleSourcePaths, modulePaths));
        return this;
      }

      Builder unit(ModuleDescriptor descriptor, Path root, String realm, List<Path> patches) {
        units.add(new Unit(descriptor, root, realm, patches));
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

      public enum Modifier {
        DOCUMENT,
        TEST
      }

      final String name;
      final Set<Modifier> modifiers;
      final List<Path> sourcePaths;
      final List<Path> modulePaths;

      public Realm(
          String name, Set<Modifier> modifiers, List<Path> sourcePaths, List<Path> modulePaths) {
        this.name = name;
        this.modifiers = modifiers.isEmpty() ? Set.of() : EnumSet.copyOf(modifiers);
        this.sourcePaths = List.copyOf(sourcePaths);
        this.modulePaths = List.copyOf(modulePaths);
      }

      @Override
      public boolean equals(Object that) {
        if (this == that) return true;
        if (that == null || getClass() != that.getClass()) return false;
        return this.hashCode() == that.hashCode();
      }

      @Override
      public int hashCode() {
        return Objects.hash(name, modifiers, sourcePaths, modulePaths);
      }

      List<Unit> units(List<Unit> units) {
        return units.stream().filter(unit -> name.equals(unit.realm)).collect(Collectors.toList());
      }

      String moduleSourcePath() {
        return Bach.Paths.join(sourcePaths).replace("{MODULE}", "*");
      }

      @Override
      public String toString() {
        var pattern = "Realm{'%s', %s, sourcePaths=%s, modulePaths=%s}";
        return String.format(pattern, name, modifiers, sourcePaths, modulePaths);
      }
    }

    public static /*record*/ class Unit {
      final ModuleDescriptor descriptor; // module foo.bar {...}
      final Path root; // "src/foo.bar"
      final String realm; // "main"
      final List<Path> sources;
      final List<Path> resources;
      final List<Path> patches;

      public Unit(ModuleDescriptor descriptor, Path root, String realm, List<Path> patches) {
        this.descriptor = descriptor;
        this.root = root;
        this.realm = realm;
        this.sources = List.of(root.resolve(realm).resolve("java"));
        this.resources = List.of(root.resolve(realm).resolve("resources"));
        this.patches = List.copyOf(patches);
      }

      @Override
      public boolean equals(Object that) {
        if (this == that) return true;
        if (that == null || getClass() != that.getClass()) return false;
        return this.hashCode() == that.hashCode();
      }

      @Override
      public int hashCode() {
        return Objects.hash(descriptor, root, realm, sources, resources, patches);
      }

      String name() {
        return descriptor.name();
      }

      @Override
      public String toString() {
        return "Unit{descriptor=" + descriptor + '}';
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

    public Command(String name, String... args) {
      this.name = name;
      this.arguments = new ArrayList<>(List.of(args));
    }

    public Command(Command that) {
      this.name = that.name;
      this.arguments = new ArrayList<>(that.arguments);
    }

    public Command add(Object object) {
      arguments.add(object.toString());
      return this;
    }

    public Command add(String key, Object value) {
      return add(key).add(value);
    }

    public Command add(String key, List<Path> paths) {
      if (paths.isEmpty()) return this;
      return add(key).add(Paths.join(paths));
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

    @Override
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public Command clone() {
      return new Command(this);
    }

    public String[] toArguments() {
      return arguments.toArray(String[]::new);
    }

    @Override
    public String toString() {
      return "Command{" + "name='" + name + '\'' + ", arguments=" + arguments + '}';
    }
  }

  /** {@link Path}-related helpers. */
  public static class Paths {
    private Paths() {}

    /** Convenient short-cut to {@code "user.home"} as a path. */
    public static final Path USER_HOME = Path.of(System.getProperty("user.home"));

    /** Copy all files and directories from source to target directory. */
    public static void copy(Path source, Path target) {
      copy(source, target, __ -> true);
    }

    /** Copy selected files and directories from source to target directory. */
    public static void copy(Path source, Path target, Predicate<Path> filter) {
      if (!Files.exists(source)) {
        throw new IllegalArgumentException("source must exist: " + source);
      }
      if (!Files.isDirectory(source)) {
        throw new IllegalArgumentException("source must be a directory: " + source);
      }
      if (Files.exists(target)) {
        if (target.equals(source)) return;
        if (!Files.isDirectory(target)) {
          throw new IllegalArgumentException("target must be a directory: " + target);
        }
        if (target.startsWith(source)) { // copy "a/" to "a/b/"...
          throw new IllegalArgumentException("target must not a child of source");
        }
      }
      var options = Set.of(StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
      try (var stream = Files.walk(source).sorted()) {
        var paths = stream.filter(filter).collect(Collectors.toList());
        for (var path : paths) {
          var destination = target.resolve(source.relativize(path).toString());
          var lastModified = Files.getLastModifiedTime(path);
          if (Files.isDirectory(path)) {
            Files.createDirectories(destination);
            Files.setLastModifiedTime(destination, lastModified);
            continue;
          }
          if (Files.exists(destination)) {
            if (lastModified.equals(Files.getLastModifiedTime(destination))) {
              continue;
            }
          }
          Files.copy(path, destination, options.toArray(CopyOption[]::new));
        }
      } catch (Exception e) {
        throw new RuntimeException("copy failed: " + source + " -> " + target, e);
      }
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

    public static List<Path> filter(List<Path> paths, Predicate<Path> filter) {
      return paths.stream().filter(filter).collect(Collectors.toList());
    }

    public static List<Path> filterExisting(List<Path> paths) {
      return filter(paths, Files::exists);
    }

    public static String join(List<Path> paths) {
      return paths.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator));
    }

    public static List<Path> list(Path directory, Predicate<Path> filter) {
      try (var stream = Files.list(directory)) {
        return stream.filter(filter).sorted().collect(Collectors.toList());
      } catch (Exception e) {
        throw new RuntimeException("List directory failed: " + directory, e);
      }
    }

    public static List<Path> list(Path directory, String glob) {
      try (var items = Files.newDirectoryStream(directory, glob)) {
        return StreamSupport.stream(items.spliterator(), false)
            .sorted()
            .collect(Collectors.toList());
      } catch (Exception e) {
        throw new RuntimeException("List directory using glob failed: " + directory, e);
      }
    }

    public static String readString(Path path) {
      try {
        return Files.readString(path);
      } catch (Exception e) {
        throw new Error("Read all content from file failed: " + path, e);
      }
    }

    /** Unzip file "in place". */
    public static Path unzip(Path zip) throws Exception {
      return unzip(zip, zip.toAbsolutePath().getParent());
    }

    /** Unzip file to specified destination directory. */
    public static Path unzip(Path zip, Path destination) throws Exception {
      var loader = Bach.class.getClassLoader();
      try (var zipFileSystem = FileSystems.newFileSystem(zip, loader)) {
        var root = zipFileSystem.getPath(zipFileSystem.getSeparator());
        copy(root, destination);
        // Single subdirectory in root of the zip file?
        var stream = Files.list(root);
        var entries = stream.collect(Collectors.toList());
        if (entries.size() == 1) {
          var singleton = entries.get(0);
          if (Files.isDirectory(singleton)) {
            return destination.resolve(singleton.getFileName().toString());
          }
        }
      }
      return destination;
    }
  }

  /** Uniform Resource Identifier ({@link URI}) read and download support. */
  public static class Uris {

    public static Uris ofSystem() {
      var log = Log.ofSystem();
      var httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
      return new Uris(log, httpClient);
    }

    private final Log log;
    private final HttpClient http;

    public Uris(Log log, HttpClient http) {
      this.log = log;
      this.http = http;
    }

    public HttpResponse<Void> head(URI uri, int timeout) throws Exception {
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
          } else {
            log.warn("No etag provided in response: %s", response);
          }
          var lastModifiedHeader = response.headers().firstValue("last-modified");
          if (lastModifiedHeader.isPresent()) {
            try {
              //noinspection SpellCheckingInspection
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
    public String read(URI uri) throws Exception {
      log.debug("Read %s", uri);
      if ("file".equals(uri.getScheme())) {
        return Files.readString(Path.of(uri));
      }
      var request = HttpRequest.newBuilder(uri).GET();
      return http.send(request.build(), HttpResponse.BodyHandlers.ofString()).body();
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
      return get(command.name).run(log.out, log.err, command.toArguments());
    }

    void maven(Object... args) throws Exception {
      // log.debug("maven(" + List.of(args) + ")");
      var zip =
          Uris.ofSystem()
              .copy(
                  URI.create(
                      "https://archive.apache.org/dist/maven/maven-3/3.6.2/binaries/apache-maven-3.6.2-bin.zip"),
                  Paths.USER_HOME
                      .resolve(".bach/tools")
                      .resolve("maven")
                      .resolve("apache-maven-3.6.2-bin.zip"),
                  StandardCopyOption.COPY_ATTRIBUTES);
      // log.debug("unzip(" + zip + ")");
      var home = Paths.unzip(zip);
      var win = System.getProperty("os.name").toLowerCase().contains("win");
      var name = "mvn" + (win ? ".cmd" : "");
      var executable = home.resolve("bin").resolve(name);
      executable.toFile().setExecutable(true);
      var list = new ArrayList<String>();
      list.add(executable.toString());
      for (var arg : args) list.add(arg.toString());
      var start = Instant.now();
      var process =
          new ProcessBuilder(list)
              .directory(project().base.toAbsolutePath().toFile())
              .inheritIO()
              .start();
      var code = process.waitFor();
      var command = new Command("<tool>", list.toArray(String[]::new));
      log.record(code, Duration.between(start, Instant.now()), command);
      if (code != 0) {
        throw new Error("Non-zero exit code " + code + " for " + String.join(" ", list));
      }
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
      this.modules = Paths.createDirectories(project.paths.modules(realm.name));
    }

    void compile(List<Project.Unit> units) {
      var moduleNames = units.stream().map(Project.Unit::name).collect(Collectors.toList());
      var modulePaths = Paths.filterExisting(List.of(project.paths.lib()));
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
              .forEach(units, this::patchModule));
      for (var unit : units) {
        if (realm.modifiers.contains(Project.Realm.Modifier.DOCUMENT)) {
          jarSources(unit);
          // TODO javadoc(unit);
        }
        jarModule(unit);
      }
    }

    private void patchModule(Command command, Project.Unit unit) {
      if (unit.patches.isEmpty()) return;
      command.add("--patch-module", unit.name() + '=' + Paths.join(unit.patches));
    }

    private void jarModule(Project.Unit unit) {
      var jar = modules.resolve(unit.name() + '-' + project.version(unit) + ".jar");
      var resources = Paths.filterExisting(unit.resources);
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
      var sources = Paths.filterExisting(unit.sources);
      var resources = Paths.filterExisting(unit.resources);
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

  private class Tester {

    Project.Realm realm;

    Tester(Project.Realm realm) {
      this.realm = realm;
    }

    void test(Iterable<Project.Unit> units) {
      log.debug("Launching all tests in realm " + realm);
      for (var unit : units) {
        log.debug("%n%n%n--> %s%n%n%n", unit);
        test(unit);
      }
    }

    private void test(Project.Unit unit) {
      var modulePath = new ArrayList<Path>();
      modulePath.add(project.paths.modules(realm.name));
      modulePath.addAll(realm.modulePaths);
      var layer = layer(modulePath, unit.name());

      var errors = new StringBuilder();
      errors.append(run(layer, "test(" + unit.name() + ")"));
      errors.append(run(layer, "junit", "--select-module", unit.name()));
      if (errors.toString().replace('0', ' ').isBlank()) {
        return;
      }
      throw new AssertionError("Test run failed!");
    }

    private ModuleLayer layer(List<Path> modulePath, String module) {
      var finder = ModuleFinder.of(modulePath.toArray(Path[]::new));
      var roots = List.of(module);
      if (log.verbose) {
        log.debug("Module path:");
        for (var element : modulePath) {
          log.debug("  -> %s", element);
        }
        log.debug("Finder finds module(s):");
        finder.findAll().stream()
            .sorted(Comparator.comparing(ModuleReference::descriptor))
            .forEach(reference -> log.debug("  -> %s", reference));
        log.debug("Root module(s):");
        for (var root : roots) {
          log.debug("  -> %s", root);
        }
      }
      var boot = ModuleLayer.boot();
      var configuration = boot.configuration().resolveAndBind(finder, ModuleFinder.of(), roots);
      var loader = ClassLoader.getPlatformClassLoader();
      var controller = ModuleLayer.defineModulesWithOneLoader(configuration, List.of(boot), loader);
      return controller.layer();
    }

    private int run(ModuleLayer layer, String name, String... args) {
      var serviceLoader = ServiceLoader.load(layer, ToolProvider.class);
      return StreamSupport.stream(serviceLoader.spliterator(), false)
          .filter(provider -> provider.name().equals(name))
          .mapToInt(tool -> Math.abs(run(tool, args)))
          .sum();
    }

    private int run(ToolProvider tool, String... args) {
      var toolLoader = tool.getClass().getClassLoader();
      var currentThread = Thread.currentThread();
      var currentContextLoader = currentThread.getContextClassLoader();
      currentThread.setContextClassLoader(toolLoader);
      try {
        var parent = toolLoader;
        while (parent != null) {
          parent.setDefaultAssertionStatus(true);
          parent = parent.getParent();
        }
        var start = Instant.now();
        int code = tool.run(log.out, log.err, args);
        var command = new Command(tool.name(), args);
        log.record(code, Duration.between(start, Instant.now()), command);
        return code;
      } finally {
        currentThread.setContextClassLoader(currentContextLoader);
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

  static final String VERSION = "2.0-ea";
}
