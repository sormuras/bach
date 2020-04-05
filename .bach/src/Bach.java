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
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.lang.System.Logger.Level;
import java.lang.module.ModuleDescriptor.Requires;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleDescriptor;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
public class Bach {
  public static final Version VERSION = Version.parse("11.0-ea");
  public static void main(String... args) {
    Main.main(args);
  }
  private final BiConsumer<Level, String> printer;
  private final boolean verbose;
  private final boolean dryRun;
  private final Workspace workspace;
  public Bach() {
    this(
        (__, message) -> System.out.println(message),
        Boolean.getBoolean("ebug") || "".equals(System.getProperty("ebug")),
        Boolean.getBoolean("ry-run") || "".equals(System.getProperty("ry-run")),
        Workspace.of());
  }
  public Bach(
      BiConsumer<Level, String> printer, boolean verbose, boolean dryRun, Workspace workspace) {
    this.printer = Objects.requireNonNull(printer, "printer");
    this.verbose = verbose;
    this.dryRun = dryRun;
    this.workspace = workspace;
    print(
        Level.DEBUG,
        String.join(
            System.lineSeparator(),
            this + " initialized",
            "\tverbose=" + verbose,
            "\tdry-run=" + dryRun,
            "\tWorkspace",
            String.format("\t\tbase='%s' -> %s", workspace.base(), workspace.base().toUri()),
            "\t\tworkspace=" + workspace.workspace()));
  }
  public boolean isVerbose() {
    return verbose;
  }
  public boolean isDryRun() {
    return dryRun;
  }
  public Workspace getWorkspace() {
    return workspace;
  }
  public void print(Level level, String message) {
    if (verbose || level.getSeverity() >= Level.INFO.getSeverity()) printer.accept(level, message);
  }
  public void build(Project project) {
    var tasks = new ArrayList<Task>();
    tasks.add(new PrintProject(project));
    tasks.add(new CheckProjectState(project));
    tasks.add(new CreateDirectories(workspace.workspace()));
    tasks.add(new PrintModules(project));
    if (dryRun) return;
    execute(new Task("Build project " + project.toNameAndVersion(), false, tasks));
  }
  public void execute(Task task) {
    var executor = new Task.Executor(this);
    print(Level.TRACE, String.join(System.lineSeparator(), "", "Execute task: " + task.name()));
    var summary = executor.execute(task).assertSuccessful();
    if (verbose) {
      print(
          Level.TRACE,
          String.join(
              System.lineSeparator(),
              "",
              "Task Execution Overview",
              "|    |Thread|Duration| Task",
              String.join(System.lineSeparator(), summary.getOverviewLines())));
    }
    var count = summary.getTaskCount();
    var duration = summary.getDuration().toMillis();
    print(
        Level.DEBUG,
        String.join(
            System.lineSeparator(),
            String.format("Execution of %d tasks took %d ms", count, duration)));
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
  public static class Project {
    private final String name;
    private final Version version;
    private final Structure structure;
    public Project(String name, Version version, Structure structure) {
      this.name = name;
      this.version = version;
      this.structure = structure;
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
      strings.add("\tUnits: " + structure.toUnitNames());
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
  public static class Structure {
    private final List<Realm> realms;
    public Structure(List<Realm> realms) {
      this.realms = realms;
    }
    public List<Realm> realms() {
      return realms;
    }
    public String toString() {
      return new StringJoiner(", ", Structure.class.getSimpleName() + "[", "]")
          .add("realms=" + realms)
          .toString();
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
      SOURCE, RESOURCE, UNDEFINED;
      public static Type of(String name) {
        if (name.startsWith("java")) return SOURCE;
        if (name.contains("resource")) return RESOURCE;
        return UNDEFINED;
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
    public static class Execution {
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
      public void print(Level level, String message) {
        var LS = System.lineSeparator();
        bach.print(level, message.lines().map(line -> indent + line).collect(Collectors.joining(LS)));
        var writer = level.getSeverity() <= Level.INFO.getSeverity() ? out : err;
        var enable = writer == err || level == Level.INFO || bach.isVerbose();
        if (enable) writer.write(message + LS);
      }
    }
    static class Executor {
      private final Bach bach;
      private final Deque<String> overview = new ConcurrentLinkedDeque<>();
      private final Deque<Execution> executions = new ConcurrentLinkedDeque<>();
      Executor(Bach bach) {
        this.bach = bach;
      }
      Summary execute(Task root) {
        var start = Instant.now();
        var throwable = execute(0, root);
        return new Summary(root.name, Duration.between(start, Instant.now()), throwable);
      }
      private Throwable execute(int depth, Task task) {
        var indent = "\t".repeat(depth);
        var name = task.name;
        var subs = task.subs;
        var flat = subs.isEmpty(); // i.e. no sub tasks
        bach.print(Level.TRACE, String.format("%s%c %s", indent, flat ? '*' : '+', name));
        executionBegin(task);
        var execution = new Execution(bach, indent);
        try {
          task.execute(execution);
          if (!flat) {
            var stream = task.parallel ? subs.parallelStream() : subs.stream();
            var errors = stream.map(sub -> execute(depth + 1, sub)).filter(Objects::nonNull);
            var error = errors.findFirst();
            if (error.isPresent()) return error.get();
            bach.print(Level.TRACE, indent + "= " + name);
          }
          executionEnd(task, execution);
        } catch (Exception exception) {
          bach.print(Level.ERROR, "Task execution failed: " + exception);
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
        var millis = Duration.between(execution.start, Instant.now()).toMillis();
        var line = String.format(format, kind, thread, millis, task.name);
        if (flat) {
          overview.add(line + " [...](#task-execution-details-" + execution.hash + ")");
          executions.add(execution);
          return;
        }
        overview.add(line);
      }
      class Summary {
        private final String title;
        private final Duration duration;
        private final Throwable throwable;
        Summary(String title, Duration duration, Throwable throwable) {
          this.title = title;
          this.duration = duration;
          this.throwable = throwable;
        }
        Summary assertSuccessful() {
          if (throwable == null) return this;
          throw new AssertionError(title + " failed", throwable);
        }
        Duration getDuration() {
          return duration;
        }
        Deque<String> getOverviewLines() {
          return overview;
        }
        int getTaskCount() {
          return executions.size();
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
            Level.INFO,
            String.join(
                System.lineSeparator(),
                "Unit " + unit.descriptor().toNameAndVersion(),
                "\t-> " + jar.toUri()));
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
      execution.print(Level.INFO, String.join(System.lineSeparator(), project.toStrings()));
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
