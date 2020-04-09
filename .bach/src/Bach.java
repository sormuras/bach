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
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.lang.System.Logger.Level;
import java.lang.module.ModuleDescriptor.Requires;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleDescriptor;
import java.net.URI;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumSet;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;
public class Bach {
  public static final Version VERSION = Version.parse("11.0-ea");
  public static void main(String... args) {
    Main.main(args);
  }
  private final Printer printer;
  private final Workspace workspace;
  public Bach() {
    this(Printer.ofSystem(), Workspace.of());
  }
  public Bach(Printer printer, Workspace workspace) {
    this.printer = Objects.requireNonNull(printer, "printer");
    this.workspace = workspace;
    printer.print(
        Level.DEBUG,
        this + " initialized",
        "\tprinter=" + printer,
        "\tWorkspace",
        "\t\tbase='" + workspace.base() + "' -> " + workspace.base().toUri(),
        "\t\tworkspace=" + workspace.workspace());
  }
  public Printer getPrinter() {
    return printer;
  }
  public Workspace getWorkspace() {
    return workspace;
  }
  public void build(Project project) {
    var tasks = new ArrayList<Task>();
    tasks.add(
        Task.parallel(
            "Versions",
            Task.run(new JavaCompiler("--version")),
            Task.run("jar", "--version"),
            Task.run("javadoc", "--version")));
    tasks.add(new ValidateWorkspace());
    tasks.add(new PrintProject(project));
    tasks.add(new ValidateProject(project));
    tasks.add(new CreateDirectories(workspace.workspace()));
    tasks.add(new PrintModules(project));
    var task = new Task("Build project " + project.toNameAndVersion(), false, tasks);
    build(project, task);
  }
  void build(Project project, Task task) {
    var summary = execute(new Task.Executor(this, project), task);
    summary.write("build");
    summary.assertSuccessful();
    printer.print(Level.INFO, "Build took " + summary.toDurationString());
  }
  public void execute(Task task) {
    execute(new Task.Executor(this, null), task).assertSuccessful();
  }
  private Task.Executor.Summary execute(Task.Executor executor, Task task) {
    var size = task.size();
    printer.print(Level.DEBUG, "Execute " + size + " tasks");
    var summary = executor.execute(task);
    printer.print(Level.DEBUG, "Executed " + summary.getTaskCounter() + " of " + size + " tasks");
    var exception = Strings.text(summary.exceptionDetails());
    if (!exception.isEmpty()) printer.print(Level.ERROR, exception);
    return summary;
  }
  public String toString() {
    return "Bach.java " + VERSION;
  }
  static class Main {
    public static void main(String... args) {
      System.out.println("Bach.java " + Bach.VERSION);
    }
  }
  public interface Printer {
    default void print(Level level, String... message) {
      if (!printable(level)) return;
      print(level, Strings.text(message));
    }
    default void print(Level level, Iterable<String> message) {
      if (!printable(level)) return;
      print(level, Strings.text(message));
    }
    boolean printable(Level level);
    void print(Level level, String message);
    static Printer ofSystem() {
      var verbose = Boolean.getBoolean("ebug") || "".equals(System.getProperty("ebug"));
      return ofSystem(verbose ? Level.ALL : Level.INFO);
    }
    static Printer ofSystem(Level threshold) {
      return new Default(Printer::systemPrintLine, threshold);
    }
    static void systemPrintLine(Level level, String message) {
      if (level.getSeverity() <= Level.INFO.getSeverity()) System.out.println(message);
      else System.err.println(message);
    }
    class Default implements Printer {
      private final BiConsumer<Level, String> consumer;
      private final Level threshold;
      public Default(BiConsumer<Level, String> consumer, Level threshold) {
        this.consumer = consumer;
        this.threshold = threshold;
      }
      public boolean printable(Level level) {
        if (threshold == Level.OFF) return false;
        return threshold == Level.ALL || threshold.getSeverity() <= level.getSeverity();
      }
      public void print(Level level, String message) {
        if (!printable(level)) return;
        synchronized (consumer) {
          consumer.accept(level, message);
        }
      }
      public String toString() {
        var levels = EnumSet.range(Level.TRACE, Level.ERROR).stream();
        var map = levels.map(level -> level + ":" + printable(level));
        return "Default[threshold=" + threshold + "] -> " + map.collect(Collectors.joining(" "));
      }
    }
  }
  public static class Project {
    private final String name;
    private final Version version;
    private final Information information;
    private final Structure structure;
    public Project(String name, Version version, Information information, Structure structure) {
      this.name = name;
      this.version = version;
      this.information = information;
      this.structure = structure;
    }
    public String name() {
      return name;
    }
    public Version version() {
      return version;
    }
    public Information information() {
      return information;
    }
    public Structure structure() {
      return structure;
    }
    public String toString() {
      return new StringJoiner(", ", Project.class.getSimpleName() + "[", "]")
          .add("name='" + name + "'")
          .add("version=" + version)
          .add("structure=" + structure)
          .toString();
    }
    public String toNameAndVersion() {
      return name + ' ' + version;
    }
    public Version toModuleVersion(Unit unit) {
      return unit.descriptor().version().orElse(version);
    }
    public List<String> toStrings() {
      var strings = new ArrayList<String>();
      strings.add("Project");
      strings.add("\tname=\"" + name + '"');
      strings.add("\tversion=" + version);
      strings.add("Information");
      strings.add("\tdescription=\"" + information.description() + '"');
      strings.add("\turi=" + information.uri());
      strings.add("Structure");
      strings.add("\tUnits: " + structure.toUnitNames());
      strings.add("\tRealms: " + structure.toRealmNames());
      structure.toMainRealm().ifPresent(realm -> strings.add("\tmain-realm=\"" + realm.name() + '"'));
      for (var realm : structure.realms()) {
        strings.add("\tRealm \"" + realm.name() + '"');
        strings.add("\t\trelease=" + realm.release());
        strings.add("\t\tpreview=" + realm.preview());
        realm.toMainUnit().ifPresent(unit -> strings.add("\t\tmain-unit=" + unit.name()));
        strings.add("\t\tUnits: [" + realm.units().size() + ']');
        for (var unit : realm.units()) {
          var module = unit.descriptor();
          strings.add("\t\tUnit \"" + module.toNameAndVersion() + '"');
          module.mainClass().ifPresent(it -> strings.add("\t\t\tmain-class=" + it));
          var requires = unit.toRequiresNames();
          if (!requires.isEmpty()) strings.add("\t\t\trequires=" + requires);
          strings.add("\t\t\tDirectories: [" + unit.directories().size() + ']');
          for (var directory : unit.directories()) {
            strings.add("\t\t\t" + directory);
          }
        }
      }
      return List.copyOf(strings);
    }
  }
  public static class Information {
    public static Information of() {
      return new Information("", null);
    }
    private final String description;
    private final URI uri;
    public Information(String description, URI uri) {
      this.description = description;
      this.uri = uri;
    }
    public String description() {
      return description;
    }
    public URI uri() {
      return uri;
    }
    public String toString() {
      return new StringJoiner(", ", Information.class.getSimpleName() + "[", "]")
          .add("description='" + description + "'")
          .add("uri=" + uri)
          .toString();
    }
  }
  public static class Structure {
    private final List<Realm> realms;
    private final String mainRealm;
    public Structure(List<Realm> realms, String mainRealm) {
      this.realms = realms;
      this.mainRealm = mainRealm;
    }
    public List<Realm> realms() {
      return realms;
    }
    public String mainRealm() {
      return mainRealm;
    }
    public String toString() {
      return new StringJoiner(", ", Structure.class.getSimpleName() + "[", "]")
          .add("realms=" + realms)
          .add("mainRealm='" + mainRealm + "'")
          .toString();
    }
    public Optional<Realm> toMainRealm() {
      return realms.stream().filter(realm -> realm.name().equals(mainRealm)).findAny();
    }
    public List<String> toRealmNames() {
      return realms.stream().map(Realm::name).collect(Collectors.toList());
    }
    public List<String> toUnitNames() {
      var names = realms.stream().flatMap(realm -> realm.units().stream()).map(Unit::name);
      return names.distinct().sorted().collect(Collectors.toList());
    }
  }
  public static class Directory {
    public enum Type {
      UNKNOWN,
      SOURCE,
      RESOURCE;
      public static Type of(String string) {
        if (string.startsWith("java")) return SOURCE;
        if (string.contains("resource")) return RESOURCE;
        return UNKNOWN;
      }
      public String toMarkdown() {
        return this == SOURCE ? ":scroll:" : this == RESOURCE ? ":books:" : "?";
      }
    }
    public static Directory of(Path path) {
      var name = String.valueOf(path.getFileName());
      var type = Type.of(name);
      var release = javaReleaseFeatureNumber(name);
      return new Directory(path, type, release);
    }
    public static List<Directory> listOf(Path root) {
      if (Files.notExists(root)) return List.of();
      var directories = new ArrayList<Directory>();
      try (var stream = Files.newDirectoryStream(root, Files::isDirectory)) {
        stream.forEach(path -> directories.add(of(path)));
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
      directories.sort(Comparator.comparingInt(Directory::release));
      return List.copyOf(directories);
    }
    static int javaReleaseFeatureNumber(String string) {
      if (string.endsWith("-module")) return 0;
      if (string.endsWith("-preview")) return Runtime.version().feature();
      if (string.startsWith("java-")) return Integer.parseInt(string.substring(5));
      return 0;
    }
    static IntSummaryStatistics javaReleaseStatistics(Stream<Path> paths) {
      var names = paths.map(Path::getFileName).map(Path::toString);
      return names.collect(Collectors.summarizingInt(Directory::javaReleaseFeatureNumber));
    }
    private final Path path;
    private final Type type;
    private final int release;
    public Directory(Path path, Type type, int release) {
      this.path = path;
      this.type = type;
      this.release = release;
    }
    public Path path() {
      return path;
    }
    public Type type() {
      return type;
    }
    public int release() {
      return release;
    }
    public String toString() {
      return new StringJoiner(", ", Directory.class.getSimpleName() + "[", "]")
          .add("path=" + path)
          .add("type=" + type)
          .add("release=" + release)
          .toString();
    }
    public String toMarkdown() {
      return type.toMarkdown() + " `" + path + "`" + (release == 0 ? "" : "@" + release);
    }
  }
  public static class Realm {
    private final String name;
    private final int release;
    private final boolean preview;
    private final List<Unit> units;
    private final String mainUnit;
    public Realm(String name, int release, boolean preview, List<Unit> units, String mainUnit) {
      this.name = name;
      this.release = release;
      this.preview = preview;
      this.units = units;
      this.mainUnit = mainUnit;
    }
    public String name() {
      return name;
    }
    public int release() {
      return release;
    }
    public boolean preview() {
      return preview;
    }
    public List<Unit> units() {
      return units;
    }
    public String mainUnit() {
      return mainUnit;
    }
    public String toString() {
      return new StringJoiner(", ", Realm.class.getSimpleName() + "[", "]")
          .add("name='" + name + "'")
          .add("release=" + release)
          .add("preview=" + preview)
          .add("units=" + units)
          .add("mainUnit=" + mainUnit)
          .toString();
    }
    public Optional<Unit> toMainUnit() {
      return units.stream().filter(unit -> unit.name().equals(mainUnit)).findAny();
    }
  }
  public static class Unit {
    private final ModuleDescriptor descriptor;
    private final List<Directory> directories;
    public Unit(ModuleDescriptor descriptor, List<Directory> directories) {
      this.descriptor = descriptor;
      this.directories = directories;
    }
    public ModuleDescriptor descriptor() {
      return descriptor;
    }
    public List<Directory> directories() {
      return directories;
    }
    public String toString() {
      return new StringJoiner(", ", Unit.class.getSimpleName() + "[", "]")
          .add("descriptor=" + descriptor)
          .add("directories=" + directories)
          .toString();
    }
    public String name() {
      return descriptor.name();
    }
    public List<String> toRequiresNames() {
      var names =
          descriptor.requires().stream()
              .filter(requires -> !requires.modifiers().contains(Requires.Modifier.MANDATED))
              .map(Requires::name);
      return names.sorted().collect(Collectors.toList());
    }
  }
  public static class Task {
    private final String name;
    private final boolean composite;
    private final boolean parallel;
    private final List<Task> subs;
    public Task(String name) {
      this(name, false, false, List.of());
    }
    public Task(String name, boolean parallel, List<Task> subs) {
      this(name, true, parallel, subs);
    }
    public Task(String name, boolean composite, boolean parallel, List<Task> subs) {
      this.name = Objects.requireNonNullElse(name, getClass().getSimpleName());
      this.composite = composite;
      this.parallel = parallel;
      this.subs = subs;
    }
    public String name() {
      return name;
    }
    public boolean composite() {
      return composite;
    }
    public boolean parallel() {
      return parallel;
    }
    public List<Task> subs() {
      return subs;
    }
    public String toString() {
      return new StringJoiner(", ", Task.class.getSimpleName() + "[", "]")
          .add("name='" + name + "'")
          .add("composite=" + composite)
          .add("parallel=" + parallel)
          .add("subs=" + subs)
          .toString();
    }
    public boolean leaf() {
      return !composite;
    }
    public void execute(Execution execution) throws Exception {}
    public int size() {
      var counter = new AtomicInteger();
      walk(task -> counter.incrementAndGet());
      return counter.get();
    }
    void walk(Consumer<Task> consumer) {
      consumer.accept(this);
      for (var sub : subs) sub.walk(consumer);
    }
    public static Task parallel(String name, Task... tasks) {
      return new Task(name, true, List.of(tasks));
    }
    public static Task sequence(String name, Task... tasks) {
      return new Task(name, false, List.of(tasks));
    }
    public static Task run(Tool tool) {
      return run(tool.name(), tool.args().toArray(String[]::new));
    }
    public static Task run(String name, String... args) {
      return run(ToolProvider.findFirst(name).orElseThrow(), args);
    }
    public static Task run(ToolProvider provider, String... args) {
      return new RunTool(provider, args);
    }
    public static class Execution implements Printer {
      private final Bach bach;
      private final String hash = Integer.toHexString(System.identityHashCode(this));
      private final StringWriter out = new StringWriter();
      private final StringWriter err = new StringWriter();
      private final Instant start = Instant.now();
      private Execution(Bach bach) {
        this.bach = bach;
      }
      public Bach getBach() {
        return bach;
      }
      public Writer getOut() {
        return out;
      }
      public Writer getErr() {
        return err;
      }
      public boolean printable(Level level) {
        return true;
      }
      public void print(Level level, String message) {
        var writer = level.getSeverity() <= Level.INFO.getSeverity() ? out : err;
        writer.write(message);
        writer.write(System.lineSeparator());
      }
    }
    static class Executor {
      private static final class Detail {
        private final Task task;
        private final Execution execution;
        private final String caption;
        private final Duration duration;
        private Detail(Task task, Execution execution, String caption, Duration duration) {
          this.task = task;
          this.execution = execution;
          this.caption = caption;
          this.duration = duration;
        }
      }
      private final Bach bach;
      private final Project project;
      private final AtomicInteger counter = new AtomicInteger(0);
      private final Deque<String> overview = new ConcurrentLinkedDeque<>();
      private final Deque<Detail> executions = new ConcurrentLinkedDeque<>();
      Executor(Bach bach, Project project) {
        this.bach = bach;
        this.project = project;
      }
      Summary execute(Task task) {
        var start = Instant.now();
        var throwable = execute(0, task);
        return new Summary(task, Duration.between(start, Instant.now()), throwable);
      }
      private Throwable execute(int depth, Task task) {
        var indent = "\t".repeat(depth);
        var name = task.name;
        var printer = bach.getPrinter();
        printer.print(Level.TRACE, String.format("%s%c %s", indent, task.leaf() ? '*' : '+', name));
        executionBegin(task);
        var execution = new Execution(bach);
        try {
          task.execute(execution);
          var out = execution.out.toString();
          if (!out.isEmpty()) printer.print(Level.DEBUG, Strings.textIndent(indent, out.lines()));
          var err = execution.err.toString();
          if (!err.isEmpty()) printer.print(Level.WARNING, Strings.textIndent(indent, err.lines()));
          if (task.composite()) {
            var stream = task.parallel ? task.subs.parallelStream() : task.subs.stream();
            var errors = stream.map(sub -> execute(depth + 1, sub)).filter(Objects::nonNull);
            var error = errors.findFirst();
            if (error.isPresent()) return error.get();
            printer.print(Level.TRACE, indent + "= " + name);
          }
          executionEnd(task, execution);
        } catch (Throwable throwable) {
          printer.print(Level.ERROR, "Task execution failed: " + throwable);
          return throwable;
        }
        return null;
      }
      private void executionBegin(Task task) {
        if (task.leaf()) return;
        var format = "|   +|%6X|        | %s";
        var thread = Thread.currentThread().getId();
        overview.add(String.format(format, thread, task.name));
      }
      private void executionEnd(Task task, Execution execution) {
        counter.incrementAndGet();
        var format = "|%4c|%6X|%8d| %s";
        var kind = task.leaf() ? ' ' : '=';
        var thread = Thread.currentThread().getId();
        var duration = Duration.between(execution.start, Instant.now());
        var line = String.format(format, kind, thread, duration.toMillis(), task.name);
        if (task.leaf()) {
          var caption = "task-execution-details-" + execution.hash;
          overview.add(line + " [...](#" + caption + ")");
          executions.add(new Detail(task, execution, caption, duration));
          return;
        }
        overview.add(line);
      }
      class Summary {
        private final Task task;
        private final Duration duration;
        private final Throwable exception;
        Summary(Task task, Duration duration, Throwable exception) {
          this.task = task;
          this.duration = duration;
          this.exception = exception;
        }
        void assertSuccessful() {
          if (exception == null) return;
          var message = task.name + " (" + task.getClass().getSimpleName() + ") failed";
          throw new AssertionError(message, exception);
        }
        String toDurationString() {
          return Strings.toString(duration);
        }
        int getTaskCounter() {
          return counter.get();
        }
        List<String> toMarkdown() {
          var md = new ArrayList<String>();
          md.add("# Summary");
          md.add("- Java " + Runtime.version());
          md.add("- " + System.getProperty("os.name"));
          md.add("- Executed task `" + task.name + "`");
          md.add("- Build took " + toDurationString());
          md.addAll(exceptionDetails());
          md.addAll(projectDescription());
          md.addAll(taskExecutionOverview());
          md.addAll(taskExecutionDetails());
          md.addAll(systemProperties());
          return md;
        }
        List<String> exceptionDetails() {
          if (exception == null) return List.of();
          var md = new ArrayList<String>();
          md.add("");
          md.add("## Exception Details");
          var lines = String.valueOf(exception.getMessage()).lines().collect(Collectors.toList());
          md.add("### " + (lines.isEmpty() ? exception.getClass() : lines.get(0)));
          if (lines.size() > 1) md.addAll(lines);
          var stackTrace = new StringWriter();
          exception.printStackTrace(new PrintWriter(stackTrace));
          md.add("```text");
          stackTrace.toString().lines().forEach(md::add);
          md.add("```");
          return md;
        }
        List<String> projectDescription() {
          if (project == null) return List.of();
          var md = new ArrayList<String>();
          md.add("");
          md.add("## Project");
          md.add("- `name` = `\"" + project.name() + "\"`");
          md.add("- `version` = `" + project.version() + "`");
          md.add("- `uri` = " + project.information().uri());
          md.add("- `description` = " + project.information().description());
          md.add("");
          md.add("|Realm|Unit|Directories|");
          md.add("|-----|----|-----------|");
          var structure = project.structure();
          for (var realm : structure.realms()) {
            for (var unit : realm.units()) {
              var directories =
                  unit.directories().stream()
                      .map(Directory::toMarkdown)
                      .collect(Collectors.joining("<br>"));
              var realmName = realm.name();
              var unitName = unit.name();
              md.add(
                  String.format(
                      "| %s | %s | %s",
                      realmName.equals(structure.mainRealm()) ? "**" + realmName + "**" : realmName,
                      unitName.equals(realm.mainUnit()) ? "**" + unitName + "**" : unitName,
                      directories));
            }
          }
          return md;
        }
        List<String> taskExecutionOverview() {
          if (overview.isEmpty()) return List.of();
          var md = new ArrayList<String>();
          md.add("");
          md.add("## Task Execution Overview");
          md.add("|    |Thread|Duration|Caption");
          md.add("|----|-----:|-------:|-------");
          md.addAll(overview);
          return md;
        }
        List<String> taskExecutionDetails() {
          if (executions.isEmpty()) return List.of();
          var md = new ArrayList<String>();
          md.add("");
          md.add("## Task Execution Details");
          md.add("");
          for (var result : executions) {
            md.add("### " + result.caption);
            md.add(" - **" + result.task.name() + "**");
            md.add(" - Started = " + result.execution.start);
            md.add(" - Duration = " + result.duration);
            md.add("");
            var out = result.execution.out.toString();
            if (!out.isBlank()) {
              md.add("Normal (expected) output");
              md.add("```");
              md.add(out.strip());
              md.add("```");
            }
            var err = result.execution.err.toString();
            if (!err.isBlank()) {
              md.add("Error output");
              md.add("```");
              md.add(err.strip());
              md.add("```");
            }
          }
          return md;
        }
        List<String> systemProperties() {
          var md = new ArrayList<String>();
          md.add("");
          md.add("## System Properties");
          System.getProperties().stringPropertyNames().stream()
              .sorted()
              .forEach(key -> md.add(String.format("- `%s`: `%s`", key, systemProperty(key))));
          return md;
        }
        String systemProperty(String systemPropertyKey) {
          var value = System.getProperty(systemPropertyKey);
          if (!"line.separator".equals(systemPropertyKey)) return value;
          var builder = new StringBuilder();
          for (char c : value.toCharArray()) {
            builder.append("0x").append(Integer.toHexString(c).toUpperCase());
          }
          return builder.toString();
        }
        void write(String prefix) {
          @SuppressWarnings("SpellCheckingInspection")
          var pattern = "yyyyMMddHHmmss";
          var formatter = DateTimeFormatter.ofPattern(pattern).withZone(ZoneOffset.UTC);
          var timestamp = formatter.format(Instant.now().truncatedTo(ChronoUnit.SECONDS));
          var workspace = bach.getWorkspace();
          var summary = workspace.workspace("summary", prefix + "-" + timestamp + ".md");
          var markdown = toMarkdown();
          try {
            Files.createDirectories(summary.getParent());
            Files.write(summary, markdown);
            Files.write(workspace.workspace("summary.md"), markdown); // overwrite existing
          } catch (IOException e) {
            throw new UncheckedIOException("Write of " + summary + " failed: " + e, e);
          }
        }
      }
    }
  }
  public static class CreateDirectories extends Task {
    private final Path path;
    public CreateDirectories(Path path) {
      super("Create directories " + path);
      this.path = path;
    }
    public void execute(Execution execution) throws Exception {
      Files.createDirectories(path);
    }
  }
  public static class DeleteDirectories extends Task {
    private final Path path;
    public DeleteDirectories(Path path) {
      super("Delete directories " + path);
      this.path = path;
    }
    public void execute(Execution execution) throws Exception {
      Paths.delete(path, __ -> true);
    }
  }
  public static class PrintModules extends Task {
    private final Project project;
    public PrintModules(Project project) {
      super("Print modules");
      this.project = project;
    }
    public void execute(Execution execution) {
      var workspace = execution.getBach().getWorkspace();
      var realm = project.structure().toMainRealm().orElseThrow();
      for (var unit : realm.units()) {
        var jar = workspace.module(realm.name(), unit.name(), project.toModuleVersion(unit));
        var nameAndVersion = unit.descriptor().toNameAndVersion();
        execution.print(Level.INFO, "Module " + nameAndVersion, "\t-> " + jar.toUri());
      }
    }
  }
  public static class PrintProject extends Task {
    private final Project project;
    public PrintProject(Project project) {
      super("Print project");
      this.project = project;
    }
    public void execute(Execution execution) {
      var structure = project.structure();
      execution.print(Level.INFO, project.toNameAndVersion(), "Units: " + structure.toUnitNames());
      execution.print(Level.DEBUG, project.toStrings());
    }
  }
  public static class RunTool extends Task {
    static String name(String tool, String... args) {
      var length = args.length;
      if (length == 0) return String.format("Run %s", tool);
      if (length == 1) return String.format("Run %s %s", tool, args[0]);
      if (length == 2) return String.format("Run %s %s %s", tool, args[0], args[1]);
      return String.format("Run %s %s %s ... (%d arguments)", tool, args[0], args[1], length);
    }
    private final ToolProvider tool;
    private final String[] args;
    public RunTool(ToolProvider tool, String... args) {
      super(name(tool.name(), args));
      this.tool = tool;
      this.args = args;
    }
    public void execute(Execution execution) {
      var out = execution.getOut();
      var err = execution.getErr();
      var code = tool.run(new PrintWriter(out), new PrintWriter(err), args);
      if (code != 0) {
        var name = tool.name();
        var caption = "Run of " + name + " failed with exit code: " + code;
        var error = Strings.textIndent("\t", Strings.text(err.toString().lines()));
        var lines = Strings.textIndent("\t", Strings.list(name, args));
        var message = Strings.text(caption, "Error:", error, "Tool:", lines);
        throw new AssertionError(message);
      }
    }
  }
  public static class ValidateProject extends Task {
    private final Project project;
    public ValidateProject(Project project) {
      super("Validate project");
      this.project = project;
    }
    public void execute(Execution execution) throws IllegalStateException {
      if (project.structure().toUnitNames().isEmpty()) fail(execution, "no unit present");
    }
    private static void fail(Execution execution, String message) {
      execution.print(Level.ERROR, message);
      throw new IllegalStateException("project validation failed: " + message);
    }
  }
  public static class ValidateWorkspace extends Task {
    public ValidateWorkspace() {
      super("Validate workspace");
    }
    public void execute(Execution execution) {
      var base = execution.getBach().getWorkspace().base();
      if (Paths.isEmpty(base)) execution.print(Level.WARNING, "Empty base directory " + base);
    }
  }
  public static class Tool {
    private final String name;
    private final List<String> args = new ArrayList<>();
    public Tool(String name, Object... arguments) {
      this.name = name;
      addAll(arguments);
    }
    public String name() {
      return name;
    }
    public List<String> args() {
      return List.copyOf(args);
    }
    public Tool add(Object argument) {
      args.add(argument.toString());
      return this;
    }
    public Tool add(String key, Object value) {
      return add(key).add(value);
    }
    public Tool add(String key, Object first, Object second) {
      return add(key).add(first).add(second);
    }
    public Tool add(boolean predicate, Object first, Object... more) {
      return predicate ? add(first).addAll(more) : this;
    }
    public Tool addAll(Object... arguments) {
      for (var argument : arguments) add(argument);
      return this;
    }
    public <T> Tool forEach(Iterable<T> iterable, BiConsumer<Tool, T> visitor) {
      iterable.forEach(argument -> visitor.accept(this, argument));
      return this;
    }
    protected boolean isAssigned(Object object) {
      if (object == null) return false;
      if (object instanceof Number) return ((Number) object).intValue() != 0;
      if (object instanceof Optional) return ((Optional<?>) object).isPresent();
      if (object instanceof Collection) return !((Collection<?>) object).isEmpty();
      return true;
    }
    protected String join(Collection<Path> paths) {
      return Strings.toString(paths).replace("{MODULE}", "*");
    }
    public List<String> toStrings() {
      return Strings.list(name(), args());
    }
  }
  public static class JavaCompiler extends Tool {
    private List<String> compileModulesCheckingTimestamps;
    private Version versionOfModulesThatAreBeingCompiled;
    private List<Path> pathsWhereToFindSourceFilesForModules;
    private List<Path> pathsWhereToFindApplicationModules;
    private Map<String, List<Path>> pathsWhereToFindMoreAssetsPerModule;
    private String characterEncodingUsedBySourceFiles;
    private int compileForVirtualMachineVersion;
    private boolean enablePreviewLanguageFeatures;
    private boolean generateMetadataForMethodParameters;
    private boolean outputMessagesAboutWhatTheCompilerIsDoing;
    private boolean outputSourceLocationsOfDeprecatedUsages;
    private boolean terminateCompilationIfWarningsOccur;
    private Path destinationDirectory;
    public JavaCompiler(Object... arguments) {
      super("javac", arguments);
    }
    public List<String> args() {
      var tool = new Tool("<local>");
      super.args().forEach(tool::add);
      var module = getCompileModulesCheckingTimestamps();
      if (isAssigned(module)) tool.add("--module", String.join(",", module));
      var moduleVersion = getVersionOfModulesThatAreBeingCompiled();
      if (isAssigned(moduleVersion)) tool.add("--module-version", moduleVersion);
      var moduleSourcePath = getPathsWhereToFindSourceFilesForModules();
      if (isAssigned(moduleSourcePath)) tool.add("--module-source-path", join(moduleSourcePath));
      var modulePath = getPathsWhereToFindApplicationModules();
      if (isAssigned(modulePath)) tool.add("--module-path", join(modulePath));
      var modulePatches = getPathsWhereToFindMoreAssetsPerModule();
      if (isAssigned(modulePatches))
        for (var patch : modulePatches.entrySet())
          tool.add("--patch-module", patch.getKey() + '=' + join(patch.getValue()));
      var release = getCompileForVirtualMachineVersion();
      if (isAssigned(release)) tool.add("--release", release);
      if (isEnablePreviewLanguageFeatures()) tool.add("--enable-preview");
      if (isGenerateMetadataForMethodParameters()) tool.add("-parameters");
      if (isOutputSourceLocationsOfDeprecatedUsages()) tool.add("-deprecation");
      if (isOutputMessagesAboutWhatTheCompilerIsDoing()) tool.add("-verbose");
      if (isTerminateCompilationIfWarningsOccur()) tool.add("-Werror");
      var encoding = getCharacterEncodingUsedBySourceFiles();
      if (isAssigned(encoding)) tool.add("-encoding", encoding);
      var destination = getDestinationDirectory();
      if (isAssigned(destination)) tool.add("-d", destination);
      return tool.args();
    }
    public JavaCompiler setCompileModulesCheckingTimestamps(List<String> modules) {
      this.compileModulesCheckingTimestamps = modules;
      return this;
    }
    public List<String> getCompileModulesCheckingTimestamps() {
      return compileModulesCheckingTimestamps;
    }
    public JavaCompiler setPathsWhereToFindSourceFilesForModules(List<Path> moduleSourcePath) {
      this.pathsWhereToFindSourceFilesForModules = moduleSourcePath;
      return this;
    }
    public List<Path> getPathsWhereToFindSourceFilesForModules() {
      return pathsWhereToFindSourceFilesForModules;
    }
    public JavaCompiler setPathsWhereToFindApplicationModules(List<Path> modulePath) {
      this.pathsWhereToFindApplicationModules = modulePath;
      return this;
    }
    public List<Path> getPathsWhereToFindApplicationModules() {
      return pathsWhereToFindApplicationModules;
    }
    public JavaCompiler setDestinationDirectory(Path destinationDirectory) {
      this.destinationDirectory = destinationDirectory;
      return this;
    }
    public Path getDestinationDirectory() {
      return destinationDirectory;
    }
    public JavaCompiler setPathsWhereToFindMoreAssetsPerModule(Map<String, List<Path>> patches) {
      this.pathsWhereToFindMoreAssetsPerModule = patches;
      return this;
    }
    public Map<String, List<Path>> getPathsWhereToFindMoreAssetsPerModule() {
      return pathsWhereToFindMoreAssetsPerModule;
    }
    public JavaCompiler setCharacterEncodingUsedBySourceFiles(String encoding) {
      this.characterEncodingUsedBySourceFiles = encoding;
      return this;
    }
    public String getCharacterEncodingUsedBySourceFiles() {
      return characterEncodingUsedBySourceFiles;
    }
    public JavaCompiler setCompileForVirtualMachineVersion(int release) {
      this.compileForVirtualMachineVersion = release;
      return this;
    }
    public int getCompileForVirtualMachineVersion() {
      return compileForVirtualMachineVersion;
    }
    public JavaCompiler setEnablePreviewLanguageFeatures(boolean enablePreview) {
      this.enablePreviewLanguageFeatures = enablePreview;
      return this;
    }
    public boolean isEnablePreviewLanguageFeatures() {
      return enablePreviewLanguageFeatures;
    }
    public JavaCompiler setGenerateMetadataForMethodParameters(boolean parameters) {
      this.generateMetadataForMethodParameters = parameters;
      return this;
    }
    public boolean isGenerateMetadataForMethodParameters() {
      return generateMetadataForMethodParameters;
    }
    public JavaCompiler setOutputSourceLocationsOfDeprecatedUsages(boolean deprecation) {
      this.outputSourceLocationsOfDeprecatedUsages = deprecation;
      return this;
    }
    public boolean isOutputSourceLocationsOfDeprecatedUsages() {
      return outputSourceLocationsOfDeprecatedUsages;
    }
    public JavaCompiler setOutputMessagesAboutWhatTheCompilerIsDoing(boolean verbose) {
      this.outputMessagesAboutWhatTheCompilerIsDoing = verbose;
      return this;
    }
    public boolean isOutputMessagesAboutWhatTheCompilerIsDoing() {
      return outputMessagesAboutWhatTheCompilerIsDoing;
    }
    public JavaCompiler setTerminateCompilationIfWarningsOccur(boolean warningsAreErrors) {
      this.terminateCompilationIfWarningsOccur = warningsAreErrors;
      return this;
    }
    public boolean isTerminateCompilationIfWarningsOccur() {
      return terminateCompilationIfWarningsOccur;
    }
    public JavaCompiler setVersionOfModulesThatAreBeingCompiled(Version moduleVersion) {
      this.versionOfModulesThatAreBeingCompiled = moduleVersion;
      return this;
    }
    public Version getVersionOfModulesThatAreBeingCompiled() {
      return versionOfModulesThatAreBeingCompiled;
    }
  }
  public static class Paths {
    public static boolean isEmpty(Path path) {
      try {
        if (Files.isRegularFile(path)) return Files.size(path) == 0L;
        try (var stream = Files.list(path)) {
          return stream.findAny().isEmpty();
        }
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
    public static void delete(Path directory, Predicate<Path> filter) throws IOException {
      try {
        Files.deleteIfExists(directory);
        return;
      } catch (DirectoryNotEmptyException __) {
      }
      try (var stream = Files.walk(directory)) {
        var paths = stream.filter(filter).sorted((p, q) -> -p.compareTo(q));
        for (var path : paths.toArray(Path[]::new)) Files.deleteIfExists(path);
      }
    }
    private Paths() {}
  }
  public static class Strings {
    public static List<String> list(String tool, String... args) {
      return list(tool, List.of(args));
    }
    public static List<String> list(String tool, List<String> args) {
      if (args.isEmpty()) return List.of(tool);
      if (args.size() == 1) return List.of(tool + ' ' + args.get(0));
      var strings = new ArrayList<String>();
      strings.add(tool + " with " + args.size() + " arguments:");
      var simple = true;
      for (String arg : args) {
        var minus = arg.startsWith("-");
        strings.add((simple | minus ? "\t" : "\t\t") + arg);
        simple = !minus;
      }
      return List.copyOf(strings);
    }
    public static String text(String... lines) {
      return String.join(System.lineSeparator(), lines);
    }
    public static String text(Iterable<String> lines) {
      return String.join(System.lineSeparator(), lines);
    }
    public static String text(Stream<String> lines) {
      return String.join(System.lineSeparator(), lines.collect(Collectors.toList()));
    }
    public static String textIndent(String indent, String... strings) {
      return indent + String.join(System.lineSeparator() + indent, strings);
    }
    public static String textIndent(String indent, Iterable<String> strings) {
      return indent + String.join(System.lineSeparator() + indent, strings);
    }
    public static String textIndent(String indent, Stream<String> strings) {
      return indent + text(strings.map(string -> indent + string));
    }
    public static String toString(Duration duration) {
      return duration
          .truncatedTo(TimeUnit.MILLISECONDS.toChronoUnit())
          .toString()
          .substring(2)
          .replaceAll("(\\d[HMS])(?!$)", "$1 ")
          .toLowerCase();
    }
    public static String toString(Collection<Path> paths) {
      return paths.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator));
    }
    private Strings() {}
  }
  public static final class Workspace {
    public static Workspace of() {
      return of(Path.of(""));
    }
    public static Workspace of(Path base) {
      return new Workspace(base, base.resolve(".bach/workspace"));
    }
    private final Path base;
    private final Path workspace;
    public Workspace(Path base, Path workspace) {
      this.base = base;
      this.workspace = workspace;
    }
    public Path base() {
      return base;
    }
    public Path workspace() {
      return workspace;
    }
    public String toString() {
      return new StringJoiner(", ", Workspace.class.getSimpleName() + "[", "]")
          .add("base=" + base)
          .add("workspace=" + workspace)
          .toString();
    }
    public Path workspace(String first, String... more) {
      return workspace.resolve(Path.of(first, more));
    }
    public Path classes(String realm, int release) {
      var version = String.valueOf(release == 0 ? Runtime.version().feature() : release);
      return workspace("classes", realm, version);
    }
    public Path modules(String realm) {
      return workspace("modules", realm);
    }
    public Path module(String realm, String module, Version version) {
      return modules(realm).resolve(jarFileName(module, version, ""));
    }
    public String jarFileName(String module, Version version, String classifier) {
      var versionSuffix = version == null ? "" : "-" + version;
      var classifierSuffix = classifier == null || classifier.isEmpty() ? "" : "-" + classifier;
      return module + versionSuffix + classifierSuffix + ".jar";
    }
  }
}
