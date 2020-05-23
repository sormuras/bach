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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
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
import java.util.LinkedHashMap;
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
  public static Bach of(UnaryOperator<Project.Builder> builder) {
    return of(UnaryOperator.identity(), builder);
  }
  public static Bach of(UnaryOperator<Scanner> scanner, UnaryOperator<Project.Builder> builder) {
    return of(builder.apply(scanner.apply(new Scanner()).newBuilder()).newProject());
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
    return build(Sequencer.Tuner::defaults);
  }
  public Summary build(Sequencer.Tuner tuner) {
    var summary = new Summary(this);
    try {
      execute(new Sequencer(project, tuner).newBuildSequence());
    } finally {
      summary.writeMarkdown(project.base().workspace("summary.md"), true);
    }
    return summary;
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
        logbook.log(Level.INFO, task.getErr().toString().strip());
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
    public static Builder builder() {
      return new Builder();
    }
    private final Base base;
    private final Info info;
    private final Library library;
    private final List<Realm> realms;
    public Project(Base base, Info info, Library library, List<Realm> realms) {
      this.base = Objects.requireNonNull(base, "base");
      this.info = Objects.requireNonNull(info, "info");
      this.library = Objects.requireNonNull(library, "library");
      this.realms = List.copyOf(Objects.requireNonNull(realms, "realms"));
    }
    public Base base() {
      return base;
    }
    public Info info() {
      return info;
    }
    public Library library() {
      return library;
    }
    public List<Realm> realms() {
      return realms;
    }
    public String toString() {
      return new StringJoiner(", ", Project.class.getSimpleName() + "[", "]")
          .add("base=" + base)
          .add("info=" + info)
          .add("library=" + library)
          .add("realms=" + realms)
          .toString();
    }
    public List<String> toStrings() {
      var list = new ArrayList<String>();
      list.add("Project");
      list.add("\ttitle: " + info.title());
      list.add("\tversion: " + info.version());
      list.add("\trealms: " + toRealmLabelNames() + " << " + toUnits().count() + " distinct unit(s)");
      list.add("\tdeclares: " + toDeclaredModuleNames());
      list.add("\trequires: " + toRequiredModuleNames());
      list.add("\texternal: " + toExternalModuleNames());
      for (var realm : realms()) {
        list.add("\tRealm " + realm.name());
        list.add("\t\tflags: " + realm.flags());
        list.add("\t\tupstreams: " + new TreeSet<>(realm.upstreams()));
        for (var unit : realm.units().values()) {
          list.add("\t\tUnit " + unit.toName());
          var module = unit.descriptor();
          list.add("\t\t\tModule Descriptor " + module.toNameAndVersion());
          list.add("\t\t\t\tmain-class: " + module.mainClass().orElse("-"));
          list.add("\t\t\t\trequires: " + unit.toRequiredNames());
          list.add("\t\t\tSources");
          for (var source : unit.sources()) {
            list.add("\t\t\t\tpath: " + source.path());
            list.add("\t\t\t\trelease: " + source.release());
          }
        }
      }
      return list;
    }
    public String toTitleAndVersion() {
      return info.title() + ' ' + info.version();
    }
    public Set<String> toDeclaredModuleNames() {
      return toUnits().map(Unit::toName).collect(Collectors.toCollection(TreeSet::new));
    }
    public Set<String> toExternalModuleNames() {
      var externals = new TreeSet<>(toRequiredModuleNames());
      externals.removeAll(toDeclaredModuleNames());
      externals.removeAll(Modules.declared(ModuleFinder.ofSystem()));
      return externals;
    }
    public Set<String> toRequiredModuleNames() {
      return Modules.required(toUnits().map(Unit::descriptor));
    }
    public Set<String> toRealmLabelNames() {
      return realms.stream().map(Realm::toLabelName).collect(Collectors.toCollection(TreeSet::new));
    }
    public Stream<Unit> toUnits() {
      return realms.stream().flatMap(realm -> realm.units().values().stream());
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
      public Path classes(String realm, String module, int release) {
        return workspace("classes-versions", String.valueOf(release), realm, module);
      }
      public Path image() {
        return workspace("image");
      }
      public Path modules(String realm) {
        return workspace("modules", realm);
      }
      public Path sources(String realm) {
        return workspace("sources", realm);
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
      public enum Flag {
        CREATE_API_DOCUMENTATION,
        CREATE_CUSTOM_RUNTIME_IMAGE,
        ENABLE_PREVIEW_LANGUAGE_FEATURES,
        LAUNCH_TESTS
      }
      private final String name;
      private final Set<Flag> flags;
      private final Map<String, Unit> units;
      private final Set<String> upstreams;
      public Realm(String name, Set<Flag> flags, Map<String, Unit> units, Set<String> upstreams) {
        this.name = Objects.requireNonNull(name, "name");
        this.flags = Set.copyOf(Objects.requireNonNull(flags, "flags"));
        this.units = Map.copyOf(Objects.requireNonNull(units, "units"));
        this.upstreams = Set.copyOf(Objects.requireNonNull(upstreams, "upstreams"));
      }
      public String name() {
        return name;
      }
      public Set<Flag> flags() {
        return flags;
      }
      public Map<String, Unit> units() {
        return units;
      }
      public Set<String> upstreams() {
        return upstreams;
      }
      public Optional<Unit> unit(String module) {
        return Optional.ofNullable(units.get(module));
      }
      public String toLabelName() {
        return name.isEmpty() ? "unnamed" : name;
      }
    }
    public static final class Unit {
      private final ModuleDescriptor descriptor;
      private final List<Source> sources;
      private final List<Path> resources;
      public Unit(ModuleDescriptor descriptor, List<Source> sources, List<Path> resources) {
        this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
        this.sources = List.copyOf(Objects.requireNonNull(sources, "sources"));
        this.resources = List.copyOf(Objects.requireNonNull(resources, "resources"));
      }
      public ModuleDescriptor descriptor() {
        return descriptor;
      }
      public List<Source> sources() {
        return sources;
      }
      public List<Path> resources() {
        return resources;
      }
      public String toName() {
        return descriptor.name();
      }
      public Set<String> toRequiredNames() {
        var names = descriptor.requires().stream().map(ModuleDescriptor.Requires::name);
        return names.collect(Collectors.toCollection(TreeSet::new));
      }
      public boolean isMultiRelease() {
        if (sources.isEmpty()) return false;
        if (sources.size() == 1) return sources.get(0).isTargeted();
        return sources.stream().allMatch(Source::isTargeted);
      }
    }
    public static final class Source {
      private final Path path;
      private final int release;
      public Source(Path path, int release) {
        this.path = Objects.requireNonNull(path, "path");
        this.release = Objects.checkIndex(release, Runtime.version().feature() + 1);
      }
      public Path path() {
        return path;
      }
      public int release() {
        return release;
      }
      public boolean isTargeted() {
        return release != 0;
      }
    }
    public static class Builder {
      private Base base = null;
      private Path baseDirectory = Path.of("");
      private Path baseWorkspace = Bach.WORKSPACE;
      private Info info = null;
      private String infoTitle = "Untitled";
      private String infoVersion = "1-ea";
      private Library library = null;
      private final Set<String> libraryRequired = new TreeSet<>();
      private final Map<String, String> libraryMap = new ModulesMap();
      private List<Realm> realms = List.of();
      public Project newProject() {
        return new Project(
            base == null ? new Base(baseDirectory, baseWorkspace) : base,
            info == null ? new Info(infoTitle, Version.parse(infoVersion)) : info,
            library == null ? new Library(libraryRequired, libraryMap::get) : library,
            realms == null ? List.of() : realms);
      }
      public Builder setBase(Base base) {
        this.base = base;
        return this;
      }
      public Builder setInfo(Info info) {
        this.info = info;
        return this;
      }
      public Builder setLibrary(Library library) {
        this.library = library;
        return this;
      }
      public Builder setRealms(List<Realm> realms) {
        this.realms = realms;
        return this;
      }
      public Builder base(String directory, String... more) {
        return base(Path.of(directory, more));
      }
      public Builder base(Path directory) {
        this.baseDirectory = directory;
        return this;
      }
      public Builder workspace(Path workspace) {
        this.baseWorkspace = workspace;
        return this;
      }
      public Builder title(String title) {
        this.infoTitle = title;
        return this;
      }
      public Builder version(String version) {
        this.infoVersion = version;
        return this;
      }
      public Builder requires(String module, String... more) {
        this.libraryRequired.add(module);
        this.libraryRequired.addAll(List.of(more));
        return this;
      }
      public Builder map(String module, String uri) {
        this.libraryMap.put(module, uri);
        return this;
      }
    }
  }
  public static class Scanner {
    public enum Layout {
      AUTOMATIC,
      DEFAULT,
      MAIN_TEST_PREVIEW
    }
    private Project.Base base = Project.Base.of();
    private List<Path> moduleInfoFiles = new ArrayList<>();
    private int limit = 9;
    private Path offset = Path.of("");
    private Layout layout = Layout.AUTOMATIC;
    public Scanner base(String directory, String... more) {
      return base(Path.of(directory, more));
    }
    public Scanner base(Path directory) {
      return base(Project.Base.of(directory));
    }
    public Scanner base(Project.Base base) {
      this.base = base;
      return this;
    }
    public Scanner moduleInfoFiles(List<Path> moduleInfoFiles) {
      this.moduleInfoFiles = moduleInfoFiles;
      return this;
    }
    public Scanner offset(String offset, String... more) {
      return offset(Path.of(offset, more));
    }
    public Scanner offset(Path offset) {
      this.offset = offset;
      return this;
    }
    public Scanner limit(int limit) {
      this.limit = limit;
      return this;
    }
    public Scanner layout(Layout layout) {
      this.layout = layout;
      return this;
    }
    public Project.Builder newBuilder() {
      if (moduleInfoFiles.isEmpty()) {
        var directory = base.directory().resolve(offset);
        if (Paths.isRoot(directory)) throw new IllegalStateException("Root directory: " + directory);
        var paths = Paths.find(List.of(directory), limit, Paths::isModuleInfoJavaFile);
        if (paths.isEmpty()) throw new IllegalStateException("No module-info.java: " + directory);
        moduleInfoFiles(paths);
      }
      var directoryName = base.directory().toAbsolutePath().getFileName();
      var builder = Project.builder();
      builder.setBase(base);
      if (directoryName != null) builder.title(directoryName.toString());
      builder.setRealms(computeRealms());
      return builder;
    }
    List<Project.Realm> computeRealms() {
      if (layout == Layout.DEFAULT) return computeUnnamedRealm();
      if (layout == Layout.MAIN_TEST_PREVIEW) return computeMainTestPreviewRealms();
      if (layout != Layout.AUTOMATIC) throw new AssertionError("Unexpected layout: " + layout);
      try {
        return computeMainTestPreviewRealms();
      } catch (UnsupportedOperationException ignored) {
      }
      return computeUnnamedRealm();
    }
    List<Project.Realm> computeUnnamedRealm() {
      return List.of(
          new RealmBuilder("")
              .takeMatchingModuleInfoFilesFrom(new ArrayList<>(moduleInfoFiles))
              .flag(Project.Realm.Flag.CREATE_API_DOCUMENTATION)
              .flag(Project.Realm.Flag.CREATE_CUSTOM_RUNTIME_IMAGE)
              .flag(Project.Realm.Flag.LAUNCH_TESTS)
              .build());
    }
    List<Project.Realm> computeMainTestPreviewRealms() {
      var files = new ArrayList<>(moduleInfoFiles);
      var main =
          new RealmBuilder("main")
              .takeMatchingModuleInfoFilesFrom(files)
              .flag(Project.Realm.Flag.CREATE_API_DOCUMENTATION)
              .flag(Project.Realm.Flag.CREATE_CUSTOM_RUNTIME_IMAGE)
              .build();
      var test =
          new RealmBuilder("test")
              .takeMatchingModuleInfoFilesFrom(files)
              .flag(Project.Realm.Flag.LAUNCH_TESTS)
              .upstream("main")
              .build();
      var preview =
          new RealmBuilder("test-preview")
              .takeMatchingModuleInfoFilesFrom(files)
              .flag(Project.Realm.Flag.ENABLE_PREVIEW_LANGUAGE_FEATURES)
              .flag(Project.Realm.Flag.LAUNCH_TESTS)
              .upstream("main")
              .upstream("test")
              .build();
      if (!files.isEmpty()) throw new UnsupportedOperationException("File(s) not taken: " + files);
      var realms = new ArrayList<Project.Realm>();
      if (!main.units().isEmpty()) realms.add(main);
      if (!test.units().isEmpty()) realms.add(test);
      if (!preview.units().isEmpty()) realms.add(preview);
      if (realms.isEmpty()) throw new UnsupportedOperationException("No match in: " + files);
      return List.copyOf(realms);
    }
    private static class RealmBuilder {
      final String name;
      final List<Path> moduleInfoFiles = new ArrayList<>();
      final Set<Project.Realm.Flag> flags = new TreeSet<>();
      final Set<String> upstreams = new TreeSet<>();
      RealmBuilder(String name) {
        this.name = Objects.requireNonNull(name, "name");
      }
      RealmBuilder flag(Project.Realm.Flag flag) {
        flags.add(flag);
        return this;
      }
      RealmBuilder upstream(String upstream) {
        upstreams.add(upstream);
        return this;
      }
      RealmBuilder takeMatchingModuleInfoFilesFrom(List<Path> files) {
        if (files.isEmpty()) return this;
        if (name.isEmpty()) {
          moduleInfoFiles.addAll(files);
          files.clear();
          return this;
        }
        var iterator = files.listIterator();
        while (iterator.hasNext()) {
          var file = iterator.next();
          if (Collections.frequency(Paths.deque(file), name) == 1) {
            moduleInfoFiles.add(file);
            iterator.remove();
          }
        }
        return this;
      }
      Project.Realm build() {
        var units = new TreeMap<String, Project.Unit>();
        for (var info : moduleInfoFiles) {
          var unit = unit(info);
          units.put(unit.toName(), unit);
        }
        return new Project.Realm(name, flags, units, upstreams);
      }
      Project.Unit unit(Path info) {
        var parent = info.getParent();
        var resources = parent.resolveSibling("resources");
        return new Project.Unit(
            Modules.describe(info),
            sources(parent),
            Files.isDirectory(resources) ? List.of(resources) : List.of());
      }
      List<Project.Source> sources(Path infoDirectory) {
        if (Paths.isMultiReleaseDirectory(infoDirectory)) {
          var map = new TreeMap<Integer, Path>(); // sorted by release number
          var paths = Paths.list(infoDirectory.getParent(), Files::isDirectory);
          for (var path : paths) Paths.findMultiReleaseNumber(path).ifPresent(n -> map.put(n, path));
          var sources = new ArrayList<Project.Source>();
          map.forEach((release, path) -> sources.add(new Project.Source(path, release)));
          return List.copyOf(sources);
        }
        var info = new Project.Source(infoDirectory, 0); // contains module-info.java file
        var java = infoDirectory.resolveSibling("java");
        if (java.equals(infoDirectory) || Files.notExists(java)) return List.of(info);
        return List.of(new Project.Source(java, 0), info);
      }
    }
  }
  public static class Sequencer {
    private final Project project;
    private final Tuner tuner;
    public Sequencer(Project project, Tuner tuner) {
      this.project = project;
      this.tuner = tuner;
    }
    public Task newBuildSequence() {
      var tasks = new ArrayList<Task>();
      tasks.add(new Task.ResolveMissingThirdPartyModules());
      for (var realm : project.realms()) {
        tasks.add(newJavacTask(realm));
        tasks.add(newJarTask(realm));
        if (realm.flags().contains(Project.Realm.Flag.CREATE_API_DOCUMENTATION))
          tasks.add(newJavadocTask(realm));
        if (realm.flags().contains(Project.Realm.Flag.CREATE_CUSTOM_RUNTIME_IMAGE))
          tasks.add(newJLinkTask(realm));
        if (realm.flags().contains(Project.Realm.Flag.LAUNCH_TESTS)) tasks.add(newTestsTask(realm));
      }
      return Task.sequence("Build Sequence", tasks);
    }
    Task newJavacTask(Project.Realm realm) {
      var tasks = new ArrayList<Task>();
      var tool = ToolProvider.findFirst("javac").orElseThrow();
      var classes = project.base().classes(realm.name());
      var arguments = Helper.newModuleArguments(project, realm).put("-d", classes);
      tuner.tune(arguments, project, Tuner.context("javac", realm));
      var args = arguments.toStringArray();
      tasks.add(new Task.RunTool("Compile modular sources of", tool, args));
      for (var unit : realm.units().values())
        if (unit.isMultiRelease()) tasks.add(newJavacTask(realm, unit));
      return Task.sequence("Compile sources of " + realm.toLabelName() + " realm", tasks);
    }
    Task newJavacTask(Project.Realm realm, Project.Unit unit) {
      var tasks = new ArrayList<Task>();
      for (var source : unit.sources()) tasks.add(newJavacTask(realm, unit, source));
      return Task.sequence("Compile multi-release unit " + unit.toName(), tasks);
    }
    Task newJavacTask(Project.Realm realm, Project.Unit unit, Project.Source source) {
      var module = unit.toName();
      var sourcePaths = List.of(unit.sources().get(0).path(), source.path());
      var arguments =
          new Arguments()
              .put("--release", source.release())
              .put("-d", project.base().classes(realm.name(), module, source.release()))
              .put("--source-path", new TreeSet<>(sourcePaths))
              .put("--class-path", List.of(project.base().classes(realm.name())));
      Paths.find(List.of(source.path()), 99, Paths::isJavaFile).forEach(arguments::add);
      tuner.tune(arguments, project, Tuner.context("javac", realm, module));
      var tool = ToolProvider.findFirst("javac").orElseThrow();
      var args = arguments.toStringArray();
      return new Task.RunTool("Compile sources targeted to " + source.release(), tool, args);
    }
    Task newJarTask(Project.Realm realm) {
      var tasks = new ArrayList<Task>();
      tasks.add(new Task.CreateDirectories(project.base().modules(realm.name())));
      tasks.add(new Task.CreateDirectories(project.base().sources(realm.name())));
      for (var unit : realm.units().values()) tasks.add(newJarTask(realm, unit));
      return Task.sequence("Create JAR files of " + realm.toLabelName() + " realm", tasks);
    }
    Task newJarTask(Project.Realm realm, Project.Unit unit) {
      var tasks = new ArrayList<Task>();
      var module = unit.toName();
      var tool = ToolProvider.findFirst("jar").orElseThrow();
      var base = project.base();
      {
        var file = Helper.jar(project, realm, module);
        var arguments = new Arguments().put("--create").put("--file", file);
        unit.descriptor().mainClass().ifPresent(main -> arguments.put("--main-class", main));
        if (unit.isMultiRelease()) {
          var sources = new ArrayDeque<>(unit.sources());
          var sources0 = sources.removeFirst();
          var classes0 = base.classes(realm.name(), module, sources0.release());
          arguments.add("-C", classes0, ".");
          if (Files.notExists(sources0.path().resolve("module-info.java"))) {
            for (var source : sources) {
              var classes = base.classes(realm.name(), module, source.release());
              if (Files.exists(source.path().resolve("module-info.java"))) {
                arguments.add("-C", classes, "module-info.class");
                break;
              }
            }
          }
          for (var source : sources) {
            arguments.add("--release", source.release());
            arguments.add("-C", base.classes(realm.name(), module, source.release()), ".");
          }
        } else arguments.add("-C", base.classes(realm.name(), module), ".");
        unit.resources().forEach(resource -> arguments.add("-C", resource, "."));
        tuner.tune(arguments, project, Tuner.context("jar", realm, module));
        var args = arguments.toStringArray();
        tasks.add(new Task.RunTool("Package classes of module " + module, tool, args));
      }
      {
        var version = project.info().version();
        var file = base.sources(realm.name()).resolve(module + "@" + version + "-sources.jar");
        var arguments = new Arguments().put("--create").put("--file", file).put("--no-manifest");
        var sources = new ArrayDeque<>(unit.sources());
        arguments.add("-C", sources.removeFirst().path(), "."); // API-defining "base" source
        for (var source : sources) {
          if (source.release() >= 9) arguments.add("--release", source.release());
          arguments.add("-C", source.path(), ".");
        }
        tuner.tune(arguments, project, Tuner.context("jar", realm, module));
        var args = arguments.toStringArray();
        tasks.add(new Task.RunTool("Package sources of module " + module, tool, args));
      }
      return Task.sequence("Create JAR files of " + realm.toLabelName() + " realm", tasks);
    }
    Task newJavadocTask(Project.Realm realm) {
      var arguments = Helper.newModuleArguments(project, realm).put("-d", project.base().api());
      tuner.tune(arguments, project, Tuner.context("javadoc", realm));
      return new Task.RunTool(
          "Generate API documentation for " + realm.toLabelName() + " realm",
          ToolProvider.findFirst("javadoc").orElseThrow(),
          arguments.toStringArray());
    }
    Task newJLinkTask(Project.Realm realm) {
      var base = project.base();
      var modulePaths = new ArrayList<Path>();
      modulePaths.add(base.modules(realm.name()));
      modulePaths.addAll(Helper.modulePaths(project, realm));
      var automaticModules =
          ModuleFinder.of(modulePaths.toArray(Path[]::new)).findAll().stream()
              .map(ModuleReference::descriptor)
              .filter(ModuleDescriptor::isAutomatic)
              .collect(Collectors.toList());
      if (!automaticModules.isEmpty()) return Task.sequence("Automatic module: " + automaticModules);
      var units = realm.units();
      var mainModule = Modules.findMainModule(units.values().stream().map(Project.Unit::descriptor));
      var arguments =
          new Arguments()
              .put("--add-modules", String.join(",", units.keySet()))
              .put("--module-path", modulePaths)
              .put("--output", base.image());
      if (mainModule.isPresent()) {
        var module = mainModule.get();
        var launcher = Path.of(module.replace('.', '/')).getFileName().toString();
        arguments.put("--launcher", launcher + '=' + module);
      }
      tuner.tune(arguments, project, Tuner.context("jlink", realm));
      return Task.sequence(
          "Create custom runtime image",
          new Task.DeleteDirectories(base.image()),
          new Task.RunTool(
              "jlink", ToolProvider.findFirst("jlink").orElseThrow(), arguments.toStringArray()));
    }
    Task newTestsTask(Project.Realm realm) {
      var base = project.base();
      var tasks = new ArrayList<Task>();
      for (var unit : realm.units().values()) {
        var module = unit.toName();
        var jar = Helper.jar(project, realm, module);
        var modulePaths = new ArrayList<Path>();
        modulePaths.add(jar);
        modulePaths.addAll(Helper.modulePaths(project, realm));
        modulePaths.add(base.modules(realm.name()));
        var arguments = new Arguments().put("--select-module", module);
        tuner.tune(arguments, project, Tuner.context("junit", realm, module));
        tasks.add(new Task.RunTestModule(module, modulePaths, arguments.toStringArray()));
      }
      return Task.sequence("Launch all tests located in " + realm.toLabelName() + " realm", tasks);
    }
    public static class Arguments {
      private final Map<String, List<Object>> namedOptions = new LinkedHashMap<>();
      private final List<Object> additionalArguments = new ArrayList<>();
      public Arguments add(Object... arguments) {
        this.additionalArguments.addAll(List.of(arguments));
        return this;
      }
      public Arguments put(String option, Object... values) {
        namedOptions.put(option, List.of(values));
        return this;
      }
      public Arguments put(String option, Collection<Path> paths) {
        return put(option, "", paths);
      }
      public Arguments put(String option, String prefix, Collection<Path> paths) {
        var path = paths.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator));
        namedOptions.put(option, List.of(prefix + path));
        return this;
      }
      public String[] toStringArray() {
        var list = new ArrayList<String>();
        for (var entry : namedOptions.entrySet()) {
          list.add(entry.getKey());
          for (var value : entry.getValue()) list.add(value.toString());
        }
        for (var additional : additionalArguments) list.add(additional.toString());
        return list.toArray(String[]::new);
      }
    }
    public interface Helper {
      static Arguments newModuleArguments(Project project, Project.Realm realm) {
        var arguments = new Arguments().put("--module", String.join(",", realm.units().keySet()));
        var modulePaths = Helper.modulePaths(project, realm);
        if (!modulePaths.isEmpty()) arguments.put("--module-path", modulePaths);
        Helper.putModuleSourcePaths(arguments, realm);
        Helper.putModulePatches(arguments, project, realm);
        if (realm.flags().contains(Project.Realm.Flag.ENABLE_PREVIEW_LANGUAGE_FEATURES)) {
          arguments.put("--enable-preview");
          arguments.put("--release", Runtime.version().feature());
        }
        return arguments;
      }
      static List<Path> modulePaths(Project project, Project.Realm realm) {
        var base = project.base();
        var lib = base.lib();
        var paths = new ArrayList<Path>();
        for (var upstream : realm.upstreams()) paths.add(base.modules(upstream));
        if (Files.isDirectory(lib) || !project.toExternalModuleNames().isEmpty()) paths.add(lib);
        return List.copyOf(paths);
      }
      static Path jar(Project project, Project.Realm realm, String module) {
        var modules = project.base().modules(realm.name());
        return modules.resolve(module + "@" + project.info().version() + ".jar");
      }
      static List<Path> relevantSourcePaths(Project.Unit unit) {
        var sources = unit.sources();
        var p0 = sources.get(0).path();
        if (sources.size() == 1 || Files.exists(p0.resolve("module-info.java"))) return List.of(p0);
        for (var source : sources) {
          var pN = source.path();
          if (Files.exists(pN.resolve("module-info.java"))) return List.of(p0, pN);
        }
        throw new IllegalStateException("No module-info.java found in: " + sources);
      }
      static void putModuleSourcePaths(Arguments arguments, Project.Realm realm) {
        var patterns = new TreeSet<String>(); // "src:etc/*/java"
        var specific = new TreeMap<String, List<Path>>(); // "foo=java:java-9"
        for (var unit : realm.units().values()) {
          var sourcePaths = Helper.relevantSourcePaths(unit);
          try {
            for (var path : sourcePaths) patterns.add(Modules.modulePatternForm(path, unit.toName()));
          } catch (FindException e) {
            specific.put(unit.toName(), sourcePaths);
          }
        }
        if (!patterns.isEmpty())
          arguments.put("--module-source-path", String.join(File.pathSeparator, patterns));
        if (specific.isEmpty()) return;
        for (var entry : specific.entrySet())
          arguments.put("--module-source-path", entry.getKey() + "=", entry.getValue());
      }
      static Map<String, List<Path>> patches(
          Collection<Project.Unit> units, List<Project.Realm> upstreams) {
        if (units.isEmpty() || upstreams.isEmpty()) return Map.of();
        var patches = new TreeMap<String, List<Path>>();
        for (var unit : units) {
          var module = unit.toName();
          for (var upstream : upstreams)
            upstream.units().values().stream()
                .filter(up -> up.toName().equals(module))
                .findAny()
                .ifPresent(up -> patches.put(module, List.of(up.sources().get(0).path())));
        }
        return patches;
      }
      static void putModulePatches(Arguments arguments, Project project, Project.Realm realm) {
        var upstreams = new ArrayList<>(project.realms());
        upstreams.removeIf(candidate -> !realm.upstreams().contains(candidate.name()));
        var patches = patches(realm.units().values(), upstreams);
        if (patches.isEmpty()) return;
        for (var patch : patches.entrySet())
          arguments.put("--patch-module", patch.getKey() + "=", patch.getValue());
      }
    }
    public interface Tuner {
      static Map<String, String> context(String tool, Project.Realm realm) {
        return Map.of("tool", tool, "realm", realm.name());
      }
      static Map<String, String> context(String tool, Project.Realm realm, String module) {
        return Map.of("tool", tool, "realm", realm.name(), "module", module);
      }
      void tune(Arguments arguments, Project project, Map<String, String> context);
      static void defaults(Arguments arguments, Project project, Map<String, String> context) {
        switch (context.get("tool")) {
          case "javac":
            arguments.put("-encoding", "UTF-8");
            arguments.put("-parameters");
            arguments.put("-Werror");
            arguments.put("-X" + "lint");
            break;
          case "javadoc":
            arguments.put("-encoding", "UTF-8");
            arguments.put("-locale", "en");
            break;
          case "jlink":
            arguments.put("--compress", "2");
            arguments.put("--no-header-files");
            arguments.put("--no-man-pages");
            arguments.put("--strip-debug");
            break;
          case "junit":
            var module = context.get("module");
            var target = project.base().workspace("junit-reports", module);
            arguments.put("--disable-ansi-colors");
            arguments.put("--reports-dir", target);
            break;
        }
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
      private final String[] junitArguments;
      public RunTestModule(String module, List<Path> modulePaths, String[] junitArguments) {
        super("Run tests for module " + module, List.of());
        this.module = module;
        this.modulePaths = modulePaths;
        this.junitArguments = junitArguments;
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
          bach.execute(tool, new PrintWriter(getOut()), new PrintWriter(getErr()), junitArguments);
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
    public static class ResolveMissingThirdPartyModules extends Task {
      public ResolveMissingThirdPartyModules() {
        super("Resolve missing 3rd-party modules", List.of());
      }
      public void execute(Bach bach) {
        var project = bach.getProject();
        var library = project.library();
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
        var modulePaths = List.of(project.base().lib());
        var declared = project.toDeclaredModuleNames();
        var resolver = new ModulesResolver(modulePaths, declared, new Transporter());
        resolver.resolve(project.toRequiredModuleNames());
        resolver.resolve(library.required());
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
      var deque = new ArrayDeque<String>();
      for (var element : info.normalize()) {
        var name = element.toString();
        if (name.equals("module-info.java")) continue;
        deque.addLast(name.equals(module) ? "*" : name);
      }
      var pattern = String.join(File.separator, deque);
      if (!pattern.contains("*")) throw new FindException("Name '" + module + "' not found: " + info);
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
    public static String platform(String linux, String mac, String windows) {
      var os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
      return os.contains("win") ? windows : os.contains("mac") ? mac : linux;
    }
    public ModulesMap() {
      put(
          "javafx.base",
          platform(
              "https://repo.maven.apache.org/maven2/org/openjfx/javafx-base/14.0.1/javafx-base-14.0.1-linux.jar",
              "https://repo.maven.apache.org/maven2/org/openjfx/javafx-base/14.0.1/javafx-base-14.0.1-mac.jar",
              "https://repo.maven.apache.org/maven2/org/openjfx/javafx-base/14.0.1/javafx-base-14.0.1-win.jar"));
      put(
          "javafx.controls",
          platform(
              "https://repo.maven.apache.org/maven2/org/openjfx/javafx-controls/14.0.1/javafx-controls-14.0.1-linux.jar",
              "https://repo.maven.apache.org/maven2/org/openjfx/javafx-controls/14.0.1/javafx-controls-14.0.1-mac.jar",
              "https://repo.maven.apache.org/maven2/org/openjfx/javafx-controls/14.0.1/javafx-controls-14.0.1-win.jar"));
      put(
          "javafx.fxml",
          platform(
              "https://repo.maven.apache.org/maven2/org/openjfx/javafx-fxml/14.0.1/javafx-fxml-14.0.1-linux.jar",
              "https://repo.maven.apache.org/maven2/org/openjfx/javafx-fxml/14.0.1/javafx-fxml-14.0.1-mac.jar",
              "https://repo.maven.apache.org/maven2/org/openjfx/javafx-fxml/14.0.1/javafx-fxml-14.0.1-win.jar"));
      put(
          "javafx.graphics",
          platform(
              "https://repo.maven.apache.org/maven2/org/openjfx/javafx-graphics/14.0.1/javafx-graphics-14.0.1-linux.jar",
              "https://repo.maven.apache.org/maven2/org/openjfx/javafx-graphics/14.0.1/javafx-graphics-14.0.1-mac.jar",
              "https://repo.maven.apache.org/maven2/org/openjfx/javafx-graphics/14.0.1/javafx-graphics-14.0.1-win.jar"));
      put(
          "javafx.media",
          platform(
              "https://repo.maven.apache.org/maven2/org/openjfx/javafx-media/14.0.1/javafx-media-14.0.1-linux.jar",
              "https://repo.maven.apache.org/maven2/org/openjfx/javafx-media/14.0.1/javafx-media-14.0.1-mac.jar",
              "https://repo.maven.apache.org/maven2/org/openjfx/javafx-media/14.0.1/javafx-media-14.0.1-win.jar"));
      put(
          "javafx.swing",
          platform(
              "https://repo.maven.apache.org/maven2/org/openjfx/javafx-swing/14.0.1/javafx-swing-14.0.1-linux.jar",
              "https://repo.maven.apache.org/maven2/org/openjfx/javafx-swing/14.0.1/javafx-swing-14.0.1-mac.jar",
              "https://repo.maven.apache.org/maven2/org/openjfx/javafx-swing/14.0.1/javafx-swing-14.0.1-win.jar"));
      put(
          "javafx.web",
          platform(
              "https://repo.maven.apache.org/maven2/org/openjfx/javafx-web/14.0.1/javafx-web-14.0.1-linux.jar",
              "https://repo.maven.apache.org/maven2/org/openjfx/javafx-web/14.0.1/javafx-web-14.0.1-mac.jar",
              "https://repo.maven.apache.org/maven2/org/openjfx/javafx-web/14.0.1/javafx-web-14.0.1-win.jar"));
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
      this.paths = Objects.requireNonNull(paths, "paths").toArray(Path[]::new);
      this.declared = new TreeSet<>(Objects.requireNonNull(declared, "declared"));
      this.transporter = Objects.requireNonNull(transporter, "transporter");
      this.system = Modules.declared(ModuleFinder.ofSystem());
      if (paths.isEmpty()) throw new IllegalArgumentException("At least one path expected");
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
    public static Deque<String> deque(Path path) {
      var deque = new ArrayDeque<String>();
      path.forEach(name -> deque.addFirst(name.toString()));
      return deque;
    }
    public static boolean isRoot(Path path) {
      return path.toAbsolutePath().normalize().getNameCount() == 0;
    }
    public static final Pattern JAVA_N_PATTERN = Pattern.compile("java.?(\\d+)");
    public static Optional<Integer> findMultiReleaseNumber(Path path) {
      var matcher = JAVA_N_PATTERN.matcher(name(path));
      if (!matcher.matches()) return Optional.empty();
      return Optional.of(Integer.parseInt(matcher.group(1)));
    }
    public static boolean isMultiReleaseDirectory(Path path) {
      return Files.isDirectory(path) && JAVA_N_PATTERN.matcher(name(path)).matches();
    }
    public static String name(Path path) {
      return path.getNameCount() == 0 ? "" : path.getFileName().toString();
    }
    public static boolean isJavaFile(Path path) {
      return Files.isRegularFile(path) && name(path).endsWith(".java");
    }
    public static boolean isModuleInfoJavaFile(Path path) {
      return Files.isRegularFile(path) && name(path).equals("module-info.java");
    }
    public static List<Path> find(Collection<Path> roots, int maxDepth, Predicate<Path> filter) {
      var paths = new TreeSet<Path>();
      for (var root : roots) {
        try (var stream = Files.walk(root, maxDepth)) {
          stream.filter(filter).forEach(paths::add);
        } catch (Exception e) {
          throw new Error("Walk directory '" + root + "' failed: " + e, e);
        }
      }
      return List.copyOf(paths);
    }
    public static List<Path> list(Path directory, DirectoryStream.Filter<? super Path> filter) {
      var paths = new TreeSet<>(Comparator.comparing(Path::toString));
      try (var directoryStream = Files.newDirectoryStream(directory, filter)) {
        directoryStream.forEach(paths::add);
      } catch (Exception e) {
        throw new Error("Stream directory '" + directory + "' failed: " + e, e);
      }
      return List.copyOf(paths);
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
