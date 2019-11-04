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
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;

/**
 * Build modular Java project.
 */
public class Bach {

  /**
   * Create project instance to build.
   */
  public static Project project() {
    return Project.Builder.build(Path.of(""));
  }

  @SuppressWarnings("unused")
  public static void direct(String... args) {
    var tools = new Tools(Log.ofSystem(true));
    tools.run(new Command("javac").add("--version"));
  }

  /**
   * Main entry-point.
   */
  public static void main(String... args) throws Exception {
    var project = project();
    if (args.length == 0) {
      System.out.println();
      System.out.println("Usage: java Bach.java <action> [args...]");
      System.out.println();
      System.out.println("Available actions:");
      System.out.println(" build");
      System.out.println("   Build project " + project.name + " " + project.version);
      System.out.println(" call <entry-point> [args...]");
      System.out.println("   Invoke the named entry-point method. The method signature must match");
      System.out.println("   the Java main default: public static void <name>(String... args) and");
      System.out.println("   it may throw any exception type. Example: java Bach.java call direct");
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

  public void build() {
    log.info("Build of project %s %s started...", project.name, project.version);
    if (log.verbose) {
      log.debug("%nRuntime information");
      log.debug("  - java.version = " + System.getProperty("java.version"));
      log.debug("  - user.dir = " + System.getProperty("user.dir"));
      log.debug("%nProject information");
      project.toSourceLines().forEach(log::debug);
      log.debug("%nTools of the trade");
      tools.print(log.out);
      log.debug("");
    }
    if (project.units.isEmpty()) {
      log.warn("Not a single module unit declared, no build.");
      return;
    }
    var start = Instant.now();
    run(new Command("javac").add("--version").add("-d", Path.of(".")));

    log.info("%nCommand history");
    log.records.forEach(log::info);

    log.info("%nBuild %d took millis.", Duration.between(start, Instant.now()).toMillis());
  }

  /**
   * Run and record the given command instance.
   */
  public void run(Command command) {
    var start = Instant.now();
    int code = tools.run(command);
    log.record(code, Duration.between(start, Instant.now()), command);
    if (code != 0) {
      throw new RuntimeException("Non-zero exit code: " + code);
    }
  }

  /**
   * Simplistic logging support.
   */
  public static class Log {

    /**
     * Create new Log instance using system default text output streams.
     */
    public static Log ofSystem() {
      var verbose = Boolean.getBoolean("verbose");
      var debug = Boolean.getBoolean("ebug") || "".equals(System.getProperty("ebug"));
      return ofSystem(verbose || debug);
    }

    /**
     * Create new Log instance using system default text output streams.
     */
    public static Log ofSystem(boolean verbose) {
      return new Log(new PrintWriter(System.out, true), new PrintWriter(System.err, true), verbose);
    }

    /**
     * Recorded command history.
     */
    private final List<String> records;

    /**
     * Text-output writer.
     */
    private final PrintWriter out, err;
    /**
     * Be verbose.
     */
    private final boolean verbose;

    public Log(PrintWriter out, PrintWriter err, boolean verbose) {
      this.out = out;
      this.err = err;
      this.verbose = verbose;
      this.records = new ArrayList<>();
    }

    /**
     * Print "debug" message to the standard output stream.
     */
    public void debug(String format, Object... args) {
      if (verbose) out.println(String.format(format, args));
    }

    /**
     * Print "information" message to the standard output stream.
     */
    public void info(String format, Object... args) {
      out.println(String.format(format, args));
    }

    /**
     * Print "warn" message to the error output stream.
     */
    public void warn(String format, Object... args) {
      err.println(String.format(format, args));
    }

    public void record(int code, Duration duration, Command command) {
      records.add(String.format("%3d %5d ms %s", code, duration.toMillis(), command.toSource()));
    }
  }

  /**
   * Project model.
   */
  public static class Project {

    final String name;
    final Version version;
    final List<Unit> units;

    public Project(String name, Version version, List<Unit> units) {
      this.name = name;
      this.version = version;
      this.units = units;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      var other = (Project) o;
      return name.equals(other.name) &&
             version.equals(other.version) &&
             units.equals(other.units);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, version, units);
    }

    public List<String> toSourceLines() {
      if (units.isEmpty()) {
        var line = "new Project(%s, Version.parse(%s), List.of())";
        return List.of(String.format(line, $(name), $(version)));
      }
      var lines = new ArrayList<String>();
      lines.add("new Project(");
      lines.add("    " + $(name) + ",");
      lines.add("    Version.parse(" + $(version) + "),");
      lines.add("    List.of(");
      var last = units.get(units.size() - 1);
      for (var unit : units) {
        var comma = unit == last ? "" : ",";
        lines.add("        Project.Unit.of(" + $(unit.info) + ")" + comma);
      }
      lines.add("    )");
      lines.add(")");
      return lines;
    }

    /**
     * Mutable project builder.
     */
    public static class Builder {

      static Project build(Path base) {
        return of(base).build();
      }

      static Builder of(Path base) {
        if (!Files.isDirectory(base)) {
          throw new IllegalArgumentException("Not a directory: " + base);
        }
        var path = base.toAbsolutePath().normalize();
        var builder = new Builder()
            .name(Optional.ofNullable(path.getFileName()).map(Path::toString).orElse("project"))
            .version(System.getProperty(".bach/project.version", "0"));
        try (var stream = Files.find(base, 10, (p, __) -> p.endsWith("module-info.java"))) {
          stream.sorted().forEach(builder::unit);
        } catch (Exception e) {
          throw new Error("Finding module-info.java files failed", e);
        }
        return builder;
      }

      String name = "project";
      Version version = Version.parse("0");
      List<Unit> units = new ArrayList<>();

      Builder name(String name) {
        this.name = name;
        return this;
      }

      Builder version(String version) {
        this.version = Version.parse(version);
        return this;
      }

      Builder unit(Path info) {
        try {
          var descriptor = Modules.describe(Files.readString(info));
          var moduleSourcePath = Modules.moduleSourcePath(info, descriptor.name());
          units.add(new Unit(info, descriptor, moduleSourcePath));
        } catch (Exception e) {
          throw new Error("Reading module declaration failed: " + info, e);
        }
        return this;
      }

      Project build() {
        return new Project(name, version, units);
      }
    }

    public static /*record*/ class Unit {
      /**
       * Path to the backing {@code module-info.java} file.
       */
      final Path info;
      /**
       * Underlying module descriptor.
       */
      final ModuleDescriptor descriptor;
      /**
       * Module source path.
       */
      final String moduleSourcePath;

      public Unit(Path info, ModuleDescriptor descriptor, String moduleSourcePath) {
        this.info = info;
        this.descriptor = descriptor;
        this.moduleSourcePath = moduleSourcePath;
      }

      @Override
      public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var other = (Unit) o;
        return info.equals(other.info) &&
               descriptor.equals(other.descriptor) &&
               moduleSourcePath.equals(other.moduleSourcePath);
      }

      @Override
      public int hashCode() {
        return Objects.hash(info, descriptor, moduleSourcePath);
      }
    }
  }

  /**
   * Static helper for modules and their friends.
   */
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

    private Modules() {
    }

    /**
     * Module descriptor parser.
     */
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

    /**
     * Compute module's source path.
     */
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

  /**
   * Command.
   */
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
      var p = String.join(", ", paths.stream().map(Bach::$).toArray(String[]::new));
      additions.add(String.format(".add(%s, %s)", $(key), p));
      var strings = paths.stream().map(Path::toString);
      return arg(key).arg(strings.collect(Collectors.joining(File.pathSeparator)));
    }

    public String toSource() {
      return String.format("new Command(%s)%s", $(name), String.join("", additions));
    }

    public String[] toStringArray() {
      return arguments.toArray(String[]::new);
    }
  }

  /**
   * Tool registry and command runner support.
   */
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

    ToolProvider get(String name) {
      var tool = map.get(name);
      if (tool == null) {
        throw new NoSuchElementException("No such tool: " + name);
      }
      return tool;
    }

    void print(PrintWriter writer) {
      for (var entry : map.entrySet()) {
        var name = entry.getKey();
        var tool = entry.getValue();
        writer.printf("  - %8s [%s] %s%n", name, Modules.origin(tool), tool);
      }
    }

    int run(Command command) {
      if (log.verbose) {
        var args = command.arguments.isEmpty() ? "" : '"' + String.join("\", \"", command.arguments) + '"';
        log.debug("| %s(%s)", command.name, args);
      }
      return get(command.name).run(log.out, log.err, command.toStringArray());
    }
  }

  static final String VERSION = "2.0-ea";

  static String $(Object object) {
    return object == null ? "null" : "\"" + object.toString().replace("\"", "\\\"") + "\"";
  }

  static String $(Path path) {
    return path == null ? "null" : "Path.of(" + $(path.toString().replace('\\', '/')) + ")";
  }
}
