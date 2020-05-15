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
import java.lang.System.Logger.Level;
import java.lang.System.Logger;
import java.lang.module.FindException;
import java.lang.module.ModuleDescriptor.Requires;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.reflect.Array;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;
public class Bach {
  public static final Version VERSION = Version.parse("11.0-ea");
  public static final Path BUILD_JAVA = Path.of(".bach/src/Build.java");
  public static final Path WORKSPACE = Path.of(".bach/workspace");
  public static void main(String... args) {
    Main.main(args);
  }
  public static Optional<Path> findCustomBuildProgram() {
    return Files.exists(BUILD_JAVA) ? Optional.of(BUILD_JAVA) : Optional.empty();
  }
  public static Bach of() {
    return of(UnaryOperator.identity());
  }
  public static Bach of(UnaryOperator<Project.Builder> operator) {
    return of(Project.of(Path.of(""), operator));
  }
  public static Bach of(Project project) {
    return new Bach(project, HttpClient.newBuilder()::build);
  }
  private final Logbook logbook;
  private final Project project;
  private final Supplier<HttpClient> httpClient;
  public Bach(Project project, Supplier<HttpClient> httpClient) {
    this(Logbook.ofSystem(), project, httpClient);
  }
  private Bach(Logbook logbook, Project project, Supplier<HttpClient> httpClient) {
    this.logbook = Objects.requireNonNull(logbook, "logbook");
    this.project = Objects.requireNonNull(project, "project");
    this.httpClient = Functions.memoize(httpClient);
    logbook.log(Level.TRACE, "Initialized " + toString());
    logbook.log(Level.DEBUG, project.toTitleAndVersion());
  }
  public Logger getLogger() {
    return logbook;
  }
  public Project getProject() {
    return project;
  }
  public HttpClient getHttpClient() {
    return httpClient.get();
  }
  public Summary build() {
    var summary = new Summary(this);
    try {
      execute(buildSequence());
    } finally {
      summary.writeMarkdown(project.base().workspace("summary.md"), true);
    }
    return summary;
  }
  private Task buildSequence() {
    var tasks = new ArrayList<Task>();
    tasks.add(new Task.CreateDirectories(project.base().lib()));
    tasks.add(new Task.ResolveMissingThirdPartyModules());
    for (var realm : project.structure().realms()) {
      tasks.add(realm.javac().toTask());
      for (var unit : realm.units()) tasks.addAll(unit.tasks());
      tasks.addAll(realm.tasks());
    }
    tasks.add(new Task.DeleteEmptyDirectory(project.base().lib()));
    return Task.sequence("Build Sequence", tasks);
  }
  private void execute(Task task) {
    var label = task.getLabel();
    var tasks = task.getList();
    if (tasks.isEmpty()) {
      logbook.log(Level.TRACE, "* {0}", label);
      try {
        if (logbook.isDryRun()) return;
        task.execute(this);
      } catch (Throwable throwable) {
        var message = "Task execution failed";
        logbook.log(Level.ERROR, message, throwable);
        throw new Error(message, throwable);
      } finally {
        logbook.log(Level.DEBUG, task.getOut().toString().strip());
        logbook.log(Level.WARNING, task.getErr().toString().strip());
      }
      return;
    }
    logbook.log(Level.TRACE, "+ {0}", label);
    var start = System.currentTimeMillis();
    for (var sub : tasks) execute(sub);
    var duration = System.currentTimeMillis() - start;
    logbook.log(Level.TRACE, "= {0} took {1} ms", label, duration);
  }
  private void execute(ToolProvider tool, PrintWriter out, PrintWriter err, String... args) {
    var call = (tool.name() + ' ' + String.join(" ", args)).trim();
    logbook.log(Level.DEBUG, call);
    var code = tool.run(out, err, args);
    if (code != 0) throw new AssertionError("Tool run exit code: " + code + "\n\t" + call);
  }
  public String toString() {
    return "Bach.java " + VERSION;
  }
  public static class Call {
    private final String name;
    private final Arguments additionalArguments = new Arguments();
    public Call(String name) {
      this.name = name;
    }
    public Arguments getAdditionalArguments() {
      return additionalArguments;
    }
    public String toLabel() {
      return name;
    }
    public ToolProvider toProvider() {
      return ToolProvider.findFirst(name).orElseThrow();
    }
    public String[] toArguments() {
      var arguments = new Arguments();
      addConfiguredArguments(arguments);
      return arguments.add(getAdditionalArguments()).toStringArray();
    }
    protected void addConfiguredArguments(Arguments arguments) {}
    public Task toTask() {
      return new Task.RunTool(toLabel(), toProvider(), toArguments());
    }
    public static class Arguments {
      private final List<String> list = new ArrayList<>();
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
      public Arguments add(Arguments arguments) {
        list.addAll(arguments.list);
        return this;
      }
      public String[] toStringArray() {
        return list.toArray(String[]::new);
      }
    }
    public static boolean assigned(Object object) {
      if (object == null) return false;
      if (object instanceof Number) return ((Number) object).intValue() != 0;
      if (object instanceof String) return !((String) object).isEmpty();
      if (object instanceof Optional) return ((Optional<?>) object).isPresent();
      if (object instanceof Collection) return !((Collection<?>) object).isEmpty();
      if (object.getClass().isArray()) return Array.getLength(object) != 0;
      return true;
    }
    public static String join(Collection<Path> paths) {
      return paths.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator));
    }
    public static String joinPaths(Collection<String> paths) {
      return String.join(File.pathSeparator, paths);
    }
  }
  static class Main {
    public static void main(String... args) {
      if (Bach.findCustomBuildProgram().isPresent()) {
        System.err.println("Custom build program execution not supported, yet.");
        return;
      }
      Bach.of().build().assertSuccessful();
    }
  }
  public static final class Project {
    public static Project of(Path directory, UnaryOperator<Builder> operator) {
      var base = Base.of(directory);
      var directoryName = base.directory().toAbsolutePath().getFileName();
      var title = Optional.ofNullable(directoryName).map(Path::toString).orElse("Untitled");
      var builder = new Builder().base(base).title(title);
      return ModulesWalker.walk(operator.apply(builder)).build();
    }
    private final Base base;
    private final Info info;
    private final Structure structure;
    public Project(Base base, Info info, Structure structure) {
      this.base = Objects.requireNonNull(base, "base");
      this.info = Objects.requireNonNull(info, "info");
      this.structure = Objects.requireNonNull(structure, "structure");
    }
    public Base base() {
      return base;
    }
    public Info info() {
      return info;
    }
    public Structure structure() {
      return structure;
    }
    public String toString() {
      return new StringJoiner(", ", Project.class.getSimpleName() + "[", "]")
          .add("base=" + base)
          .add("info=" + info)
          .add("structure=" + structure)
          .toString();
    }
    public List<String> toStrings() {
      var list = new ArrayList<String>();
      list.add("Project");
      list.add("\ttitle: " + info.title());
      list.add("\tversion: " + info.version());
      list.add("\trealms: " + structure.realms().size());
      list.add("\tunits: " + structure.units().count());
      for (var realm : structure.realms()) {
        list.add("\tRealm " + realm.name());
        list.add("\t\tjavac: " + String.format("%.77s...", realm.javac().toLabel()));
        list.add("\t\ttasks: " + realm.tasks().size());
        for (var unit : realm.units()) {
          list.add("\t\tUnit " + unit.name());
          list.add("\t\t\ttasks: " + unit.tasks().size());
          var module = unit.descriptor();
          list.add("\t\t\tModule Descriptor " + module.toNameAndVersion());
          list.add("\t\t\t\tmain: " + module.mainClass().orElse("-"));
          list.add("\t\t\t\trequires: " + new TreeSet<>(module.requires()));
        }
      }
      return list;
    }
    public String toTitleAndVersion() {
      return info.title() + ' ' + info.version();
    }
    public Set<String> toDeclaredModuleNames() {
      return structure.units().map(Unit::name).collect(Collectors.toCollection(TreeSet::new));
    }
    public Set<String> toRequiredModuleNames() {
      return Modules.required(structure.units().map(Unit::descriptor));
    }
    public static final class Base {
      public static Base of() {
        return of(Path.of(""));
      }
      public static Base of(Path directory) {
        return new Base(directory, directory.resolve(Bach.WORKSPACE));
      }
      private final Path directory;
      private final Path workspace;
      Base(Path directory, Path workspace) {
        this.directory = Objects.requireNonNull(directory, "directory");
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
      public Path lib() {
        return path("lib");
      }
      public Path workspace(String first, String... more) {
        return workspace.resolve(Path.of(first, more));
      }
      public Path api() {
        return workspace("api");
      }
      public Path classes(String realm) {
        return workspace("classes", realm);
      }
      public Path classes(String realm, String module) {
        return workspace("classes", realm, module);
      }
      public Path image() {
        return workspace("image");
      }
      public Path modules(String realm) {
        return workspace("modules", realm);
      }
      public List<Path> modulePaths(Iterable<String> realms) {
        var paths = new ArrayList<Path>();
        for (var realm : realms) paths.add(modules(realm));
        paths.add(lib());
        return List.copyOf(paths);
      }
    }
    public static final class Info {
      private final String title;
      private final Version version;
      public Info(String title, Version version) {
        this.title = Objects.requireNonNull(title, "title");
        this.version = Objects.requireNonNull(version, "version");
      }
      public String title() {
        return title;
      }
      public Version version() {
        return version;
      }
      public String toString() {
        return new StringJoiner(", ", Info.class.getSimpleName() + "[", "]")
            .add("title='" + title + "'")
            .add("version=" + version)
            .toString();
      }
    }
    public static final class Structure {
      private final Library library;
      private final List<Realm> realms;
      public Structure(Library library, List<Realm> realms) {
        this.library = library;
        this.realms = List.copyOf(Objects.requireNonNull(realms, "realms"));
      }
      public Library library() {
        return library;
      }
      public List<Realm> realms() {
        return realms;
      }
      public Stream<Unit> units() {
        return realms.stream().flatMap(realm -> realm.units().stream());
      }
    }
    public static final class Library {
      public static Library of() {
        return of(new ModulesMap());
      }
      public static Library of(Map<String, String> map) {
        return new Library(Set.of(), map::get);
      }
      private final Set<String> required;
      private final UnaryOperator<String> lookup;
      public Library(Set<String> required, UnaryOperator<String> lookup) {
        this.required = required;
        this.lookup = lookup;
      }
      public Set<String> required() {
        return required;
      }
      public UnaryOperator<String> lookup() {
        return lookup;
      }
    }
    public static final class Realm {
      private final String name;
      private final List<Unit> units;
      private final Javac javac;
      private final List<Task> tasks;
      public Realm(String name, List<Unit> units, Javac javac, List<Task> tasks) {
        this.name = Objects.requireNonNull(name, "name");
        this.units = List.copyOf(Objects.requireNonNull(units, "units"));
        this.javac = Objects.requireNonNull(javac, "javac");
        this.tasks = List.copyOf(Objects.requireNonNull(tasks, "tasks"));
      }
      public String name() {
        return name;
      }
      public List<Unit> units() {
        return units;
      }
      public Javac javac() {
        return javac;
      }
      public List<Task> tasks() {
        return tasks;
      }
    }
    public static final class Unit {
      private final ModuleDescriptor descriptor;
      private final List<Path> paths;
      private final List<Task> tasks;
      public Unit(ModuleDescriptor descriptor, List<Path> paths, List<Task> tasks) {
        this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
        this.paths = List.copyOf(Objects.requireNonNull(paths, "paths"));
        this.tasks = List.copyOf(Objects.requireNonNull(tasks, "tasks"));
      }
      public ModuleDescriptor descriptor() {
        return descriptor;
      }
      public List<Path> paths() {
        return paths;
      }
      public List<Task> tasks() {
        return tasks;
      }
      public String name() {
        return descriptor.name();
      }
    }
    public interface Tuner {
      void tune(Call call, Map<String, String> context);
      static void defaults(Call call, @SuppressWarnings("unused") Map<String, String> context) {
        if (call instanceof GenericSourcesConsumer) {
          var consumer = (GenericSourcesConsumer<?>) call;
          consumer.setCharacterEncodingUsedBySourceFiles("UTF-8");
        }
        if (call instanceof Javac) {
          var javac = (Javac) call;
          javac.setGenerateMetadataForMethodParameters(true);
          javac.setTerminateCompilationIfWarningsOccur(true);
          javac.getAdditionalArguments().add("-X" + "lint");
        }
        if (call instanceof Javadoc) {
          var javadoc = (Javadoc) call;
          javadoc.getAdditionalArguments().add("-locale", "en");
        }
        if (call instanceof Jlink) {
          var jlink = (Jlink) call;
          jlink.getAdditionalArguments().add("--compress", "2");
          jlink.getAdditionalArguments().add("--no-header-files");
          jlink.getAdditionalArguments().add("--no-man-pages");
          jlink.getAdditionalArguments().add("--strip-debug");
        }
      }
    }
    public static class Builder {
      private Base base = Base.of();
      private Info info = new Info("Project Title", Version.parse("1-ea"));
      private Library library = Library.of();
      private Structure structure = new Structure(Library.of(), List.of());
      private Tuner tuner = Tuner::defaults;
      private List<Path> walkModuleInfoFiles = List.of();
      public Project build() {
        return new Project(base, info, structure);
      }
      public Base getBase() {
        return base;
      }
      public Info getInfo() {
        return info;
      }
      public Library getLibrary() {
        return library;
      }
      public Tuner getTuner() {
        return tuner;
      }
      public List<Path> getWalkModuleInfoFiles() {
        return walkModuleInfoFiles;
      }
      public Builder base(Base base) {
        this.base = base;
        return this;
      }
      public Builder base(String directory) {
        return base(Base.of(Path.of(directory)));
      }
      public Builder title(String title) {
        return info(new Info(title, info.version()));
      }
      public Builder version(String version) {
        return version(Version.parse(version));
      }
      public Builder version(Version version) {
        return info(new Info(info.title(), version));
      }
      public Builder info(Info info) {
        this.info = info;
        return this;
      }
      public Builder library(Library library) {
        this.library = library;
        return this;
      }
      public Builder structure(Structure structure) {
        this.structure = structure;
        return this;
      }
      public Builder tuner(Tuner tuner) {
        this.tuner = tuner;
        return this;
      }
      public Builder walkModuleInfoFiles(List<Path> files) {
        this.walkModuleInfoFiles = files;
        return this;
      }
    }
  }
  public static class Summary {
    private final Bach bach;
    private final Logbook logbook;
    public Summary(Bach bach) {
      this.bach = bach;
      this.logbook = (Logbook) bach.getLogger();
    }
    public void assertSuccessful() {
      var entries = logbook.entries(Level.WARNING).collect(Collectors.toList());
      if (entries.isEmpty()) return;
      var lines = new StringJoiner(System.lineSeparator());
      lines.add(String.format("Collected %d error(s)", entries.size()));
      for (var entry : entries) lines.add("\t- " + entry.message());
      lines.add("");
      lines.add(String.join(System.lineSeparator(), toMarkdown()));
      var error = new AssertionError(lines.toString());
      for (var entry : entries) if (entry.exception() != null) error.addSuppressed(entry.exception());
      throw error;
    }
    public void writeMarkdown(Path file, boolean createCopyWithTimestamp) {
      var markdown = toMarkdown();
      try {
        Files.createDirectories(file.getParent());
        Files.write(file, markdown);
        if (createCopyWithTimestamp) {
          @SuppressWarnings("SpellCheckingInspection")
          var pattern = "yyyyMMdd_HHmmss";
          var formatter = DateTimeFormatter.ofPattern(pattern).withZone(ZoneOffset.UTC);
          var timestamp = formatter.format(Instant.now().truncatedTo(ChronoUnit.SECONDS));
          var summaries = Files.createDirectories(file.resolveSibling("summaries"));
          Files.copy(file, summaries.resolve("summary-" + timestamp + ".md"));
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    public List<String> toMarkdown() {
      var md = new ArrayList<String>();
      md.add("# Summary for " + bach.getProject().toTitleAndVersion());
      md.addAll(projectDescription());
      md.addAll(logbookEntries());
      return md;
    }
    private List<String> projectDescription() {
      var md = new ArrayList<String>();
      var project = bach.getProject();
      md.add("");
      md.add("## Project");
      md.add("- title: " + project.info().title());
      md.add("- version: " + project.info().version());
      md.add("");
      md.add("```text");
      md.addAll(project.toStrings());
      md.add("```");
      return md;
    }
    private List<String> logbookEntries() {
      var md = new ArrayList<String>();
      md.add("");
      md.add("## Logbook");
      for (var entry : logbook.entries(Level.ALL).collect(Collectors.toList())) {
        md.add("- " + entry.level());
        var one = entry.message().lines().count() == 1;
        md.add((one ? "`" : "```text\n") + entry.message() + (one ? "`" : "\n```"));
      }
      return md;
    }
  }
  public static class Task {
    public static Task sequence(String label, Task... tasks) {
      return sequence(label, List.of(tasks));
    }
    public static Task sequence(String label, List<Task> tasks) {
      return new Task(label, tasks);
    }
    private final String label;
    private final List<Task> list;
    private final StringWriter out;
    private final StringWriter err;
    public Task() {
      this("", List.of());
    }
    public Task(String label, List<Task> list) {
      Objects.requireNonNull(label, "label");
      this.label = label.isBlank() ? getClass().getSimpleName() : label;
      this.list = List.copyOf(Objects.requireNonNull(list, "list"));
      this.out = new StringWriter();
      this.err = new StringWriter();
    }
    public String getLabel() {
      return label;
    }
    public List<Task> getList() {
      return list;
    }
    public StringWriter getOut() {
      return out;
    }
    public StringWriter getErr() {
      return err;
    }
    public String toString() {
      return new StringJoiner(", ", Task.class.getSimpleName() + "[", "]")
          .add("label='" + label + "'")
          .add("list.size=" + list.size())
          .toString();
    }
    public void execute(Bach bach) throws Exception {}
    public static class RunTool extends Task {
      private final ToolProvider tool;
      private final String[] args;
      public RunTool(String label, ToolProvider tool, String... args) {
        super(label, List.of());
        this.tool = tool;
        this.args = args;
      }
      public void execute(Bach bach) {
        bach.execute(tool, new PrintWriter(getOut()), new PrintWriter(getErr()), args);
      }
    }
    public static class RunTestModule extends Task {
      private final String module;
      private final List<Path> modulePaths;
      public RunTestModule(String module, List<Path> modulePaths) {
        super("Run tests for module " + module, List.of());
        this.module = module;
        this.modulePaths = modulePaths;
      }
      public void execute(Bach bach) {
        var currentThread = Thread.currentThread();
        var currentContextLoader = currentThread.getContextClassLoader();
        try {
          for (var tool : Modules.findTools(module, modulePaths)) executeTool(bach, tool);
        } finally {
          currentThread.setContextClassLoader(currentContextLoader);
        }
      }
      private void executeTool(Bach bach, ToolProvider tool) {
        Thread.currentThread().setContextClassLoader(tool.getClass().getClassLoader());
        if (tool.name().equals("test(" + module + ")")) {
          bach.execute(tool, new PrintWriter(getOut()), new PrintWriter(getErr()));
          return;
        }
        if (tool.name().equals("junit")) {
          var base = bach.getProject().base();
          bach.execute(
              tool,
              new PrintWriter(getOut()),
              new PrintWriter(getErr()),
              "--select-module",
              module,
              "--disable-ansi-colors",
              "--reports-dir",
              base.workspace("junit-reports", module).toString());
        }
      }
    }
    public static class CreateDirectories extends Task {
      private final Path directory;
      public CreateDirectories(Path directory) {
        super("Create directories " + directory.toUri(), List.of());
        this.directory = directory;
      }
      public void execute(Bach bach) throws Exception {
        Files.createDirectories(directory);
      }
    }
    public static class DeleteDirectories extends Task {
      private final Path directory;
      public DeleteDirectories(Path directory) {
        super("Delete directory " + directory, List.of());
        this.directory = directory;
      }
      public void execute(Bach bach) throws Exception {
        if (Files.notExists(directory)) return;
        try (var stream = Files.walk(directory)) {
          var paths = stream.sorted((p, q) -> -p.compareTo(q));
          for (var path : paths.toArray(Path[]::new)) Files.deleteIfExists(path);
        }
      }
    }
    public static class DeleteEmptyDirectory extends Task {
      private final Path directory;
      public DeleteEmptyDirectory(Path directory) {
        super("Delete directory " + directory + " if it is empty", List.of());
        this.directory = directory;
      }
      public void execute(Bach bach) throws Exception {
        if (Files.notExists(directory)) return;
        if (Paths.isEmpty(directory)) Files.deleteIfExists(directory);
      }
    }
    public static class ResolveMissingThirdPartyModules extends Task {
      public ResolveMissingThirdPartyModules() {
        super("Resolve missing 3rd-party modules", List.of());
      }
      public void execute(Bach bach) {
        var project = bach.getProject();
        var library = project.structure().library();
        class Transporter implements Consumer<Set<String>> {
          public void accept(Set<String> modules) {
            var resources = new Resources(bach.getHttpClient());
            for (var module : modules) {
              var raw = library.lookup().apply(module);
              if (raw == null) continue;
              try {
                var lib = Files.createDirectories(project.base().lib());
                var uri = URI.create(raw);
                var name = module + ".jar";
                var file = resources.copy(uri, lib.resolve(name));
                var size = Files.size(file);
                bach.getLogger().log(Level.INFO, "{0} ({1} bytes) << {2}", file, size, uri);
              } catch (Exception e) {
                throw new Error("Resolve module '" + module + "' failed: " + raw + "\n\t" + e, e);
              }
            }
          }
        }
        var modulePaths = project.base().modulePaths(List.of());
        var declared = project.toDeclaredModuleNames();
        var resolver = new ModulesResolver(modulePaths, declared, new Transporter());
        resolver.resolve(project.toRequiredModuleNames());
        resolver.resolve(library.required());
      }
    }
  }
  @SuppressWarnings("unchecked")
  public static abstract class GenericSourcesConsumer<T> extends Call {
    private Path destinationDirectory;
    private String characterEncodingUsedBySourceFiles;
    private Set<String> modules;
    public GenericSourcesConsumer(String name) {
      super(name);
    }
    protected void addConfiguredArguments(Arguments arguments) {
      var destination = getDestinationDirectory();
      if (assigned(destination)) arguments.add("-d", destination);
      var encoding = getCharacterEncodingUsedBySourceFiles();
      if (assigned(encoding)) arguments.add("-encoding", encoding);
      var modules = getModules();
      if (assigned(modules)) arguments.add("--module", String.join(",", new TreeSet<>(modules)));
    }
    public Path getDestinationDirectory() {
      return destinationDirectory;
    }
    public T setDestinationDirectory(Path directory) {
      this.destinationDirectory = directory;
      return (T) this;
    }
    public String getCharacterEncodingUsedBySourceFiles() {
      return characterEncodingUsedBySourceFiles;
    }
    public T setCharacterEncodingUsedBySourceFiles(String encoding) {
      this.characterEncodingUsedBySourceFiles = encoding;
      return (T) this;
    }
    public Set<String> getModules() {
      return modules;
    }
    public T setModules(Set<String> modules) {
      this.modules = modules;
      return (T) this;
    }
  }
  public static class Jar extends Call {
    public Jar() {
      super("jar");
    }
    public String toLabel() {
      return "Operate on JAR file";
    }
  }
  public static class Javac extends GenericSourcesConsumer<Javac> {
    private Version versionOfModulesThatAreBeingCompiled;
    private List<String> patternsWhereToFindSourceFiles;
    private Map<String, List<Path>> pathsWhereToFindSourceFiles;
    private Map<String, List<Path>> pathsWhereToFindMoreAssetsPerModule;
    private List<Path> pathsWhereToFindApplicationModules;
    private int compileForVirtualMachineVersion;
    private boolean enablePreviewLanguageFeatures;
    private boolean generateMetadataForMethodParameters;
    private boolean outputMessagesAboutWhatTheCompilerIsDoing;
    private boolean outputSourceLocationsOfDeprecatedUsages;
    private boolean terminateCompilationIfWarningsOccur;
    public Javac() {
      super("javac");
    }
    public String toLabel() {
      return "Compile module(s): " + getModules();
    }
    protected void addConfiguredArguments(Arguments arguments) {
      super.addConfiguredArguments(arguments);
      var version = getVersionOfModulesThatAreBeingCompiled();
      if (assigned(version)) arguments.add("--module-version", version);
      var patterns = getPatternsWhereToFindSourceFiles();
      if (assigned(patterns)) arguments.add("--module-source-path", joinPaths(patterns));
      var specific = getPathsWhereToFindSourceFiles();
      if (assigned(specific))
        for (var entry : specific.entrySet())
          arguments.add("--module-source-path", entry.getKey() + '=' + join(entry.getValue()));
      var patches = getPathsWhereToFindMoreAssetsPerModule();
      if (assigned(patches))
        for (var patch : patches.entrySet())
          arguments.add("--patch-module", patch.getKey() + '=' + join(patch.getValue()));
      var modulePath = getPathsWhereToFindApplicationModules();
      if (assigned(modulePath)) arguments.add("--module-path", join(modulePath));
      var release = getCompileForVirtualMachineVersion();
      if (assigned(release)) arguments.add("--release", release);
      if (isEnablePreviewLanguageFeatures()) arguments.add("--enable-preview");
      if (isGenerateMetadataForMethodParameters()) arguments.add("-parameters");
      if (isOutputSourceLocationsOfDeprecatedUsages()) arguments.add("-deprecation");
      if (isOutputMessagesAboutWhatTheCompilerIsDoing()) arguments.add("-verbose");
      if (isTerminateCompilationIfWarningsOccur()) arguments.add("-Werror");
    }
    public Version getVersionOfModulesThatAreBeingCompiled() {
      return versionOfModulesThatAreBeingCompiled;
    }
    public Javac setVersionOfModulesThatAreBeingCompiled(
        Version versionOfModulesThatAreBeingCompiled) {
      this.versionOfModulesThatAreBeingCompiled = versionOfModulesThatAreBeingCompiled;
      return this;
    }
    public List<String> getPatternsWhereToFindSourceFiles() {
      return patternsWhereToFindSourceFiles;
    }
    public Javac setPatternsWhereToFindSourceFiles(List<String> patterns) {
      this.patternsWhereToFindSourceFiles = patterns;
      return this;
    }
    public Map<String, List<Path>> getPathsWhereToFindSourceFiles() {
      return pathsWhereToFindSourceFiles;
    }
    public Javac setPathsWhereToFindSourceFiles(Map<String, List<Path>> map) {
      this.pathsWhereToFindSourceFiles = map;
      return this;
    }
    public Map<String, List<Path>> getPathsWhereToFindMoreAssetsPerModule() {
      return pathsWhereToFindMoreAssetsPerModule;
    }
    public Javac setPathsWhereToFindMoreAssetsPerModule(Map<String, List<Path>> map) {
      this.pathsWhereToFindMoreAssetsPerModule = map;
      return this;
    }
    public List<Path> getPathsWhereToFindApplicationModules() {
      return pathsWhereToFindApplicationModules;
    }
    public Javac setPathsWhereToFindApplicationModules(
        List<Path> pathsWhereToFindApplicationModules) {
      this.pathsWhereToFindApplicationModules = pathsWhereToFindApplicationModules;
      return this;
    }
    public int getCompileForVirtualMachineVersion() {
      return compileForVirtualMachineVersion;
    }
    public Javac setCompileForVirtualMachineVersion(int release) {
      this.compileForVirtualMachineVersion = release;
      return this;
    }
    public boolean isEnablePreviewLanguageFeatures() {
      return enablePreviewLanguageFeatures;
    }
    public Javac setEnablePreviewLanguageFeatures(boolean preview) {
      this.enablePreviewLanguageFeatures = preview;
      return this;
    }
    public boolean isGenerateMetadataForMethodParameters() {
      return generateMetadataForMethodParameters;
    }
    public Javac setGenerateMetadataForMethodParameters(boolean parameters) {
      this.generateMetadataForMethodParameters = parameters;
      return this;
    }
    public boolean isOutputMessagesAboutWhatTheCompilerIsDoing() {
      return outputMessagesAboutWhatTheCompilerIsDoing;
    }
    public Javac setOutputMessagesAboutWhatTheCompilerIsDoing(boolean verbose) {
      this.outputMessagesAboutWhatTheCompilerIsDoing = verbose;
      return this;
    }
    public boolean isOutputSourceLocationsOfDeprecatedUsages() {
      return outputSourceLocationsOfDeprecatedUsages;
    }
    public Javac setOutputSourceLocationsOfDeprecatedUsages(boolean deprecation) {
      this.outputSourceLocationsOfDeprecatedUsages = deprecation;
      return this;
    }
    public boolean isTerminateCompilationIfWarningsOccur() {
      return terminateCompilationIfWarningsOccur;
    }
    public Javac setTerminateCompilationIfWarningsOccur(boolean error) {
      this.terminateCompilationIfWarningsOccur = error;
      return this;
    }
  }
  public static class Javadoc extends GenericSourcesConsumer<Javadoc> {
    private List<String> patternsWhereToFindSourceFiles;
    private Map<String, List<Path>> pathsWhereToFindSourceFiles;
    private Map<String, List<Path>> pathsWhereToFindMoreAssetsPerModule;
    private List<Path> pathsWhereToFindApplicationModules;
    private String characterEncodingUsedBySourceFiles;
    private int compileForVirtualMachineVersion;
    private boolean enablePreviewLanguageFeatures;
    private boolean outputMessagesAboutWhatJavadocIsDoing;
    private boolean shutOffDisplayingStatusMessages;
    public Javadoc() {
      super("javadoc");
    }
    public String toLabel() {
      return "Generate API documentation for " + getModules();
    }
    protected void addConfiguredArguments(Arguments arguments) {
      super.addConfiguredArguments(arguments);
      var patterns = getPatternsWhereToFindSourceFiles();
      if (assigned(patterns)) arguments.add("--module-source-path", joinPaths(patterns));
      var specific = getPathsWhereToFindSourceFiles();
      if (assigned(specific))
        for (var entry : specific.entrySet())
          arguments.add("--module-source-path", entry.getKey() + '=' + join(entry.getValue()));
      var patches = getPathsWhereToFindMoreAssetsPerModule();
      if (assigned(patches))
        for (var patch : patches.entrySet())
          arguments.add("--patch-module", patch.getKey() + '=' + join(patch.getValue()));
      var modulePath = getPathsWhereToFindApplicationModules();
      if (assigned(modulePath)) arguments.add("--module-path", join(modulePath));
      var encoding = getCharacterEncodingUsedBySourceFiles();
      if (assigned(encoding)) arguments.add("-encoding", encoding);
      var release = getCompileForVirtualMachineVersion();
      if (assigned(release)) arguments.add("--release", release);
      if (isEnablePreviewLanguageFeatures()) arguments.add("--enable-preview");
      if (isOutputMessagesAboutWhatJavadocIsDoing()) arguments.add("-verbose");
      if (isShutOffDisplayingStatusMessages()) arguments.add("-quiet");
    }
    public List<String> getPatternsWhereToFindSourceFiles() {
      return patternsWhereToFindSourceFiles;
    }
    public Javadoc setPatternsWhereToFindSourceFiles(List<String> patterns) {
      this.patternsWhereToFindSourceFiles = patterns;
      return this;
    }
    public Map<String, List<Path>> getPathsWhereToFindSourceFiles() {
      return pathsWhereToFindSourceFiles;
    }
    public Javadoc setPathsWhereToFindSourceFiles(Map<String, List<Path>> map) {
      this.pathsWhereToFindSourceFiles = map;
      return this;
    }
    public Map<String, List<Path>> getPathsWhereToFindMoreAssetsPerModule() {
      return pathsWhereToFindMoreAssetsPerModule;
    }
    public Javadoc setPathsWhereToFindMoreAssetsPerModule(Map<String, List<Path>> map) {
      this.pathsWhereToFindMoreAssetsPerModule = map;
      return this;
    }
    public List<Path> getPathsWhereToFindApplicationModules() {
      return pathsWhereToFindApplicationModules;
    }
    public Javadoc setPathsWhereToFindApplicationModules(List<Path> paths) {
      this.pathsWhereToFindApplicationModules = paths;
      return this;
    }
    public String getCharacterEncodingUsedBySourceFiles() {
      return characterEncodingUsedBySourceFiles;
    }
    public Javadoc setCharacterEncodingUsedBySourceFiles(String encoding) {
      this.characterEncodingUsedBySourceFiles = encoding;
      return this;
    }
    public int getCompileForVirtualMachineVersion() {
      return compileForVirtualMachineVersion;
    }
    public Javadoc setCompileForVirtualMachineVersion(int release) {
      this.compileForVirtualMachineVersion = release;
      return this;
    }
    public boolean isEnablePreviewLanguageFeatures() {
      return enablePreviewLanguageFeatures;
    }
    public Javadoc setEnablePreviewLanguageFeatures(boolean preview) {
      this.enablePreviewLanguageFeatures = preview;
      return this;
    }
    public boolean isOutputMessagesAboutWhatJavadocIsDoing() {
      return outputMessagesAboutWhatJavadocIsDoing;
    }
    public Javadoc setOutputMessagesAboutWhatJavadocIsDoing(boolean verbose) {
      this.outputMessagesAboutWhatJavadocIsDoing = verbose;
      return this;
    }
    public boolean isShutOffDisplayingStatusMessages() {
      return shutOffDisplayingStatusMessages;
    }
    public Javadoc setShutOffDisplayingStatusMessages(boolean quiet) {
      this.shutOffDisplayingStatusMessages = quiet;
      return this;
    }
  }
  public static class Jlink extends Call {
    private Path locationOfTheGeneratedRuntimeImage;
    private Set<String> modules;
    public Jlink() {
      super("jlink");
    }
    public String toLabel() {
      return "Create a custom runtime image with dependencies for " + getModules();
    }
    protected void addConfiguredArguments(Arguments arguments) {
      var output = getLocationOfTheGeneratedRuntimeImage();
      if (assigned(output)) arguments.add("--output", output);
      var modules = getModules();
      if (assigned(modules)) arguments.add("--add-modules", String.join(",", new TreeSet<>(modules)));
    }
    public Path getLocationOfTheGeneratedRuntimeImage() {
      return locationOfTheGeneratedRuntimeImage;
    }
    public Jlink setLocationOfTheGeneratedRuntimeImage(Path output) {
      this.locationOfTheGeneratedRuntimeImage = output;
      return this;
    }
    public Set<String> getModules() {
      return modules;
    }
    public Jlink setModules(Set<String> modules) {
      this.modules = modules;
      return this;
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
  public static class Logbook implements System.Logger {
    public static Logbook ofSystem() {
      var debug = Boolean.getBoolean("ebug") || "".equals(System.getProperty("ebug"));
      var dryRun = Boolean.getBoolean("ry-run") || "".equals(System.getProperty("ry-run"));
      return new Logbook(System.out::println, debug, dryRun);
    }
    private final Consumer<String> consumer;
    private final boolean debug;
    private final boolean dryRun;
    private final Collection<Entry> entries;
    public Logbook(Consumer<String> consumer, boolean debug, boolean dryRun) {
      this.consumer = consumer;
      this.debug = debug;
      this.dryRun = dryRun;
      this.entries = new ConcurrentLinkedQueue<>();
    }
    public boolean isDebug() {
      return debug;
    }
    public boolean isDryRun() {
      return dryRun;
    }
    public String getName() {
      return "Logbook";
    }
    public boolean isLoggable(Level level) {
      if (level == Level.ALL) return isDebug();
      if (level == Level.OFF) return isDryRun();
      return true;
    }
    public void log(Level level, ResourceBundle bundle, String message, Throwable thrown) {
      if (message == null || message.isBlank()) return;
      var entry = new Entry(level, message, thrown);
      if (debug) consumer.accept(entry.toString());
      entries.add(entry);
    }
    public void log(Level level, ResourceBundle bundle, String pattern, Object... arguments) {
      var message = arguments == null ? pattern : MessageFormat.format(pattern, arguments);
      log(level, bundle, message, (Throwable) null);
    }
    public Stream<Entry> entries(Level threshold) {
      return entries.stream().filter(entry -> entry.level.getSeverity() >= threshold.getSeverity());
    }
    public List<String> messages() {
      return lines(entry -> entry.message);
    }
    public List<String> lines(Function<Entry, String> mapper) {
      return entries.stream().map(mapper).collect(Collectors.toList());
    }
    public static final class Entry {
      private final Level level;
      private final String message;
      private final Throwable exception;
      public Entry(Level level, String message, Throwable exception) {
        this.level = level;
        this.message = message;
        this.exception = exception;
      }
      public Level level() {
        return level;
      }
      public String message() {
        return message;
      }
      public Throwable exception() {
        return exception;
      }
      public String toString() {
        var exceptionMessage = exception == null ? "" : " -> " + exception;
        return String.format("%c|%s%s", level.name().charAt(0), message, exceptionMessage);
      }
    }
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
    public static List<ToolProvider> findTools(String module, List<Path> modulePaths) {
      var boot = ModuleLayer.boot();
      var roots = Set.of(module);
      var finder = ModuleFinder.of(modulePaths.toArray(Path[]::new));
      var parent = ClassLoader.getPlatformClassLoader();
      try {
        var configuration = boot.configuration().resolveAndBind(finder, ModuleFinder.of(), roots);
        var controller = ModuleLayer.defineModulesWithOneLoader(configuration, List.of(boot), parent);
        var layer = controller.layer();
        var loader = layer.findLoader(module);
        loader.setDefaultAssertionStatus(true);
        var services = ServiceLoader.load(layer, ToolProvider.class);
        return services.stream().map(ServiceLoader.Provider::get).collect(Collectors.toList());
      } catch (FindException exception) {
        var message = new StringJoiner(System.lineSeparator());
        message.add(exception.getMessage());
        message.add("Module path:");
        modulePaths.forEach(path -> message.add("\t" + path));
        message.add("Finder finds module(s):");
        finder.findAll().stream()
            .sorted(Comparator.comparing(ModuleReference::descriptor))
            .forEach(reference -> message.add("\t" + reference));
        message.add("");
        throw new RuntimeException(message.toString(), exception);
      }
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
    public static String modulePatternForm(Path info, String module) {
      var pattern = info.normalize().getParent().toString().replace(module, "*");
      if (pattern.equals("*")) return ".";
      if (pattern.endsWith("*")) return pattern.substring(0, pattern.length() - 2);
      if (pattern.startsWith("*")) return "." + File.separator + pattern;
      return pattern;
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
  public static class ModulesMap extends TreeMap<String, String> {
    private static final long serialVersionUID = -7978021121082640440L;
    public ModulesMap() {
      put("junit", "https://repo.maven.apache.org/maven2/junit/junit/4.13/junit-4.13.jar");
      put(
          "net.bytebuddy",
          "https://repo.maven.apache.org/maven2/net/bytebuddy/byte-buddy/1.10.10/byte-buddy-1.10.10.jar");
      put(
          "net.bytebuddy.agent",
          "https://repo.maven.apache.org/maven2/net/bytebuddy/byte-buddy-agent/1.10.10/byte-buddy-agent-1.10.10.jar");
      put(
          "org.apiguardian.api",
          "https://repo.maven.apache.org/maven2/org/apiguardian/apiguardian-api/1.1.0/apiguardian-api-1.1.0.jar");
      put(
          "org.assertj.core",
          "https://repo.maven.apache.org/maven2/org/assertj/assertj-core/3.16.1/assertj-core-3.16.1.jar");
      put(
          "org.hamcrest",
          "https://repo.maven.apache.org/maven2/org/hamcrest/hamcrest/2.2/hamcrest-2.2.jar");
      put(
          "org.junit.jupiter",
          "https://repo.maven.apache.org/maven2/org/junit/jupiter/junit-jupiter/5.7.0-M1/junit-jupiter-5.7.0-M1.jar");
      put(
          "org.junit.jupiter.api",
          "https://repo.maven.apache.org/maven2/org/junit/jupiter/junit-jupiter-api/5.7.0-M1/junit-jupiter-api-5.7.0-M1.jar");
      put(
          "org.junit.jupiter.engine",
          "https://repo.maven.apache.org/maven2/org/junit/jupiter/junit-jupiter-engine/5.7.0-M1/junit-jupiter-engine-5.7.0-M1.jar");
      put(
          "org.junit.jupiter.params",
          "https://repo.maven.apache.org/maven2/org/junit/jupiter/junit-jupiter-params/5.7.0-M1/junit-jupiter-params-5.7.0-M1.jar");
      put(
          "org.junit.platform.commons",
          "https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-commons/1.7.0-M1/junit-platform-commons-1.7.0-M1.jar");
      put(
          "org.junit.platform.console",
          "https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-console/1.7.0-M1/junit-platform-console-1.7.0-M1.jar");
      put(
          "org.junit.platform.engine",
          "https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-engine/1.7.0-M1/junit-platform-engine-1.7.0-M1.jar");
      put(
          "org.junit.platform.launcher",
          "https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-launcher/1.7.0-M1/junit-platform-launcher-1.7.0-M1.jar");
      put(
          "org.junit.platform.reporting",
          "https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-reporting/1.7.0-M1/junit-platform-reporting-1.7.0-M1.jar");
      put(
          "org.junit.platform.testkit",
          "https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-testkit/1.7.0-M1/junit-platform-testkit-1.7.0-M1.jar");
      put(
          "org.junit.vintage.engine",
          "https://repo.maven.apache.org/maven2/org/junit/vintage/junit-vintage-engine/5.7.0-M1/junit-vintage-engine-5.7.0-M1.jar");
      put(
          "org.objectweb.asm",
          "https://repo.maven.apache.org/maven2/org/ow2/asm/asm/8.0.1/asm-8.0.1.jar");
      put(
          "org.objectweb.asm.commons",
          "https://repo.maven.apache.org/maven2/org/ow2/asm/asm-commons/8.0.1/asm-commons-8.0.1.jar");
      put(
          "org.objectweb.asm.tree",
          "https://repo.maven.apache.org/maven2/org/ow2/asm/asm-tree/8.0.1/asm-tree-8.0.1.jar");
      put(
          "org.objectweb.asm.tree.analysis",
          "https://repo.maven.apache.org/maven2/org/ow2/asm/asm-analysis/8.0.1/asm-analysis-8.0.1.jar");
      put(
          "org.objectweb.asm.util",
          "https://repo.maven.apache.org/maven2/org/ow2/asm/asm-util/8.0.1/asm-util-8.0.1.jar");
      put(
          "org.opentest4j",
          "https://repo.maven.apache.org/maven2/org/opentest4j/opentest4j/1.2.0/opentest4j-1.2.0.jar");
    }
  }
  public static class ModulesResolver {
    private final Path[] paths;
    private final Set<String> declared;
    private final Consumer<Set<String>> transporter;
    private final Set<String> system;
    public ModulesResolver(List<Path> paths, Set<String> declared, Consumer<Set<String>> transporter) {
      this.paths = paths.toArray(Path[]::new);
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
  public static class ModulesWalker {
    public static Project.Builder walk(Project.Builder builder) {
      var base = builder.getBase().directory();
      var moduleInfoFiles =
          builder.getWalkModuleInfoFiles().isEmpty()
              ? Paths.find(List.of(base), Paths::isModuleInfoJavaFile)
              : builder.getWalkModuleInfoFiles();
      if (moduleInfoFiles.isEmpty()) throw new IllegalStateException("No module found: " + base);
      var walker = new ModulesWalker(builder, moduleInfoFiles);
      return builder.structure(walker.newStructure());
    }
    private final Project.Base base;
    private final Project.Info info;
    private final Project.Library library;
    private final Project.Tuner tuner;
    private final List<Path> moduleInfoFiles;
    public ModulesWalker(Project.Builder builder, List<Path> moduleInfoFiles) {
      this.base = builder.getBase();
      this.info = builder.getInfo();
      this.library = builder.getLibrary();
      this.tuner = builder.getTuner();
      this.moduleInfoFiles = moduleInfoFiles;
    }
    public Project.Structure newStructure() {
      try {
        return newStructureWithMainTestPreviewRealms();
      } catch (IllegalStateException ignore) {
      }
      return newStructureWithSingleUnnamedRealm();
    }
    public Project.Structure newStructureWithSingleUnnamedRealm() {
      var realms = List.of(newRealm("", moduleInfoFiles, false, true, List.of()));
      return new Project.Structure(library, realms);
    }
    public Project.Structure newStructureWithMainTestPreviewRealms() {
      var mainModuleInfoFiles = new ArrayList<Path>();
      var testModuleInfoFiles = new ArrayList<Path>();
      var viewModuleInfoFiles = new ArrayList<Path>();
      for (var moduleInfoFile : moduleInfoFiles) {
        var deque = Paths.deque(moduleInfoFile);
        if (Collections.frequency(deque, "main") == 1) {
          mainModuleInfoFiles.add(moduleInfoFile);
        } else if (Collections.frequency(deque, "test") == 1) {
          testModuleInfoFiles.add(moduleInfoFile);
        } else if (Collections.frequency(deque, "test-preview") == 1) {
          viewModuleInfoFiles.add(moduleInfoFile);
        } else {
          var message = new StringBuilder("Cannot guess realm of " + moduleInfoFile);
          message.append('\n').append('\n');
          for (var file : moduleInfoFiles) message.append("\t\t").append(file).append('\n');
          throw new IllegalStateException(message.toString());
        }
      }
      var main = newRealm("main", mainModuleInfoFiles, false, false, List.of());
      var test = newRealm("test", testModuleInfoFiles, false, true, List.of(main));
      var view = newRealm("test-preview", viewModuleInfoFiles, true, true, List.of(main, test));
      var realms = new ArrayList<Project.Realm>();
      if (!main.units().isEmpty()) realms.add(main);
      if (!test.units().isEmpty()) realms.add(test);
      if (!view.units().isEmpty()) realms.add(view);
      return new Project.Structure(library, realms);
    }
    public Project.Realm newRealm(
        String realm,
        List<Path> moduleInfoFiles,
        boolean preview,
        boolean test,
        List<Project.Realm> upstreams) {
      var moduleNames = new TreeSet<String>();
      var moduleSourcePathPatterns = new TreeSet<String>();
      var units = new ArrayList<Project.Unit>();
      var javadocCommentFound = false;
      for (var moduleInfoFile : moduleInfoFiles) {
        javadocCommentFound = javadocCommentFound || Paths.isJavadocCommentAvailable(moduleInfoFile);
        var descriptor = Modules.describe(moduleInfoFile);
        var module = descriptor.name();
        moduleNames.add(module);
        moduleSourcePathPatterns.add(Modules.modulePatternForm(moduleInfoFile, descriptor.name()));
        var classes = base.classes(realm, module);
        var modules = base.modules(realm);
        var jar = modules.resolve(module + ".jar");
        var context = Map.of("realm", realm, "module", module);
        var jarCreate = new Jar();
        var jarCreateArgs = jarCreate.getAdditionalArguments();
        jarCreateArgs.add("--create").add("--file", jar);
        descriptor.mainClass().ifPresent(main -> jarCreateArgs.add("--main-class", main));
        jarCreateArgs.add("-C", classes, ".");
        tuner.tune(jarCreate, context);
        var jarDescribe = new Jar();
        jarDescribe.getAdditionalArguments().add("--describe-module").add("--file", jar);
        tuner.tune(jarDescribe, context);
        var task =
            Task.sequence(
                "Create modular JAR file " + jar.getFileName(),
                new Task.CreateDirectories(jar.getParent()),
                jarCreate.toTask(),
                jarDescribe.toTask());
        var parent = moduleInfoFile.getParent();
        units.add(new Project.Unit(descriptor, List.of(parent), List.of(task)));
      }
      var namesOfUpstreams = upstreams.stream().map(Project.Realm::name).collect(Collectors.toList());
      var patchesToUpstreams = patches(units, upstreams);
      var context = Map.of("realm", realm);
      var javac =
          new Javac()
              .setModules(moduleNames)
              .setVersionOfModulesThatAreBeingCompiled(info.version())
              .setPatternsWhereToFindSourceFiles(new ArrayList<>(moduleSourcePathPatterns))
              .setPathsWhereToFindApplicationModules(base.modulePaths(namesOfUpstreams))
              .setPathsWhereToFindMoreAssetsPerModule(patchesToUpstreams)
              .setDestinationDirectory(base.classes(realm));
      if (preview) {
        javac.setCompileForVirtualMachineVersion(Runtime.version().feature());
        javac.setEnablePreviewLanguageFeatures(true);
        javac.getAdditionalArguments().add("-X" + "lint:-preview");
      }
      tuner.tune(javac, context);
      var tasks = new ArrayList<Task>();
      if (javadocCommentFound) {
        var javadoc =
            new Javadoc()
                .setDestinationDirectory(base.api())
                .setModules(moduleNames)
                .setPatternsWhereToFindSourceFiles(new ArrayList<>(moduleSourcePathPatterns))
                .setPathsWhereToFindApplicationModules(base.modulePaths(namesOfUpstreams))
                .setPathsWhereToFindMoreAssetsPerModule(patchesToUpstreams);
        tuner.tune(javadoc, context);
        tasks.add(javadoc.toTask());
      }
      var mainModule = Modules.findMainModule(units.stream().map(Project.Unit::descriptor));
      if (mainModule.isPresent()) {
        var jlink =
            new Jlink().setModules(moduleNames).setLocationOfTheGeneratedRuntimeImage(base.image());
        var launcher = Path.of(mainModule.get().replace('.', '/')).getFileName().toString();
        var arguments = jlink.getAdditionalArguments();
        arguments.add("--module-path", base.modules(realm));
        arguments.add("--launcher", launcher + '=' + mainModule.get());
        tuner.tune(jlink, context);
        tasks.add(
            Task.sequence(
                String.format("Create custom runtime image with '%s' as launcher", launcher),
                new Task.DeleteDirectories(base.image()),
                jlink.toTask()));
      }
      if (test) {
        for (var unit : units) {
          var module = unit.name();
          var jar = base.modules(realm).resolve(module + ".jar");
          var modulePaths = new ArrayList<Path>();
          modulePaths.add(jar);
          modulePaths.addAll(base.modulePaths(namesOfUpstreams));
          modulePaths.add(base.modules(realm));
          tasks.add(new Task.RunTestModule(module, modulePaths));
        }
      }
      return new Project.Realm(realm, units, javac, tasks);
    }
    static Map<String, List<Path>> patches(List<Project.Unit> units, List<Project.Realm> upstreams) {
      if (units.isEmpty() || upstreams.isEmpty()) return Map.of();
      var patches = new TreeMap<String, List<Path>>();
      for (var unit : units) {
        var module = unit.name();
        for (var upstream : upstreams)
          upstream.units().stream()
              .filter(up -> up.name().equals(module))
              .findAny()
              .ifPresent(up -> patches.put(module, up.paths()));
      }
      return patches;
    }
  }
  public static class Paths {
    public static Deque<String> deque(Path path) {
      var deque = new ArrayDeque<String>();
      path.forEach(name -> deque.addFirst(name.toString()));
      return deque;
    }
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
    public static boolean isJavadocCommentAvailable(Path path) {
      try {
        return Files.readString(path).contains("/**");
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
    public static boolean isModuleInfoJavaFile(Path path) {
      return Files.isRegularFile(path) && path.getFileName().toString().equals("module-info.java");
    }
    public static List<Path> find(Collection<Path> roots, Predicate<Path> filter) {
      var files = new TreeSet<Path>();
      for (var root : roots) {
        try (var stream = Files.walk(root)) {
          stream.filter(filter).forEach(files::add);
        } catch (Exception e) {
          throw new Error("Walk directory '" + root + "' failed: " + e, e);
        }
      }
      return List.copyOf(files);
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
      throw new IllegalStateException("Copy " + uri + " failed: response=" + response);
    }
    public String read(URI uri) throws Exception {
      var request = HttpRequest.newBuilder(uri).GET();
      return client.send(request.build(), BodyHandlers.ofString()).body();
    }
  }
}
