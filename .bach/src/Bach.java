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
import java.lang.module.ModuleDescriptor.Requires;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringJoiner;
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
    return of(Path.of(""));
  }
  public static Bach of(Path directory) {
    return of(Project.newProject(directory).build());
  }
  public static Bach of(UnaryOperator<Project.Builder> operator) {
    return of(operator.apply(Project.newProject(Path.of(""))).build());
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
  Bach(Logbook logbook, Project project, Supplier<HttpClient> httpClient) {
    this.logbook = Objects.requireNonNull(logbook, "logbook");
    this.project = Objects.requireNonNull(project, "project");
    this.httpClient = Functions.memoize(httpClient);
    logbook.log(Level.TRACE, "Initialized " + toString());
    logbook.log(Level.DEBUG, String.join(System.lineSeparator(), project.toStrings()));
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
    var summary = new Summary();
    execute(buildSequence());
    return summary;
  }
  Task buildSequence() {
    var tasks = new ArrayList<Task>();
    for (var realm : project.structure().realms()) {
      tasks.add(realm.javac());
      for (var unit : realm.units()) tasks.addAll(unit.tasks());
      tasks.addAll(realm.tasks());
    }
    return Task.sequence("Build Sequence", tasks);
  }
  void execute(Task task) {
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
      }
      return;
    }
    logbook.log(Level.TRACE, "+ {0}", label);
    var start = System.currentTimeMillis();
    for (var sub : tasks) execute(sub);
    var duration = System.currentTimeMillis() - start;
    logbook.log(Level.TRACE, "= {0} took {1} ms", label, duration);
  }
  void execute(ToolProvider tool, PrintWriter out, PrintWriter err, String... args) {
    var call = (tool.name() + ' ' + String.join(" ", args)).trim();
    logbook.log(Level.DEBUG, call);
    var code = tool.run(out, err, args);
    if (code != 0) throw new AssertionError("Tool run exit code: " + code + "\n\t" + call);
  }
  public String toString() {
    return "Bach.java " + VERSION;
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
    public static Builder newProject(Path directory) {
      return new Builder().base(Base.of(directory)).walk();
    }
    public static Builder newProject(String title, String version) {
      return new Builder().title(title).version(Version.parse(version));
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
      list.add("\tunits: " + structure.units().size());
      for (var realm : structure.realms()) {
        list.add("\tRealm " + realm.name());
        list.add("\t\tjavac: " + String.format("%.77s...", realm.javac().getLabel()));
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
      return structure.units.stream().map(Unit::name).collect(Collectors.toCollection(TreeSet::new));
    }
    public Set<String> toRequiredModuleNames() {
      return Modules.required(structure.units().stream().map(Unit::descriptor));
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
      private final List<Realm> realms;
      private final List<Unit> units;
      public Structure(List<Realm> realms, List<Unit> units) {
        this.realms = List.copyOf(Objects.requireNonNull(realms, "realms"));
        this.units = List.copyOf(Objects.requireNonNull(units, "units"));
      }
      public List<Realm> realms() {
        return realms;
      }
      public List<Unit> units() {
        return units;
      }
    }
    public static final class Realm {
      private final String name;
      private final List<Unit> units;
      private final Task javac;
      private final List<Task> tasks;
      public Realm(String name, List<Unit> units, Task javac, List<Task> tasks) {
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
      public Task javac() {
        return javac;
      }
      public List<Task> tasks() {
        return tasks;
      }
    }
    public static final class Unit {
      private final ModuleDescriptor descriptor;
      private final List<Task> tasks;
      public Unit(ModuleDescriptor descriptor, List<Task> tasks) {
        this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
        this.tasks = List.copyOf(Objects.requireNonNull(tasks, "tasks"));
      }
      public ModuleDescriptor descriptor() {
        return descriptor;
      }
      public List<Task> tasks() {
        return tasks;
      }
      public String name() {
        return descriptor.name();
      }
    }
    public static class Builder {
      private Base base = Base.of();
      private String title = "Project Title";
      private Version version = Version.parse("1-ea");
      private Structure structure = new Structure(List.of(), List.of());
      public Project build() {
        var info = new Info(title, version);
        return new Project(base, info, structure);
      }
      public Builder base(Base base) {
        this.base = base;
        return this;
      }
      public Builder title(String title) {
        this.title = title;
        return this;
      }
      public Builder version(Version version) {
        this.version = version;
        return this;
      }
      public Builder structure(Structure structure) {
        this.structure = structure;
        return this;
      }
      public Builder walk() {
        var moduleInfoFiles = Paths.find(List.of(base.directory()), Paths::isModuleInfoJavaFile);
        if (moduleInfoFiles.isEmpty()) throw new IllegalStateException("No module found: " + base);
        return walkAndSetSingleUnnamedRealmStructure(moduleInfoFiles);
      }
      public Builder walkAndSetSingleUnnamedRealmStructure(List<Path> moduleInfoFiles) {
        var moduleNames = new TreeSet<String>();
        var moduleSourcePathPatterns = new TreeSet<String>();
        var units = new ArrayList<Unit>();
        for (var info : moduleInfoFiles) {
          var descriptor = Modules.describe(info);
          var module = descriptor.name();
          moduleNames.add(module);
          moduleSourcePathPatterns.add(Modules.modulePatternForm(info, descriptor.name()));
          var classes = base.classes("", module);
          var modules = base.modules("");
          var jar = modules.resolve(module + ".jar");
          units.add(new Unit(descriptor, List.of(new Task.CreateJar(jar, classes))));
        }
        var moduleSourcePath = String.join(File.pathSeparator, moduleSourcePathPatterns);
        var realm =
            new Realm(
                "",
                units,
                Task.runTool(
                    "javac",
                    "--module",
                    String.join(",", moduleNames),
                    "--module-source-path",
                    moduleSourcePath,
                    "-d",
                    base.classes("")),
                List.of(Task.runTool("javadoc", "--version"), Task.runTool("jlink", "--version")));
        var directoryName = base.directory().toAbsolutePath().getFileName();
        return title("Project " + Optional.ofNullable(directoryName).map(Path::toString).orElse("?"))
            .structure(new Structure(List.of(realm), units));
      }
    }
  }
  public class Summary {
    public void assertSuccessful() {}
  }
  public static class Task {
    public static Task sequence(String label, Task... tasks) {
      return sequence(label, List.of(tasks));
    }
    public static Task sequence(String label, List<Task> tasks) {
      return new Task(label, tasks);
    }
    public static Task runTool(String name, Object... arguments) {
      var tool = ToolProvider.findFirst(name).orElseThrow();
      var args = new String[arguments.length];
      for (int i = 0; i < args.length; i++) args[i] = arguments[i].toString();
      return new RunTool(tool, args);
    }
    private final String label;
    private final List<Task> list;
    public Task() {
      this("", List.of());
    }
    public Task(String label, List<Task> list) {
      Objects.requireNonNull(label, "label");
      this.label = label.isBlank() ? getClass().getSimpleName() : label;
      this.list = List.copyOf(Objects.requireNonNull(list, "list"));
    }
    public String getLabel() {
      return label;
    }
    public List<Task> getList() {
      return list;
    }
    public String toString() {
      return new StringJoiner(", ", Task.class.getSimpleName() + "[", "]")
          .add("label='" + label + "'")
          .add("list.size=" + list.size())
          .toString();
    }
    public void execute(Bach bach) throws Exception {}
    static class RunTool extends Task {
      private final ToolProvider tool;
      private final String[] args;
      public RunTool(ToolProvider tool, String... args) {
        super(tool.name() + " " + String.join(" ", args), List.of());
        this.tool = tool;
        this.args = args;
      }
      public void execute(Bach bach) {
        var out = new StringWriter();
        var err = new StringWriter();
        bach.execute(tool, new PrintWriter(out), new PrintWriter(err), args);
        var outString = out.toString().strip();
        if (!outString.isEmpty()) bach.getLogger().log(Level.DEBUG, outString);
        var errString = err.toString().strip();
        if (!errString.isEmpty()) bach.getLogger().log(Level.WARNING, outString);
      }
    }
    static class CreateDirectories extends Task {
      final Path directory;
      CreateDirectories(Path directory) {
        super("Create directories " + directory.toUri(), List.of());
        this.directory = directory;
      }
      public void execute(Bach bach) throws Exception {
        Files.createDirectories(directory);
      }
    }
    static class DeleteDirectories extends Task {
      final Path directory;
      DeleteDirectories(Path directory) {
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
    static class CreateJar extends Task {
      private static List<Task> list(Path jar, Path classes) {
        return List.of(
            new CreateDirectories(jar.getParent()),
            runTool("jar", "--create", "--file", jar, "-C", classes, "."),
            runTool("jar", "--describe-module", "--file", jar));
      }
      public CreateJar(Path jar, Path classes) {
        super("Create modular JAR file " + jar.getFileName(), list(jar, classes));
      }
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
      entries.add(new Entry(level, message, thrown));
    }
    public void log(Level level, ResourceBundle bundle, String pattern, Object... arguments) {
      entries.add(new Entry(level, MessageFormat.format(pattern, arguments), null));
    }
    public List<String> messages() {
      return lines(entry -> entry.message);
    }
    public List<String> lines(Function<Entry, String> mapper) {
      return entries.stream().map(mapper).collect(Collectors.toList());
    }
    public final class Entry {
      private final Level level;
      private final String message;
      private final Throwable thrown;
      public Entry(Level level, String message, Throwable thrown) {
        this.level = level;
        this.message = message;
        this.thrown = thrown;
        if (debug) consumer.accept(message);
      }
      public String toString() {
        if (thrown == null) return level + "|" + message;
        return level + "|" + message + " -> " + thrown;
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
