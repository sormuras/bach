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
import java.lang.module.FindException;
import java.lang.module.ModuleDescriptor.Requires;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.module.ResolutionException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
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
import java.util.HashMap;
import java.util.IntSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
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
  private final Supplier<HttpClient> httpClient;
  public Bach() {
    this(Printer.ofSystem(), Workspace.of(), HttpClient.newBuilder()::build);
  }
  public Bach(Printer printer, Workspace workspace, Supplier<HttpClient> httpClient) {
    this.printer = Objects.requireNonNull(printer, "printer");
    this.workspace = Objects.requireNonNull(workspace, "workspace");
    this.httpClient = Functions.memoize(httpClient);
    printer.print(
        Level.DEBUG,
        this + " initialized",
        "\tprinter=" + printer,
        "\tWorkspace",
        "\t\tbase='" + workspace.base() + "' -> " + workspace.base().toUri(),
        "\t\tlib='" + workspace.lib(),
        "\t\tworkspace=" + workspace.workspace());
  }
  public Printer getPrinter() {
    return printer;
  }
  public Workspace getWorkspace() {
    return workspace;
  }
  public HttpClient getHttpClient() {
    return httpClient.get();
  }
  public void build(Project project) {
    build(project, new BuildTaskFactory(workspace, project, printer.printable(Level.DEBUG)).get());
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
      strings.add("\tDeclared modules: " + structure.toDeclaredModuleNames());
      strings.add("\tRequired modules: " + structure.toRequiredModuleNames());
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
      strings.add("Library");
      strings.add("\tlocator=" + structure.library().locator());
      strings.add("\trequires=" + structure.library().requires());
      return List.copyOf(strings);
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
  public static final class Library {
    public static Library of(String... requires) {
      return new Library(Locator.of(), Set.of(requires));
    }
    private final Locator locator;
    private final Set<String> requires;
    public Library(Locator locator, Set<String> requires) {
      this.locator = locator;
      this.requires = requires;
    }
    public Locator locator() {
      return locator;
    }
    public Set<String> requires() {
      return requires;
    }
  }
  static abstract class JUnit5Modules extends Locator.AbstractLocator {
    private final String group;
    private final String version;
    JUnit5Modules(String group, String version) {
      this.group = group;
      this.version = version;
    }
    void put(String suffix, long size, String md5) {
      var module = group + suffix;
      var artifact = module.substring(4).replace('.', '-');
      var gav = String.join(":", group, artifact, version);
      var central = Maven.central(gav, module, size, md5);
      put(module, central);
    }
  }
  public static class JUnitJupiterModules extends JUnit5Modules {
    public JUnitJupiterModules() {
      super("org.junit.jupiter", "5.6.2");
      put("", 6359, "f516ecfd11b70dc28a1793ae5e48c6ea");
      put(".api", 154036, "134c39075fcc504a722b1b33432a1111");
      put(".engine", 209317, "34cae629d115762add3318dcc902706f");
      put(".params", 562271, "0dc5639e8cfec8b920869f1ee16746c2");
    }
  }
  public static class JUnitPlatformModules extends JUnit5Modules {
    public JUnitPlatformModules() {
      super("org.junit.platform", "1.6.2");
      put(".commons", 96675, "827619f760062525354d47befc86ff9b");
      put(".console", 433740, "c86a03b3bc95477ae55453e2d9dc4212");
      put(".engine", 174108, "b41ff34208cb373de0bf954e70c4d78b");
      put(".launcher", 121929, "efed110dfb13f33a7787b16cfbf8cd2e");
      put(".reporting", 22426, "f99152f2cd481166abf64109b3308825");
      put(".testkit", 42956, "d7a063edea927c01d7eb6d45475f675b");
      put(
          "org.apiguardian.api",
          "org.apiguardian:apiguardian-api:1.1.0",
          2387,
          "944805817b648e558ed6be6fc7f054f3");
      put(
          "org.opentest4j",
          "org.opentest4j:opentest4j:1.2.0",
          7653,
          "45c9a837c21f68e8c93e85b121e2fb90");
    }
  }
  public static class JUnitVintageModules extends JUnit5Modules {
    public JUnitVintageModules() {
      super("org.junit.vintage", "5.6.2");
      put(".engine", 63769, "5e5be4d146a53451aef718a4e6438ecf");
      put("junit", "junit:junit:4.13", 381765, "5da6445d7b80aba2623e73d4561dcfde");
      put("org.hamcrest", "org.hamcrest:hamcrest:2.2", 123360, "10b47e837f271d0662f28780e60388e8");
    }
  }
  public interface Locator extends Function<String, URI> {
    URI apply(String module);
    static Locator of() {
      return new DefaultLocator();
    }
    static Map<String, String> parseFragment(String fragment) {
      if (fragment.isEmpty()) return Map.of();
      if (fragment.length() < "a=b".length())
        throw new IllegalArgumentException("Fragment too short: " + fragment);
      if (fragment.indexOf('=') == -1)
        throw new IllegalArgumentException("At least one = expected: " + fragment);
      var map = new LinkedHashMap<String, String>();
      var parts = fragment.split("[&=]");
      for (int i = 0; i < parts.length; i += 2) map.put(parts[i], parts[i + 1]);
      return map;
    }
    static String toFragment(Map<String, String> map) {
      var joiner = new StringJoiner("&");
      map.forEach((key, value) -> joiner.add(key + '=' + value));
      return joiner.toString();
    }
    abstract class AbstractLocator extends TreeMap<String, String> implements Locator {
      public String put(String module, String gav, long size, String md5) {
        return put(module, Maven.central(gav, module, size, md5));
      }
      public URI apply(String module) {
        var uri = get(module);
        if (uri == null) return null;
        return URI.create(uri);
      }
      public String toString() {
        return getClass().getSimpleName() + " [" + size() + " modules]";
      }
    }
    class DefaultLocator extends AbstractLocator {
      public DefaultLocator() {
        putAll(new JUnitPlatformModules());
        putAll(new JUnitJupiterModules());
        putAll(new JUnitVintageModules());
      }
    }
    interface Maven {
      String CENTRAL_REPOSITORY = "https://repo.maven.apache.org/maven2";
      static String central(String mavenGroupArtifactVersion, String module, long size, String md5) {
        var coordinates = mavenGroupArtifactVersion.split(":");
        var group = coordinates[0];
        var artifact = coordinates[1];
        var version = coordinates[2];
        var classifier = coordinates.length < 4 ? "" : coordinates[3];
        var ssp = maven(CENTRAL_REPOSITORY, group, artifact, version, classifier);
        var attributes = new LinkedHashMap<String, String>();
        attributes.put("module", module);
        attributes.put("version", version);
        if (size >= 0) attributes.put("size", Long.toString(size));
        if (!md5.isEmpty()) attributes.put("md5", md5);
        return ssp + '#' + toFragment(attributes);
      }
      static String central(String group, String artifact, String version) {
        return maven(CENTRAL_REPOSITORY, group, artifact, version, "");
      }
      static String maven(String repository, String g, String a, String v, String classifier) {
        var filename = a + '-' + (classifier.isEmpty() ? v : v + '-' + classifier);
        var joiner = new StringJoiner("/").add(repository);
        joiner.add(g.replace('.', '/')).add(a).add(v).add(filename + ".jar");
        return joiner.toString();
      }
    }
  }
  public static class Realm {
    private final String name;
    private final List<Unit> units;
    private final String mainUnit;
    private final JavaCompiler javac;
    public Realm(String name, List<Unit> units, String mainUnit, JavaCompiler javac) {
      this.name = name;
      this.units = units;
      this.mainUnit = mainUnit;
      this.javac = javac;
    }
    public String name() {
      return name;
    }
    public int release() {
      return javac.release();
    }
    public boolean preview() {
      return javac.preview();
    }
    public List<Unit> units() {
      return units;
    }
    public String mainUnit() {
      return mainUnit;
    }
    public JavaCompiler javac() {
      return javac;
    }
    public String toString() {
      return new StringJoiner(", ", Realm.class.getSimpleName() + "[", "]")
          .add("name='" + name + "'")
          .add("release=" + release())
          .add("preview=" + preview())
          .add("units=" + units)
          .add("mainUnit=" + mainUnit)
          .add("javac=" + javac)
          .toString();
    }
    public Optional<Unit> toMainUnit() {
      return units.stream().filter(unit -> unit.name().equals(mainUnit)).findAny();
    }
  }
  public static class Structure {
    private final List<Realm> realms;
    private final String mainRealm;
    private final Library library;
    public Structure(List<Realm> realms, String mainRealm, Library library) {
      this.realms = realms;
      this.mainRealm = mainRealm;
      this.library = library;
    }
    public List<Realm> realms() {
      return realms;
    }
    public String mainRealm() {
      return mainRealm;
    }
    public Library library() {
      return library;
    }
    public String toString() {
      return new StringJoiner(", ", Structure.class.getSimpleName() + "[", "]")
          .add("realms=" + realms)
          .add("mainRealm='" + mainRealm + "'")
          .add("library=" + library)
          .toString();
    }
    public Optional<Realm> toMainRealm() {
      return realms.stream().filter(realm -> realm.name().equals(mainRealm)).findAny();
    }
    public List<String> toRealmNames() {
      return realms.stream().map(Realm::name).collect(Collectors.toList());
    }
    public Set<String> toDeclaredModuleNames() {
      var names = realms.stream().flatMap(realm -> realm.units().stream()).map(Unit::name);
      return names.sorted().collect(Collectors.toCollection(TreeSet::new));
    }
    public Set<String> toRequiredModuleNames() {
      return Modules.required(realms.stream().flatMap(realm -> realm.units().stream()).map(Unit::descriptor));
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
      return run(tool.name(), tool.toArgumentStrings().toArray(String[]::new));
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
  public static class BuildTaskFactory implements Supplier<Task> {
    private final Workspace workspace;
    private final Project project;
    private final boolean verbose;
    public BuildTaskFactory(Workspace workspace, Project project, boolean verbose) {
      this.workspace = workspace;
      this.project = project;
      this.verbose = verbose;
    }
    public Task get() {
      return Task.sequence(
          "Build project " + project.toNameAndVersion(),
          printVersionInformationOfFoundationTools(),
          new ValidateWorkspace(),
          new PrintProject(project),
          new ValidateProject(project),
          new CreateDirectories(workspace.workspace()),
          new ResolveMissingModules(project),
          compileAllRealms(),
          new PrintModules(project));
    }
    protected Task printVersionInformationOfFoundationTools() {
      return verbose
          ? Task.parallel(
              "Print version of various foundation tools",
              Task.run(Tool.of("javac", "--version")),
              Task.run("jar", "--version"),
              Task.run("javadoc", "--version"))
          : Task.sequence("Print version of javac", Task.run("javac", "--version"));
    }
    protected Task compileAllRealms() {
      var realms = project.structure().realms();
      var tasks = realms.stream().map(this::compileRealm);
      return Task.sequence("Compile all realms", tasks.toArray(Task[]::new));
    }
    protected Task compileRealm(Realm realm) {
      return Task.sequence(
          "Compile " + realm.name() + " realm", Task.run(realm.javac()), createArchives(realm)
          );
    }
    protected Task createArchives(Realm realm) {
      var jars = new ArrayList<Task>();
      for (var unit : realm.units()) {
        jars.add(createArchive(realm, unit));
      }
      return Task.sequence(
          "Package " + realm.name() + " modules and sources",
          new CreateDirectories(workspace.modules(realm.name())),
          Task.parallel("Jar each " + realm.name() + " module", jars.toArray(Task[]::new)));
    }
    protected Task createArchive(Realm realm, Unit unit) {
      var file = workspace.module(realm.name(), unit.name(), project.toModuleVersion(unit));
      var options = new ArrayList<Option>();
      options.add(new JavaArchiveTool.PerformOperation(JavaArchiveTool.Operation.CREATE));
      options.add(new JavaArchiveTool.ArchiveFile(file));
      var root = workspace.classes(realm.name(), realm.release()).resolve(unit.name());
      options.add(new JavaArchiveTool.ChangeDirectory(root));
      return Task.run(Tool.jar(options));
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
    public void execute(Execution execution) throws Exception {
      var workspace = execution.getBach().getWorkspace();
      var realm = project.structure().toMainRealm().orElseThrow();
      for (var unit : realm.units()) {
        var jar = workspace.module(realm.name(), unit.name(), project.toModuleVersion(unit));
        execution.print(Level.INFO, "file: " + jar.getFileName(), "size: " + Files.size(jar));
        var out = new PrintWriter(execution.getOut());
        var err = new PrintWriter(execution.getErr());
        ToolProvider.findFirst("jar")
            .orElseThrow()
            .run(out, err, "--describe-module", "--file", jar.toString());
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
      execution.print(Level.INFO, project.toNameAndVersion(), "Units: " + structure.toDeclaredModuleNames());
      execution.print(Level.DEBUG, project.toStrings());
    }
  }
  public static class ResolveMissingModules extends Task {
    private final Project project;
    public ResolveMissingModules(Project project) {
      super("Resolve missing modules");
      this.project = project;
    }
    public void execute(Execution execution) {
      var structure = project.structure();
      var library = structure.library();
      var directory = execution.getBach().getWorkspace().lib();
      var transporter = new Transporter(execution.getBach(), directory, library.locator());
      var declared = structure.toDeclaredModuleNames();
      var requires = new TreeSet<String>();
      requires.addAll(structure.toRequiredModuleNames());
      requires.addAll(library.requires());
      var resolver = new ModulesResolver(new Path[] {directory}, declared, transporter);
      resolver.resolve(requires);
    }
    private static class Transporter implements Consumer<Set<String>> {
      private final Bach bach;
      private final Path directory;
      private final Locator locator;
      private Transporter(Bach bach, Path directory, Locator locator) {
        this.bach = bach;
        this.directory = directory;
        this.locator = locator;
      }
      public void accept(Set<String> modules) {
        var resources = new Resources(bach.getHttpClient());
        for (var module : modules) {
          var uri = locator.apply(module);
          if (uri == null) throw new FindException("Module " + module + " not locatable: " + locator);
          var attributes = Locator.parseFragment(uri.getFragment());
          var version = Optional.ofNullable(attributes.get("version"));
          var jar = module + version.map(v -> '@' + v).orElse("") + ".jar";
          try {
            resources.copy(uri, directory.resolve(jar));
          } catch (Exception e) {
            throw new ResolutionException("Copy failed for: " + uri, e);
          }
        }
      }
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
      if (project.structure().toDeclaredModuleNames().isEmpty()) fail(execution, "no unit present");
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
  public static class Arguments {
    private final List<String> list = new ArrayList<>();
    public Arguments(Object... arguments) {
      addAll(arguments);
    }
    public List<String> build() {
      return List.copyOf(list);
    }
    public Arguments add(Object argument) {
      list.add(argument.toString());
      return this;
    }
    public Arguments add(String key, Object value) {
      return add(key).add(value);
    }
    public Arguments add(String key, Object first, Object second) {
      return add(key).add(first).add(second);
    }
    public Arguments add(boolean predicate, Object first, Object... more) {
      return predicate ? add(first).addAll(more) : this;
    }
    @SafeVarargs
    public final <T> Arguments addAll(T... arguments) {
      for (var argument : arguments) add(argument);
      return this;
    }
    public <T> Arguments forEach(Iterable<T> iterable, BiConsumer<Arguments, T> consumer) {
      iterable.forEach(argument -> consumer.accept(this, argument));
      return this;
    }
  }
  public static final class JavaArchiveTool extends Tool {
    public enum Operation {
      CREATE,
      GENERATE_INDEX,
      LIST,
      UPDATE,
      EXTRACT,
      DESCRIBE_MODULE
    }
    JavaArchiveTool(List<? extends Option> options) {
      super("jar", options);
    }
    public static final class PerformOperation implements Option {
      private final Operation mode;
      private final List<String> more;
      public PerformOperation(Operation mode, String... more) {
        this.mode = mode;
        this.more = List.of(more);
      }
      public Operation mode() {
        return mode;
      }
      public List<String> more() {
        return more;
      }
      public void visit(Arguments arguments) {
        var key = "--" + mode.toString().toLowerCase().replace('_', '-');
        var value = more.isEmpty() ? "" : "=" + more.get(0);
        arguments.add(key + value);
      }
    }
    public static final class ArchiveFile extends KeyValueOption<Path> {
      public ArchiveFile(Path file) {
        super("--file", file);
      }
    }
    public static final class ChangeDirectory extends KeyValueOption<Path> {
      public ChangeDirectory(Path value) {
        super("-C", value);
      }
      public void visit(Arguments arguments) {
        arguments.add("-C", value(), ".");
      }
    }
    public static final class MultiReleaseVersion implements Option {
      private final int version;
      public MultiReleaseVersion(int version) {
        this.version = version;
      }
      public int version() {
        return version;
      }
      public void visit(Arguments arguments) {
        arguments.add("--release", version);
      }
    }
  }
  public static final class JavaCompiler extends Tool {
    JavaCompiler(List<? extends Option> options) {
      super("javac", options);
    }
    public int release() {
      return find(JavaCompiler.CompileForJavaRelease.class).map(KeyValueOption::value).orElse(0);
    }
    public boolean preview() {
      return find(JavaCompiler.EnablePreviewLanguageFeatures.class).isPresent();
    }
    public static final class DestinationDirectory extends KeyValueOption<Path> {
      public DestinationDirectory(Path directory) {
        super("-d", directory);
      }
    }
    public static final class CompileForJavaRelease extends KeyValueOption<Integer> {
      public CompileForJavaRelease(Integer release) {
        super("--release", release);
      }
    }
    public static final class EnablePreviewLanguageFeatures implements Option {
      public void visit(Arguments arguments) {
        arguments.add("--enable-preview");
      }
    }
    public static final class CompileModulesCheckingTimestamps implements Option {
      private final List<String> modules;
      public CompileModulesCheckingTimestamps(List<String> modules) {
        this.modules = modules;
      }
      public List<String> modules() {
        return modules;
      }
      public void visit(Arguments arguments) {
        arguments.add("--module", String.join(",", modules));
      }
    }
    public static final class ModulePath implements Option {
      private final List<Path> paths;
      public ModulePath(List<Path> paths) {
        this.paths = paths;
      }
      public void visit(Arguments arguments) {
        arguments.add("--module-path", Strings.toString(paths));
      }
    }
    public static final class ModuleSourcePathInModuleSpecificForm implements Option {
      private final String module;
      private final List<Path> paths;
      public ModuleSourcePathInModuleSpecificForm(String module, List<Path> paths) {
        this.module = module;
        this.paths = paths;
      }
      public void visit(Arguments arguments) {
        arguments.add("--module-source-path", module + "=" + Strings.toString(paths));
      }
    }
    public static final class ModuleSourcePathInModulePatternForm implements Option {
      private final List<String> patterns;
      public ModuleSourcePathInModulePatternForm(List<String> patterns) {
        this.patterns = patterns;
      }
      public void visit(Arguments arguments) {
        arguments.add("--module-source-path", String.join(File.pathSeparator, patterns));
      }
    }
  }
  public static final class JavaDocumentationGenerator extends Tool {
    JavaDocumentationGenerator(List<? extends Option> options) {
      super("javadoc", options);
    }
    public static final class DestinationDirectory extends KeyValueOption<Path> {
      public DestinationDirectory(Path directory) {
        super("-d", directory);
      }
    }
    public static final class DocumentListOfModules implements Option {
      private final List<String> modules;
      public DocumentListOfModules(List<String> modules) {
        this.modules = modules;
      }
      public List<String> modules() {
        return modules;
      }
      public void visit(Arguments arguments) {
        arguments.add("--module", String.join(",", modules));
      }
    }
  }
  public static class KeyValueOption<V> implements Option {
    private final String key;
    private final V value;
    public KeyValueOption(String key, V value) {
      this.key = key;
      this.value = value;
    }
    public V value() {
      return value;
    }
    public void visit(Arguments arguments) {
      arguments.add(key);
      if (value == null) return;
      arguments.add(value);
    }
  }
  public static class ObjectArrayOption<V> implements Option {
    private final V[] values;
    @SafeVarargs
    public ObjectArrayOption(V... values) {
      this.values = values;
    }
    public V[] value() {
      return values;
    }
    public void visit(Arguments arguments) {
      arguments.addAll(values);
    }
  }
  public interface Option {
    void visit(Arguments arguments);
  }
  public static class Tool {
    public static Tool of(String name, String... arguments) {
      return new Tool(name, List.of(new ObjectArrayOption<>(arguments)));
    }
    public static JavaArchiveTool jar(List<? extends Option> options) {
      return new JavaArchiveTool(options);
    }
    public static JavaCompiler javac(List<? extends Option> options) {
      return new JavaCompiler(options);
    }
    public static JavaDocumentationGenerator javadoc(List<? extends Option> options) {
      return new JavaDocumentationGenerator(options);
    }
    private final String name;
    private final List<? extends Option> options;
    public Tool(String name, List<? extends Option> options) {
      this.name = name;
      this.options = options;
      var type = getClass();
      if (type == Tool.class) return;
      var optionsDeclaredInDifferentClass =
          options.stream()
              .filter(option -> !type.equals(option.getClass().getDeclaringClass()))
              .collect(Collectors.toList());
      if (optionsDeclaredInDifferentClass.isEmpty()) return;
      var caption = String.format("All options of tool %s must be declared in %s", name, type);
      var message = new StringJoiner(System.lineSeparator() + "\tbut ").add(caption);
      for (var option : optionsDeclaredInDifferentClass) {
        var optionClass = option.getClass();
        message.add(optionClass + " is declared in " + optionClass.getDeclaringClass());
      }
      throw new IllegalArgumentException(message.toString());
    }
    public String name() {
      return name;
    }
    public <O extends Option> Stream<O> filter(Class<O> type) {
      return options.stream().filter(option -> option.getClass().equals(type)).map(type::cast);
    }
    public <O extends Option> Optional<O> find(Class<O> type) {
      return filter(type).findAny();
    }
    public <O extends Option> O get(Class<O> type) {
      return find(type).orElseThrow();
    }
    protected void addInitialArguments(Arguments arguments) {}
    protected void addMoreArguments(Arguments arguments) {}
    public List<String> toArgumentStrings() {
      var arguments = new Arguments();
      addInitialArguments(arguments);
      options.forEach(option -> option.visit(arguments));
      addMoreArguments(arguments);
      return arguments.build();
    }
  }
  public static class Functions {
    public static <T> Supplier<T> memoize(Supplier<T> supplier) {
      Objects.requireNonNull(supplier, "supplier");
      class CachingSupplier implements Supplier<T> {
        Supplier<T> delegate = this::initialize;
        boolean initialized = false;
        public T get() {
          return delegate.get();
        }
        private synchronized T initialize() {
          if (initialized) return delegate.get();
          T value = supplier.get();
          delegate = () -> value;
          initialized = true;
          return value;
        }
      }
      return new CachingSupplier();
    }
    private Functions() {}
  }
  public static class Modules {
    interface Patterns {
      Pattern NAME =
          Pattern.compile(
              "(?:module)" // key word
                  + "\\s+([\\w.]+)" // module name
                  + "(?:\\s*/\\*.*\\*/\\s*)?" // optional multi-line comment
                  + "\\s*\\{"); // end marker
      Pattern REQUIRES =
          Pattern.compile(
              "(?:requires)" // key word
                  + "(?:\\s+[\\w.]+)?" // optional modifiers
                  + "\\s+([\\w.]+)" // module name
                  + "(?:\\s*/\\*\\s*([\\w.\\-+]+)\\s*\\*/\\s*)?" // optional '/*' version '*/'
                  + "\\s*;"); // end marker
    }
    public static Optional<String> findMainClass(Path info, String module) {
      var main = Path.of(module.replace('.', '/'), "Main.java");
      var exists = Files.isRegularFile(info.resolveSibling(main));
      return exists ? Optional.of(module + '.' + "Main") : Optional.empty();
    }
    public static Optional<String> findMainModule(Stream<ModuleDescriptor> descriptors) {
      var mains = descriptors.filter(d -> d.mainClass().isPresent()).collect(Collectors.toList());
      return mains.size() == 1 ? Optional.of(mains.get(0).name()) : Optional.empty();
    }
    public static ModuleDescriptor describe(Path info) {
      try {
        var module = describe(Files.readString(info));
        var temporary = module.build();
        findMainClass(info, temporary.name()).ifPresent(module::mainClass);
        return module.build();
      } catch (IOException e) {
        throw new UncheckedIOException("Describe failed", e);
      }
    }
    public static ModuleDescriptor.Builder describe(String source) {
      var nameMatcher = Patterns.NAME.matcher(source);
      if (!nameMatcher.find())
        throw new IllegalArgumentException("Expected Java module source unit, but got: " + source);
      var name = nameMatcher.group(1).trim();
      var builder = ModuleDescriptor.newModule(name);
      var requiresMatcher = Patterns.REQUIRES.matcher(source);
      while (requiresMatcher.find()) {
        var requiredName = requiresMatcher.group(1);
        Optional.ofNullable(requiresMatcher.group(2))
            .ifPresentOrElse(
                version -> builder.requires(Set.of(), requiredName, Version.parse(version)),
                () -> builder.requires(requiredName));
      }
      return builder;
    }
    public static Set<String> declared(ModuleFinder finder) {
      return declared(finder.findAll().stream().map(ModuleReference::descriptor));
    }
    public static Set<String> declared(Stream<ModuleDescriptor> descriptors) {
      return descriptors.map(ModuleDescriptor::name).collect(Collectors.toCollection(TreeSet::new));
    }
    public static Set<String> required(ModuleFinder finder) {
      return required(finder.findAll().stream().map(ModuleReference::descriptor));
    }
    public static Set<String> required(Stream<ModuleDescriptor> descriptors) {
      return descriptors
          .map(ModuleDescriptor::requires)
          .flatMap(Set::stream)
          .filter(requires -> !requires.modifiers().contains(Requires.Modifier.MANDATED))
          .filter(requires -> !requires.modifiers().contains(Requires.Modifier.SYNTHETIC))
          .map(Requires::name)
          .collect(Collectors.toCollection(TreeSet::new));
    }
    private Modules() {}
  }
  public static class ModulesResolver {
    private final Path[] paths;
    private final Set<String> declared;
    private final Consumer<Set<String>> transporter;
    private final Set<String> system;
    public ModulesResolver(Path[] paths, Set<String> declared, Consumer<Set<String>> transporter) {
      this.paths = paths;
      this.declared = new TreeSet<>(declared);
      this.transporter = transporter;
      this.system = Modules.declared(ModuleFinder.ofSystem());
    }
    public void resolve(Set<String> required) {
      resolveModules(required);
      resolveLibraryModules();
    }
    public void resolveModules(Set<String> required) {
      var missing = missing(required);
      if (missing.isEmpty()) return;
      transporter.accept(missing);
      var unresolved = missing(required);
      if (unresolved.isEmpty()) return;
      throw new IllegalStateException("Unresolved modules: " + unresolved);
    }
    public void resolveLibraryModules() {
      do {
        var missing = missing(Modules.required(ModuleFinder.of(paths)));
        if (missing.isEmpty()) return;
        resolveModules(missing);
      } while (true);
    }
    Set<String> missing(Set<String> required) {
      var missing = new TreeSet<>(required);
      missing.removeAll(declared);
      if (required.isEmpty()) return Set.of();
      missing.removeAll(system);
      if (required.isEmpty()) return Set.of();
      var library = Modules.declared(ModuleFinder.of(paths));
      missing.removeAll(library);
      return missing;
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
    public static Path assertFileAttributes(Path file, Map<String, String> attributes) {
      if (attributes.isEmpty()) return file;
      var map = new HashMap<>(attributes);
      var size = map.remove("size");
      if (size != null) {
        var expectedSize = Long.parseLong(size);
        try {
          long fileSize = Files.size(file);
          if (expectedSize != fileSize) {
            var details = "expected " + expectedSize + " bytes\n\tactual " + fileSize + " bytes";
            throw new AssertionError("File size mismatch: " + file + "\n\t" + details);
          }
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      }
      map.remove("module");
      map.remove("version");
      if (map.isEmpty()) return file;
      try {
        var bytes = Files.readAllBytes(file);
        for (var expectedDigest : map.entrySet()) {
          var md = MessageDigest.getInstance(expectedDigest.getKey().toUpperCase(Locale.US));
          md.update(bytes);
          var actualDigestValue = Strings.hex(md.digest());
          var expectedDigestValue = expectedDigest.getValue().toLowerCase(Locale.US);
          if (expectedDigestValue.equals(actualDigestValue)) continue;
          var details = "expected " + expectedDigestValue + ", but got " + actualDigestValue;
          throw new AssertionError("File digest mismatch: " + file + "\n\t" + details);
        }
      } catch (Exception e) {
        throw new AssertionError("File digest check failed: " + file, e);
      }
      return file;
    }
    private Paths() {}
  }
  public static class Resources {
    private final HttpClient client;
    public Resources(HttpClient client) {
      this.client = client;
    }
    public HttpResponse<Void> head(URI uri, int timeout) throws Exception {
      var nobody = HttpRequest.BodyPublishers.noBody();
      var duration = Duration.ofSeconds(timeout);
      var request = HttpRequest.newBuilder(uri).method("HEAD", nobody).timeout(duration).build();
      return client.send(request, BodyHandlers.discarding());
    }
    public Path copy(URI uri, Path file) throws Exception {
      return copy(uri, file, StandardCopyOption.COPY_ATTRIBUTES);
    }
    public Path copy(URI uri, Path file, CopyOption... options) throws Exception {
      var request = HttpRequest.newBuilder(uri).GET();
      if (Files.exists(file) && Files.getFileStore(file).supportsFileAttributeView("user")) {
        var etagBytes = (byte[]) Files.getAttribute(file, "user:etag");
        var etag = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(etagBytes)).toString();
        request.setHeader("If-None-Match", etag);
      }
      var directory = file.getParent();
      if (directory != null) Files.createDirectories(directory);
      var handler = BodyHandlers.ofFile(file);
      var response = client.send(request.build(), handler);
      if (response.statusCode() == 200) {
        if (Set.of(options).contains(StandardCopyOption.COPY_ATTRIBUTES)) {
          var etagHeader = response.headers().firstValue("etag");
          if (etagHeader.isPresent() && Files.getFileStore(file).supportsFileAttributeView("user")) {
            var etag = StandardCharsets.UTF_8.encode(etagHeader.get());
            Files.setAttribute(file, "user:etag", etag);
          }
          var lastModifiedHeader = response.headers().firstValue("last-modified");
          if (lastModifiedHeader.isPresent()) {
            @SuppressWarnings("SpellCheckingInspection")
            var format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
            var current = System.currentTimeMillis();
            var millis = format.parse(lastModifiedHeader.get()).getTime(); // 0 means "unknown"
            var fileTime = FileTime.fromMillis(millis == 0 ? current : millis);
            Files.setLastModifiedTime(file, fileTime);
          }
        }
        return file;
      }
      if (response.statusCode() == 304 /*Not Modified*/) return file;
      Files.deleteIfExists(file);
      throw new IllegalStateException("copy " + uri + " failed: response=" + response);
    }
    public String read(URI uri) throws Exception {
      var request = HttpRequest.newBuilder(uri).GET();
      return client.send(request.build(), BodyHandlers.ofString()).body();
    }
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
      return text(strings.map(string -> indent + string));
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
    private static final char[] HEX_TABLE = "0123456789abcdef".toCharArray();
    public static String hex(byte[] bytes) {
      var chars = new char[bytes.length * 2];
      for (int i = 0; i < bytes.length; i++) {
        int value = bytes[i] & 0xFF;
        chars[i * 2] = HEX_TABLE[value >>> 4];
        chars[i * 2 + 1] = HEX_TABLE[value & 0x0F];
      }
      return new String(chars);
    }
    private Strings() {}
  }
  public static final class Workspace {
    public static Workspace of() {
      return of(Path.of(""));
    }
    public static Workspace of(Path base) {
      return new Workspace(base, base.resolve("lib"), base.resolve(".bach/workspace"));
    }
    private final Path base;
    private final Path lib;
    private final Path workspace;
    public Workspace(Path base, Path lib, Path workspace) {
      this.base = base;
      this.lib = lib;
      this.workspace = workspace;
    }
    public Path base() {
      return base;
    }
    public Path lib() {
      return lib;
    }
    public Path workspace() {
      return workspace;
    }
    public String toString() {
      return new StringJoiner(", ", Workspace.class.getSimpleName() + "[", "]")
          .add("base=" + base)
          .add("lib=" + lib)
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
