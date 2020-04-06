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
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
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
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumSet;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
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
    tasks.add(new PrintProject(project));
    tasks.add(new CheckProjectState(project));
    tasks.add(new CreateDirectories(workspace.workspace()));
    tasks.add(new PrintModules(project));
    var build = new Task("Build project " + project.toNameAndVersion(), false, tasks);
    var summary = execute(new Task.Executor(this, project), build);
    summary.write("build");
    summary.assertSuccessful();
    printer.print(Level.INFO, "Build took " + summary.toDurationString());
  }
  public void execute(Task task) {
    execute(new Task.Executor(this, null), task).assertSuccessful();
  }
  Task.Executor.Summary execute(Task.Executor executor, Task task) {
    printer.print(Level.DEBUG, "", "Execute task: " + task.name());
    var summary = executor.execute(task);
    printer.print(Level.DEBUG, "", "Executed tasks: " + summary.getTaskCount());
    return summary;
  }
  public String toString() {
    return "Bach.java " + VERSION;
  }
  public interface Convention {
    static Optional<String> mainClass(Path info, String module) {
      var main = Path.of(module.replace('.', '/'), "Main.java");
      var exists = Files.isRegularFile(info.resolveSibling(main));
      return exists ? Optional.of(module + '.' + "Main") : Optional.empty();
    }
    static Optional<String> mainModule(Stream<ModuleDescriptor> descriptors) {
      var mains = descriptors.filter(d -> d.mainClass().isPresent()).collect(Collectors.toList());
      return mains.size() == 1 ? Optional.of(mains.get(0).name()) : Optional.empty();
    }
    static int javaReleaseFeatureNumber(String string) {
      if (string.endsWith("-module")) return 0;
      if (string.endsWith("-preview")) return Runtime.version().feature();
      if (string.startsWith("java-")) return Integer.parseInt(string.substring(5));
      return 0;
    }
    static IntSummaryStatistics javaReleaseStatistics(Stream<Path> paths) {
      var names = paths.map(Path::getFileName).map(Path::toString);
      return names.collect(Collectors.summarizingInt(Convention::javaReleaseFeatureNumber));
    }
    static void amendJUnitTestEngines(Set<String> modules) {
      if (modules.contains("org.junit.jupiter") || modules.contains("org.junit.jupiter.api"))
        modules.add("org.junit.jupiter.engine");
      if (modules.contains("junit")) {
        modules.add("org.hamcrest");
        modules.add("org.junit.vintage.engine");
      }
    }
    static void amendJUnitPlatformConsole(Set<String> modules) {
      if (modules.contains("org.junit.platform.console")) return;
      var triggers =
          Set.of("org.junit.jupiter.engine", "org.junit.vintage.engine", "org.junit.platform.engine");
      modules.stream()
          .filter(triggers::contains)
          .findAny()
          .ifPresent(__ -> modules.add("org.junit.platform.console"));
    }
  }
  static class Main {
    public static void main(String... args) {
      System.out.println("Bach.java " + Bach.VERSION);
    }
  }
  public interface Printer {
    default void print(Level level, String... message) {
      if (!isPrintable(level)) return;
      print(level, String.join(System.lineSeparator(), message));
    }
    default void print(Level level, Iterable<? extends CharSequence> message) {
      if (!isPrintable(level)) return;
      print(level, String.join(System.lineSeparator(), message));
    }
    default void print(Level level, Stream<? extends CharSequence> message) {
      if (!isPrintable(level)) return;
      print(level, message.collect(Collectors.joining(System.lineSeparator())));
    }
    boolean isPrintable(Level level);
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
      public boolean isPrintable(Level level) {
        if (threshold == Level.OFF) return false;
        return threshold == Level.ALL || threshold.getSeverity() <= level.getSeverity();
      }
      public void print(Level level, String message) {
        if (isPrintable(level)) consumer.accept(level, message);
      }
      public String toString() {
        var levels = EnumSet.range(Level.TRACE, Level.ERROR).stream();
        var map = levels.map(level -> level + ":" + isPrintable(level));
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
    public List<String> toStrings() {
      var strings = new ArrayList<String>();
      strings.add("Project");
      strings.add("\tname=" + name);
      strings.add("\tversion=" + version);
      strings.add("\tdescription=" + information.description());
      strings.add("\turi=" + information.uri());
      strings.add("\tUnits: " + structure.toUnitNames());
      structure.toMainRealm().ifPresent(it -> strings.add("\tmain-realm=" + it.name()));
      strings.add("\tRealms: " + structure.toRealmNames());
      for (var realm : structure.realms()) {
        strings.add("\t\tRealm \"" + realm.name() + '"');
        strings.add("\t\t\trelease=" + realm.release());
        strings.add("\t\t\tpreview=" + realm.preview());
        strings.add("\t\t\tUnits: [" + realm.units().size() + ']');
        for (var unit : realm.units()) {
          var module = unit.descriptor();
          strings.add("\t\t\t\tUnit \"" + module.toNameAndVersion() + '"');
          module.mainClass().ifPresent(it -> strings.add("\t\t\t\t\tmain-class=" + it));
          var requires = unit.toRequiresNames();
          if (!requires.isEmpty()) strings.add("\t\t\t\t\trequires=" + requires);
          strings.add("\t\t\t\t\tDirectories: [" + unit.directories().size() + ']');
          for (var directory : unit.directories()) {
            strings.add("\t\t\t\t\t\t" + directory);
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
      if (realms.isEmpty()) return Optional.empty();
      if (realms.size() == 1) return Optional.of(realms.get(0));
      if (mainRealm == null) return Optional.empty();
      return realms.stream().filter(realm -> realm.name().equals(mainRealm)).findAny();
    }
    public List<String> toRealmNames() {
      return realms.stream().map(Realm::name).collect(Collectors.toList());
    }
    public List<String> toUnitNames() {
      return realms.stream()
          .flatMap(realm -> realm.units().stream())
          .map(Unit::name)
          .distinct()
          .sorted()
          .collect(Collectors.toList());
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
      var release = Convention.javaReleaseFeatureNumber(name);
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
    public Realm(String name, int release, boolean preview, List<Unit> units) {
      this.name = name;
      this.release = release;
      this.preview = preview;
      this.units = units;
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
    public String toString() {
      return new StringJoiner(", ", Realm.class.getSimpleName() + "[", "]")
          .add("name='" + name + "'")
          .add("release=" + release)
          .add("preview=" + preview)
          .add("units=" + units)
          .toString();
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
    private final boolean parallel;
    private final List<Task> subs;
    public Task(String name) {
      this(name, false, List.of());
    }
    public Task(String name, boolean parallel, List<Task> subs) {
      this.name = Objects.requireNonNullElse(name, getClass().getSimpleName());
      this.parallel = parallel;
      this.subs = subs;
    }
    public String name() {
      return name;
    }
    public void execute(Execution execution) throws Exception {}
    public static Task parallel(String name, Task... tasks) {
      return new Task(name, true, List.of(tasks));
    }
    public static Task sequence(String name, Task... tasks) {
      return new Task(name, false, List.of(tasks));
    }
    public static class Execution implements Printer {
      private final Bach bach;
      private final String indent;
      private final String hash = Integer.toHexString(System.identityHashCode(this));
      private final StringWriter out = new StringWriter();
      private final StringWriter err = new StringWriter();
      private final Instant start = Instant.now();
      private Execution(Bach bach, String indent) {
        this.bach = bach;
        this.indent = indent;
      }
      public Bach getBach() {
        return bach;
      }
      public boolean isPrintable(Level level) {
        return true;
      }
      public void print(Level level, String message) {
        bach.getPrinter().print(level, message.lines().map(line -> indent + line));
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
        var subs = task.subs;
        var flat = subs.isEmpty(); // i.e. no sub tasks
        var printer = bach.getPrinter();
        printer.print(Level.TRACE, String.format("%s%c %s", indent, flat ? '*' : '+', name));
        executionBegin(task);
        var execution = new Execution(bach, indent);
        try {
          task.execute(execution);
          if (!flat) {
            var stream = task.parallel ? subs.parallelStream() : subs.stream();
            var errors = stream.map(sub -> execute(depth + 1, sub)).filter(Objects::nonNull);
            var error = errors.findFirst();
            if (error.isPresent()) return error.get();
            printer.print(Level.TRACE, indent + "= " + name);
          }
          executionEnd(task, execution);
        } catch (Exception exception) {
          printer.print(Level.ERROR, "Task execution failed: " + exception);
          return exception;
        }
        return null;
      }
      private void executionBegin(Task task) {
        if (task.subs.isEmpty()) return;
        var format = "|   +|%6X|        | %s";
        var thread = Thread.currentThread().getId();
        overview.add(String.format(format, thread, task.name));
      }
      private void executionEnd(Task task, Execution execution) {
        var format = "|%4c|%6X|%8d| %s";
        var flat = task.subs.isEmpty();
        var kind = flat ? ' ' : '=';
        var thread = Thread.currentThread().getId();
        var duration = Duration.between(execution.start, Instant.now());
        var line = String.format(format, kind, thread, duration.toMillis(), task.name);
        if (flat) {
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
          return duration
              .truncatedTo(TimeUnit.MILLISECONDS.toChronoUnit())
              .toString()
              .substring(2)
              .replaceAll("(\\d[HMS])(?!$)", "$1 ")
              .toLowerCase();
        }
        int getTaskCount() {
          return executions.size();
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
          for (var realm : project.structure().realms()) {
            for (var unit : realm.units()) {
              var directories =
                  unit.directories().stream()
                      .map(Directory::toMarkdown)
                      .collect(Collectors.joining("<br>"));
              md.add(String.format("| %s | %s | %s", realm.name(), unit.name(), directories));
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
            Files.write(workspace.workspace("summary.md"), markdown); // replace existing
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        }
      }
    }
  }
  public static class CheckProjectState extends Task {
    private final Project project;
    public CheckProjectState(Project project) {
      super("Check project state");
      this.project = project;
    }
    public void execute(Execution context) throws IllegalStateException {
      if (project.structure().toUnitNames().isEmpty()) fail("no unit present");
    }
    private static void fail(String message) {
      throw new IllegalStateException("project validation failed: " + message);
    }
  }
  public static class CreateDirectories extends Task {
    private final Path path;
    public CreateDirectories(Path path) {
      super("Create directories " + path);
      this.path = path;
    }
    public void execute(Execution context) throws Exception {
      Files.createDirectories(path);
    }
  }
  public static class DeleteDirectories extends Task {
    private final Path path;
    public DeleteDirectories(Path path) {
      super("Delete directories " + path);
      this.path = path;
    }
    public void execute(Execution context) throws Exception {
      delete(path, __ -> true);
    }
    static void delete(Path directory, Predicate<Path> filter) throws Exception {
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
  }
  public static class PrintModules extends Task {
    private final Project project;
    public PrintModules(Project project) {
      super("Print modules");
      this.project = project;
    }
    public void execute(Execution execution) {
      var realm = project.structure().realms().get(0);
      for (var unit : realm.units()) {
        var jar = execution.getBach().getWorkspace().jarFilePath(project, realm, unit);
        execution.print(
            Level.INFO, "Unit " + unit.descriptor().toNameAndVersion(), "\t-> " + jar.toUri());
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
      execution.print(Level.INFO, project.toStrings());
    }
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
    public Path classes(Realm realm) {
      return classes(realm, realm.release());
    }
    public Path classes(Realm realm, int release) {
      var version = String.valueOf(release == 0 ? Runtime.version().feature() : release);
      return workspace.resolve("classes").resolve(realm.name()).resolve(version);
    }
    public Path modules(Realm realm) {
      return workspace.resolve("modules").resolve(realm.name());
    }
    public String jarFileName(Project project, Unit unit, String classifier) {
      var unitVersion = unit.descriptor().version();
      var moduleVersion = unitVersion.isPresent() ? unitVersion : Optional.ofNullable(project.version());
      var versionSuffix = moduleVersion.map(v -> "-" + v).orElse("");
      var classifierSuffix = classifier.isEmpty() ? "" : "-" + classifier;
      return unit.name() + versionSuffix + classifierSuffix + ".jar";
    }
    public Path jarFilePath(Project project, Realm realm, Unit unit) {
      return modules(realm).resolve(jarFileName(project, unit, ""));
    }
  }
}
