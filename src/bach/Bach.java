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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.System.Logger.Level;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
public class Bach {
  public static final Version VERSION = Version.parse("11.0-ea");
  public static void main(String... args) {
    Main.main(args);
  }
  private final Consumer<String> printer;
  private final boolean verbose;
  public Bach() {
    this(
        System.out::println,
        Boolean.getBoolean("verbose") // -D verbose=true
            || Boolean.getBoolean("ebug") // -Debug=true
            || "".equals(System.getProperty("ebug")));
  }
  public Bach(Consumer<String> printer, boolean verbose) {
    this.printer = printer;
    this.verbose = verbose;
    print(Level.TRACE, "Bach initialized");
  }
  public boolean verbose() {
    return verbose;
  }
  Consumer<String> printer() {
    return printer;
  }
  public String print(String format, Object... args) {
    return print(Level.INFO, format, args);
  }
  public String print(Level level, String format, Object... args) {
    var message = String.format(format, args);
    if (verbose() || level.getSeverity() >= Level.INFO.getSeverity()) printer().accept(message);
    return message;
  }
  public Summary build(Consumer<Project.Builder> projectBuilderConsumer) {
    return build(project(projectBuilderConsumer));
  }
  public Summary build(Project project) {
    return build(project, new BuildTaskGenerator(project, verbose()));
  }
  Summary build(Project project, Supplier<Task> taskSupplier) {
    var start = Instant.now();
    print("Build %s", project.name());
    var summary = new Summary(project);
    execute(taskSupplier.get(), summary);
    var markdown = summary.write();
    var duration =
        Duration.between(start, Instant.now())
            .truncatedTo(TimeUnit.MILLISECONDS.toChronoUnit())
            .toString()
            .substring(2)
            .replaceAll("(\\d[HMS])(?!$)", "$1 ")
            .toLowerCase();
    print("Build took %s -> %s", duration, markdown.toUri());
    return summary;
  }
  void execute(Task task, Summary summary) {
    var title = task.title();
    var children = task.children();
    print(Level.DEBUG, "%c %s", children.isEmpty() ? '*' : '+', title);
    summary.executionBegin(task);
    var result = task.execute(new ExecutionContext(this));
    if (verbose()) {
      result.out().lines().forEach(printer());
      result.err().lines().forEach(printer());
    }
    if (result.code() != 0) {
      result.err().lines().forEach(printer);
      summary.executionEnd(task, result);
      var message = title + ": non-zero result code: " + result.code();
      throw new RuntimeException(message);
    }
    if (!children.isEmpty()) {
      try {
        var tasks = task.parallel() ? children.parallelStream() : children.stream();
        tasks.forEach(child -> execute(child, summary));
      } catch (RuntimeException e) {
        summary.addSuppressed(e);
      }
      print(Level.DEBUG, "= %s", title);
    }
    summary.executionEnd(task, result);
  }
  Project project(Consumer<Project.Builder> projectBuilderConsumer) {
    var projectBuilder = Project.builder();
    projectBuilderConsumer.accept(projectBuilder);
    return projectBuilder.build();
  }
  interface Convention {
    static Optional<String> mainClass(Path info, String module) {
      var main = Path.of(module.replace('.', '/'), "Main.java");
      var exists = Files.isRegularFile(info.resolveSibling(main));
      return exists ? Optional.of(module + '.' + "Main") : Optional.empty();
    }
    static void amendJUnitTestEngines(Map<String, Version> modules) {
      var names = modules.keySet();
      if (names.contains("org.junit.jupiter") || names.contains("org.junit.jupiter.api"))
        modules.putIfAbsent("org.junit.jupiter.engine", null);
      if (names.contains("junit")) modules.putIfAbsent("org.junit.vintage.engine", null);
    }
    static void amendJUnitPlatformConsole(Map<String, Version> modules) {
      var names = modules.keySet();
      if (names.contains("org.junit.platform.console")) return;
      var triggers =
          Set.of("org.junit.jupiter.engine", "org.junit.vintage.engine", "org.junit.platform.engine");
      names.stream()
          .filter(triggers::contains)
          .findAny()
          .ifPresent(__ -> modules.put("org.junit.platform.console", null));
    }
  }
  public static final class Paths {
    private static final Path CLASSES = Path.of("classes");
    private static final Path MODULES = Path.of("modules");
    private static final Path SOURCES = Path.of("sources");
    private static final Path DOCUMENTATION = Path.of("documentation");
    private static final Path JAVADOC = DOCUMENTATION.resolve("javadoc");
    public static Paths of(Path base) {
      return new Paths(base, base.resolve(".bach"), base.resolve("lib"));
    }
    private final Path base;
    private final Path out;
    private final Path lib;
    public Paths(Path base, Path out, Path lib) {
      this.base = base;
      this.out = out;
      this.lib = lib;
    }
    public Path base() {
      return base;
    }
    public Path out() {
      return out;
    }
    public Path lib() {
      return lib;
    }
    public Path out(String first, String... more) {
      var path = Path.of(first, more);
      return out.resolve(path);
    }
    public Path classes(Realm realm) {
      return out.resolve(CLASSES).resolve(realm.name());
    }
    public Path javadoc() {
      return out.resolve(JAVADOC);
    }
    public Path modules(Realm realm) {
      return out.resolve(MODULES).resolve(realm.name());
    }
    public Path sources(Realm realm) {
      return out.resolve(SOURCES).resolve(realm.name());
    }
  }
  public static final class Project {
    public static Builder builder() {
      return new Builder();
    }
    private final String name;
    private final Version version;
    private final Structure structure;
    private Project(String name, Version version, Structure structure) {
      this.name = Objects.requireNonNull(name, "name");
      this.version = version;
      this.structure = Objects.requireNonNull(structure, "structure");
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
    public Paths paths() {
      return structure().paths();
    }
    public Tuner tuner() {
      return structure().tuner();
    }
    public String toNameAndVersion() {
      if (version == null) return name;
      return name + ' ' + version;
    }
    public String toJarName(Unit unit, String classifier) {
      var unitVersion = unit.descriptor().version();
      var version = unitVersion.isPresent() ? unitVersion : Optional.ofNullable(this.version);
      var versionSuffix = version.map(v -> "-" + v).orElse("");
      var classifierSuffix = classifier.isEmpty() ? "" : "-" + classifier;
      return unit.name() + versionSuffix + classifierSuffix + ".jar";
    }
    public Path toModularJar(Realm realm, Unit unit) {
      return paths().modules(realm).resolve(toJarName(unit, ""));
    }
    public static class Builder {
      private String name;
      private Version version;
      private Paths paths;
      private List<Unit> units;
      private List<Realm> realms;
      private Tuner tuner;
      private Builder() {
        name(null);
        version((Version) null);
        paths("");
        units(List.of());
        realms(List.of());
        tuner(new Tuner());
      }
      public Project build() {
        var structure = new Structure(paths, units, realms, tuner);
        return new Project(name, version, structure);
      }
      public Builder name(String name) {
        this.name = name;
        return this;
      }
      public Builder version(Version version) {
        this.version = version;
        return this;
      }
      public Builder version(String version) {
        return version(Version.parse(version));
      }
      public Builder paths(Paths paths) {
        this.paths = paths;
        return this;
      }
      public Builder paths(Path base) {
        return paths(Paths.of(base));
      }
      public Builder paths(String base) {
        return paths(Path.of(base));
      }
      public Builder units(List<Unit> units) {
        this.units = units;
        return this;
      }
      public Builder realms(List<Realm> realms) {
        this.realms = realms;
        return this;
      }
      public Builder tuner(Tuner tuner) {
        this.tuner = tuner;
        return this;
      }
    }
  }
  public static final class Realm {
    public enum Flag {
      ENABLE_PREVIEW,
      CREATE_JAVADOC,
      LAUNCH_TESTS
    }
    private final String name;
    private final int feature;
    private final List<Unit> units;
    private final List<Realm> requires;
    private final Set<Flag> flags;
    public Realm(
        String name, int feature, List<Unit> units, List<Realm> requires, Flag... flags) {
      this.name = Objects.requireNonNull(name, "name");
      this.feature = Objects.checkIndex(feature, Runtime.version().feature() + 1);
      this.units = List.copyOf(units);
      this.requires = List.copyOf(requires);
      this.flags = flags.length == 0 ? Set.of() : EnumSet.copyOf(Set.of(flags));
    }
    public String name() {
      return name;
    }
    public int feature() {
      return feature;
    }
    public List<Unit> units() {
      return units;
    }
    public List<Realm> requires() {
      return requires;
    }
    public Set<Flag> flags() {
      return flags;
    }
    public String title() {
      return name.isEmpty() ? "default" : name;
    }
    public OptionalInt release() {
      return feature == 0 ? OptionalInt.empty() : OptionalInt.of(feature);
    }
    public Optional<Unit> unit(String name) {
      return units.stream().filter(unit -> unit.name().equals(name)).findAny();
    }
    public List<String> moduleNames() {
      return units.stream().map(Unit::name).collect(Collectors.toList());
    }
    public List<Path> moduleSourcePaths() {
      return units.stream()
          .map(Unit::moduleSourcePath)
          .distinct()
          .collect(Collectors.toList());
    }
    public List<Path> modulePaths(Paths paths) {
      var list = new ArrayList<Path>();
      requires.stream().map(paths::modules).forEach(list::add);
      list.add(paths.lib());
      return list;
    }
    public Map<String, List<Path>> patches(BiFunction<Realm, Unit, List<Path>> patcher) {
      if (units.isEmpty() || requires.isEmpty()) return Map.of();
      var patches = new TreeMap<String, List<Path>>();
      for (var unit : units()) {
        var module = unit.name();
        for (var required : requires) {
          var other = required.unit(module);
          if (other.isEmpty()) continue;
          var paths = patcher.apply(required, other.orElseThrow());
          if (paths.isEmpty()) continue;
          patches.put(module, paths);
        }
      }
      return patches;
    }
  }
  public static final class Source {
    public enum Flag {
      VERSIONED
    }
    public static Source of(Path path, Flag... flags) {
      return new Source(path, 0, Set.of(flags));
    }
    private final Path path;
    private final int release;
    private final Set<Flag> flags;
    public Source(Path path, int release, Set<Flag> flags) {
      this.path = Objects.requireNonNull(path, "path");
      this.release = release;
      this.flags = flags.isEmpty() ? Set.of() : EnumSet.copyOf(flags);
    }
    public Path path() {
      return path;
    }
    public int release() {
      return release;
    }
    public Set<Flag> flags() {
      return flags;
    }
    public boolean isVersioned() {
      return flags.contains(Flag.VERSIONED);
    }
    public boolean isTargeted() {
      return release != 0;
    }
    public OptionalInt target() {
      return isTargeted() ? OptionalInt.of(release) : OptionalInt.empty();
    }
  }
  public static final class Structure {
    private final Paths paths;
    private final List<Unit> units;
    private final List<Realm> realms;
    private final Tuner tuner;
    public Structure(Paths paths, List<Unit> units, List<Realm> realms, Tuner tuner) {
      this.paths = paths;
      this.units = units;
      this.realms = realms;
      this.tuner = tuner;
    }
    public Paths paths() {
      return paths;
    }
    public List<Unit> units() {
      return units;
    }
    public List<Realm> realms() {
      return realms;
    }
    public Tuner tuner() {
      return tuner;
    }
  }
  public interface Tool {
    static Any of(String name, Object... arguments) {
      return new Any(name, arguments);
    }
    static JavaCompiler javac() {
      return new JavaCompiler();
    }
    static String join(Collection<Path> paths) {
      return paths.stream()
          .map(Path::toString)
          .map(string -> string.replace("{MODULE}", "*"))
          .collect(Collectors.joining(File.pathSeparator));
    }
    String name();
    String[] toStrings();
    class Any implements Tool {
      private final String name;
      private final List<String> args = new ArrayList<>();
      private Any(String name, Object... arguments) {
        this.name = name;
        addAll(arguments);
      }
      @Override
      public String name() {
        return name;
      }
      public Any add(Object argument) {
        args.add(argument.toString());
        return this;
      }
      public Any add(String key, Object value) {
        return add(key).add(value);
      }
      public Any add(String key, Object first, Object second) {
        return add(key).add(first).add(second);
      }
      public Any add(boolean predicate, Object first, Object... more) {
        return predicate ? add(first).addAll(more) : this;
      }
      public Any addAll(Object... arguments) {
        for (var argument : arguments) add(argument);
        return this;
      }
      public <T> Any forEach(Iterable<T> iterable, BiConsumer<Any, T> visitor) {
        iterable.forEach(argument -> visitor.accept(this, argument));
        return this;
      }
      public String[] toStrings() {
        return args.toArray(String[]::new);
      }
    }
    class JavaCompiler implements Tool {
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
      private JavaCompiler() {}
      @Override
      public String name() {
        return "javac";
      }
      @Override
      public String[] toStrings() {
        var args = new ArrayList<String>();
        if (isAssigned(getCompileModulesCheckingTimestamps())) {
          args.add("--module");
          args.add(String.join(",", getCompileModulesCheckingTimestamps()));
        }
        if (isAssigned(getVersionOfModulesThatAreBeingCompiled())) {
          args.add("--module-version");
          args.add(String.valueOf(getVersionOfModulesThatAreBeingCompiled()));
        }
        if (isAssigned(getPathsWhereToFindSourceFilesForModules())) {
          args.add("--module-source-path");
          args.add(join(getPathsWhereToFindSourceFilesForModules()));
        }
        if (isAssigned(getPathsWhereToFindApplicationModules())) {
          args.add("--module-path");
          args.add(join(getPathsWhereToFindApplicationModules()));
        }
        if (isAssigned(getPathsWhereToFindMoreAssetsPerModule())) {
          for(var patch : getPathsWhereToFindMoreAssetsPerModule().entrySet()) {
            args.add("--patch-module");
            args.add(patch.getKey() + '=' + join(patch.getValue()));
          }
        }
        if (isAssigned(getCompileForVirtualMachineVersion())) {
          args.add("--release");
          args.add(String.valueOf(getCompileForVirtualMachineVersion()));
        }
        if (isEnablePreviewLanguageFeatures()) args.add("--enable-preview");
        if (isGenerateMetadataForMethodParameters()) args.add("-parameters");
        if (isOutputSourceLocationsOfDeprecatedUsages()) args.add("-deprecation");
        if (isOutputMessagesAboutWhatTheCompilerIsDoing()) args.add("-verbose");
        if (isTerminateCompilationIfWarningsOccur()) args.add("-Werror");
        if (isAssigned(getCharacterEncodingUsedBySourceFiles())) {
          args.add("-encoding");
          args.add(getCharacterEncodingUsedBySourceFiles());
        }
        if (isAssigned(getDestinationDirectory())) {
          args.add("-d");
          args.add(String.valueOf(getDestinationDirectory()));
        }
        return args.toArray(String[]::new);
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
    private static boolean isAssigned(Object object) {
      if (object == null) return false;
      if (object instanceof Number) return ((Number) object).intValue() != 0;
      if (object instanceof Optional) return ((Optional<?>) object).isPresent();
      if (object instanceof Collection) return !((Collection<?>) object).isEmpty();
      return true;
    }
  }
  public static class Tuner {
    public void tune(Tool.Any any, Project project, Realm realm, Unit unit) {}
    public void tune(Tool.JavaCompiler javac, Project project, Realm realm) {}
  }
  public static final class Unit {
    private final Path info;
    private final ModuleDescriptor descriptor;
    private final Path moduleSourcePath;
    private final List<Source> sources;
    private final List<Path> resources;
    public Unit(
        Path info,
        ModuleDescriptor descriptor,
        Path moduleSourcePath,
        List<Source> sources,
        List<Path> resources) {
      this.info = Objects.requireNonNull(info, "info");
      this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
      this.moduleSourcePath = Objects.requireNonNull(moduleSourcePath, "moduleSourcePath");
      this.sources = List.copyOf(sources);
      this.resources = List.copyOf(resources);
    }
    public Path info() {
      return info;
    }
    public ModuleDescriptor descriptor() {
      return descriptor;
    }
    public Path moduleSourcePath() {
      return moduleSourcePath;
    }
    public List<Source> sources() {
      return sources;
    }
    public List<Path> resources() {
      return resources;
    }
    public String name() {
      return descriptor.name();
    }
    public boolean isMainClassPresent() {
      return descriptor.mainClass().isPresent();
    }
    public <T> List<T> sources(Function<Source, T> mapper) {
      if (sources.isEmpty()) return List.of();
      if (sources.size() == 1) return List.of(mapper.apply(sources.get(0)));
      return sources.stream().map(mapper).collect(Collectors.toList());
    }
    public boolean isMultiRelease() {
      if (sources.isEmpty()) return false;
      if (sources.size() == 1) return sources.get(0).isTargeted();
      return sources.stream().allMatch(Source::isTargeted);
    }
  }
  public static class BuildTaskGenerator implements Supplier<Task> {
    public static Task parallel(String title, Task... tasks) {
      return new Task(title, true, List.of(tasks));
    }
    public static Task sequence(String title, Task... tasks) {
      return new Task(title, false, List.of(tasks));
    }
    public static Task run(Tool tool) {
      return run(tool.name(), tool.toStrings());
    }
    public static Task run(String name, String... args) {
      var provider = ToolProvider.findFirst(name).orElseThrow();
      return new Tasks.RunToolProvider(provider, args);
    }
    private final Project project;
    private final boolean verbose;
    public BuildTaskGenerator(Project project, boolean verbose) {
      this.project = project;
      this.verbose = verbose;
    }
    public Project project() {
      return project;
    }
    public boolean verbose() {
      return verbose;
    }
    @Override
    public Task get() {
      return sequence(
          "Build " + project().toNameAndVersion(),
          createDirectories(project.paths().out()),
          printVersionInformationOfFoundationTools(),
          resolveMissingModules(),
          parallel(
              "Compile realms and generate API documentation",
              compileApiDocumentation(),
              compileAllRealms()),
          launchAllTests());
    }
    protected Task createDirectories(Path path) {
      return new Tasks.CreateDirectories(path);
    }
    protected Task printVersionInformationOfFoundationTools() {
      return verbose()
          ? parallel(
              "Print version of various foundation tools",
              run("javac", "--version"),
              run("javadoc", "--version"),
              run("jar", "--version"))
          : sequence("Print version of javac", run("javac", "--version"));
    }
    protected Task resolveMissingModules() {
      return sequence("Resolve missing modules");
    }
    protected Task compileAllRealms() {
      var realms = project.structure().realms();
      if (realms.isEmpty()) return sequence("Cannot compile modules: 0 realms declared");
      var tasks = realms.stream().map(this::compileRealm);
      return sequence("Compile all realms", tasks.toArray(Task[]::new));
    }
    protected Task compileRealm(Realm realm) {
      if (realm.units().isEmpty()) return sequence("No units in " + realm.title() + " realm?!");
      var paths = project.paths();
      var enablePreview = realm.flags().contains(Realm.Flag.ENABLE_PREVIEW);
      var release = enablePreview ? OptionalInt.of(Runtime.version().feature()) : realm.release();
      var patches = realm.patches((other, unit) -> List.of(project.toModularJar(other, unit)));
      var javac =
          Tool.javac()
              .setCompileModulesCheckingTimestamps(realm.moduleNames())
              .setVersionOfModulesThatAreBeingCompiled(project.version())
              .setPathsWhereToFindSourceFilesForModules(realm.moduleSourcePaths())
              .setPathsWhereToFindApplicationModules(realm.modulePaths(paths))
              .setPathsWhereToFindMoreAssetsPerModule(patches)
              .setEnablePreviewLanguageFeatures(enablePreview)
              .setCompileForVirtualMachineVersion(release.orElse(0))
              .setCharacterEncodingUsedBySourceFiles("UTF-8")
              .setOutputMessagesAboutWhatTheCompilerIsDoing(false)
              .setGenerateMetadataForMethodParameters(true)
              .setOutputSourceLocationsOfDeprecatedUsages(true)
              .setTerminateCompilationIfWarningsOccur(true)
              .setDestinationDirectory(paths.classes(realm));
      project.tuner().tune(javac, project, realm);
      return sequence("Compile " + realm.title() + " realm", run(javac), packageRealm(realm));
    }
    protected Task packageRealm(Realm realm) {
      var jars = new ArrayList<Task>();
      for (var unit : realm.units()) {
        jars.add(packageUnitModule(realm, unit));
        jars.add(packageUnitSources(realm, unit));
      }
      return sequence(
          "Package " + realm.title() + " modules and sources",
          createDirectories(project.paths().modules(realm)),
          createDirectories(project.paths().sources(realm)),
          parallel("Jar each " + realm.title() + " module", jars.toArray(Task[]::new)));
    }
    protected Task packageUnitModule(Realm realm, Unit unit) {
      var paths = project.paths();
      var module = unit.name();
      var classes = paths.classes(realm).resolve(module);
      var jar =
          Tool.of("jar")
              .add("--create")
              .add("--file", project.toModularJar(realm, unit))
              .add(verbose, "--verbose")
              .add("-C", classes, ".")
              .forEach(
                  realm.requires(),
                  (args, other) -> {
                    var patched = other.unit(module).isPresent();
                    var path = paths.classes(other).resolve(module);
                    args.add(patched, "-C", path, ".");
                  })
              .forEach(unit.resources(), (any, path) -> any.add("-C", path, "."));
      project.tuner().tune(jar, project, realm, unit);
      return run(jar);
    }
    protected Task packageUnitSources(Realm realm, Unit unit) {
      var sources = project.paths().sources(realm);
      var jar =
          Tool.of("jar")
              .add("--create")
              .add("--file", sources.resolve(project.toJarName(unit, "sources")))
              .add(verbose, "--verbose")
              .add("--no-manifest")
              .forEach(unit.sources(Source::path), (any, path) -> any.add("-C", path, "."))
              .forEach(unit.resources(), (any, path) -> any.add("-C", path, "."));
      project.tuner().tune(jar, project, realm, unit);
      return run(jar);
    }
    protected Task compileApiDocumentation() {
      return sequence("Compile API documentation");
    }
    protected Task launchAllTests() {
      return sequence("Launch all tests");
    }
  }
  public static final class ExecutionContext {
    private final Bach bach;
    private final Instant start;
    private final StringWriter out;
    private final StringWriter err;
    public ExecutionContext(Bach bach) {
      this.bach = bach;
      this.start = Instant.now();
      this.out = new StringWriter();
      this.err = new StringWriter();
    }
    public Bach bach() {
      return bach;
    }
    public Instant start() {
      return start;
    }
    public void print(Level level, String format, Object... args) {
      if (bach().verbose() || level.getSeverity() >= Level.INFO.getSeverity()) {
        var writer = level.getSeverity() <= Level.INFO.getSeverity() ? out : err;
        writer.write(String.format(format, args));
        writer.write(System.lineSeparator());
      }
    }
    public ExecutionResult ok() {
      var duration = Duration.between(start(), Instant.now());
      return new ExecutionResult(0, duration, out.toString(), err.toString(), null);
    }
    public ExecutionResult failed(Throwable throwable) {
      var duration = Duration.between(start(), Instant.now());
      return new ExecutionResult(1, duration, out.toString(), err.toString(), throwable);
    }
  }
  public static final class ExecutionResult {
    private final int code;
    private final Duration duration;
    private final String out;
    private final String err;
    private final Throwable throwable;
    public ExecutionResult(int code, Duration duration, String out, String err, Throwable throwable) {
      this.code = code;
      this.duration = duration;
      this.out = out;
      this.err = err;
      this.throwable = throwable;
    }
    public int code() {
      return code;
    }
    public Duration duration() {
      return duration;
    }
    public String out() {
      return out;
    }
    public String err() {
      return err;
    }
    public Throwable throwable() {
      return throwable;
    }
  }
  public static final class Snippet {
    interface Scribe {
      Snippet toSnippet();
      default String $(Object object) {
        if (object == null) return "null";
        return "\"" + object.toString() + "\"";
      }
      default String $(Path path) {
        return "Path.of(" + $(path.toString().replace('\\', '/')) + ")";
      }
      default String $(String[] strings) {
        if (strings.length == 0) return "";
        if (strings.length == 1) return $(strings[0]);
        if (strings.length == 2) return $(strings[0]) + ", " + $(strings[1]);
        var joiner = new StringJoiner(", ");
        for (var string : strings) joiner.add($(string));
        return joiner.toString();
      }
    }
    public static Snippet of(String... lines) {
      return new Snippet(Set.of(), List.of(lines));
    }
    public static Snippet of(Class<?> type, String... lines) {
      return new Snippet(Set.of(type), List.of(lines));
    }
    public static Snippet of(List<Snippet> snippets) {
      if (snippets.isEmpty()) return Snippet.of();
      if (snippets.size() == 1) return snippets.get(0);
      var types = new HashSet<Class<?>>();
      var lines = new ArrayList<String>();
      for(var snippet : snippets) {
        types.addAll(snippet.types());
        lines.addAll(snippet.lines());
      }
      return new Snippet(types, lines);
    }
    public static List<String> program(Task root) {
      var snippets = new ArrayList<Snippet>();
      root.walk(task -> snippets.add(task.toSnippet()));
      var snippet = of(snippets);
      var program = new ArrayList<String>();
      program.add("// Build program generated by Bach.java on " + Instant.now());
      snippet.types().stream()
          .map(Class::getCanonicalName)
          .sorted()
          .forEach(type -> program.add("import " + type + ";"));
      program.add("class Build {");
      program.add("  public static void main(String... args) {");
      snippet.lines().forEach(line -> program.add("    " + line));
      program.add("  }");
      program.add("  static void run(String name, String... args) {");
      program.add("    var tool = java.util.spi.ToolProvider.findFirst(name).orElseThrow();");
      program.add("    System.out.println('\\n' + name + ' ' + String.join(\" \", args));");
      program.add("    tool.run(System.out, System.err, args);");
      program.add("  }");
      program.add("}");
      return program;
    }
    private final Set<Class<?>> types;
    private final List<String> lines;
    public Snippet(Set<Class<?>> types, List<String> lines) {
      this.types = types;
      this.lines = lines;
    }
    public Set<Class<?>> types() {
      return types;
    }
    public List<String> lines() {
      return lines;
    }
  }
  public static class Task implements Snippet.Scribe {
    private final String title;
    private final boolean parallel;
    private final List<Task> children;
    public Task(String title, boolean parallel, List<Task> children) {
      this.title = Objects.requireNonNull(title, "title");
      this.parallel = parallel;
      this.children = List.copyOf(children);
    }
    public String title() {
      return title;
    }
    public boolean parallel() {
      return parallel;
    }
    public List<Task> children() {
      return children;
    }
    public ExecutionResult execute(ExecutionContext execution) {
      return execution.ok();
    }
    @Override
    public Snippet toSnippet() {
      return Snippet.of("// " + title);
    }
    void walk(Consumer<Task> consumer) {
      consumer.accept(this);
      for(var task : children()) task.walk(consumer);
    }
  }
  public interface Tasks {
    class CreateDirectories extends Task {
      private final Path path;
      public CreateDirectories(Path path) {
        super("Create directories " + path, false, List.of());
        this.path = path;
      }
      @Override
      public ExecutionResult execute(ExecutionContext context) {
        try {
          Files.createDirectories(path);
          return context.ok();
        } catch (Exception e) {
          return context.failed(e);
        }
      }
      @Override
      public Snippet toSnippet() {
        return Snippet.of(Files.class, "Files.createDirectories(" + $(path) + ");");
      }
    }
    class RunToolProvider extends Task {
      public interface GarbageCollect {}
      static String title(String tool, String... args) {
        var length = args.length;
        if (length == 0) return String.format("Run `%s`", tool);
        if (length == 1) return String.format("Run `%s %s`", tool, args[0]);
        if (length == 2) return String.format("Run `%s %s %s`", tool, args[0], args[1]);
        return String.format("Run `%s %s %s ...` (%d arguments)", tool, args[0], args[1], length);
      }
      private final ToolProvider[] tool;
      private final String name;
      private final String[] args;
      public RunToolProvider(ToolProvider tool, String... args) {
        super(title(tool.name(), args), false, List.of());
        this.tool = new ToolProvider[] {tool};
        this.name = tool.name();
        this.args = args;
      }
      @Override
      public ExecutionResult execute(ExecutionContext context) {
        var out = new StringWriter();
        var err = new StringWriter();
        var code = tool[0].run(new PrintWriter(out), new PrintWriter(err), args);
        var duration = Duration.between(context.start(), Instant.now());
        if (tool[0] instanceof GarbageCollect) {
          tool[0] = null;
          System.gc();
        }
        return new ExecutionResult(code, duration, out.toString(), err.toString(), null);
      }
      @Override
      public Snippet toSnippet() {
        return Snippet.of(String.format("run(%s, %s);", $(name), $(args)));
      }
    }
  }
  static class Main {
    public static void main(String... args) {
      System.out.println("Bach.java " + Bach.VERSION);
      new Bach().build(project -> project.name("project")).assertSuccessful();
    }
  }
  public static final class Summary {
    private final Project project;
    private final Deque<String> executions = new ConcurrentLinkedDeque<>();
    private final Deque<Detail> details = new ConcurrentLinkedDeque<>();
    private final Deque<Throwable> suppressed = new ConcurrentLinkedDeque<>();
    public Summary(Project project) {
      this.project = project;
    }
    public Project project() {
      return project;
    }
    public void addSuppressed(Throwable throwable) {
      suppressed.add(throwable);
    }
    public void assertSuccessful() {
      if (suppressed.isEmpty()) return;
      var message = new StringJoiner("\n");
      message.add(String.format("collected %d suppressed throwable(s)", suppressed.size()));
      message.add(String.join("\n", toMarkdown()));
      var error = new AssertionError(message.toString());
      suppressed.forEach(error::addSuppressed);
      throw error;
    }
    public int countedChildlessTasks() {
      return details.size();
    }
    public int countedExecutionEvents() {
      return executions.size();
    }
    void executionBegin(Task task) {
      if (task.children().isEmpty()) return;
      var format = "|   +|%6X|        | %s";
      var thread = Thread.currentThread().getId();
      executions.add(String.format(format, thread, task.title()));
    }
    void executionEnd(Task task, ExecutionResult result) {
      var format = "|%4c|%6X|%8d| %s";
      var children = task.children();
      var kind = children.isEmpty() ? result.code() == 0 ? ' ' : 'X' : '=';
      var thread = Thread.currentThread().getId();
      var millis = result.duration().toMillis();
      var title = children.isEmpty() ? "**" + task.title() + "**" : task.title();
      var row = String.format(format, kind, thread, millis, title);
      if (children.isEmpty()) {
        var hash = Integer.toHexString(System.identityHashCode(task));
        var detail = new Detail("Task Execution Details " + hash, task, result);
        executions.add(row + " [...](#task-execution-details-" + hash + ")");
        details.add(detail);
      } else {
        executions.add(row);
      }
    }
    public List<String> toMarkdown() {
      var md = new ArrayList<String>();
      md.add("# Summary");
      md.addAll(projectDescription());
      md.addAll(taskExecutionOverview());
      md.addAll(taskExecutionDetails());
      md.addAll(exceptionDetails());
      md.addAll(systemProperties());
      return md;
    }
    private List<String> projectDescription() {
      var md = new ArrayList<String>();
      var version = Optional.ofNullable(project.version());
      md.add("");
      md.add("## Project");
      md.add("- name: " + project.name());
      md.add("- version: " + version.map(Object::toString).orElse("_none_"));
      md.add("");
      md.add("```text");
      md.add(project.toString());
      md.add("```");
      return md;
    }
    private List<String> taskExecutionOverview() {
      var md = new ArrayList<String>();
      md.add("");
      md.add("## Task Execution Overview");
      md.add("|    |Thread|Duration|Caption");
      md.add("|----|-----:|-------:|-------");
      md.addAll(executions);
      md.add("");
      md.add("Legend");
      md.add(" - A row starting with `+` denotes the start of a task container.");
      md.add(" - A blank row start (` `) is a normal task execution. Its caption is emphasized.");
      md.add(" - A row starting with `X` marks an erroneous task execution.");
      md.add(" - A row starting with `=` marks the end (sum) of a task container.");
      md.add(" - The Thread column shows the thread identifier, with `1` denoting main thread.");
      md.add(" - Duration is measured in milliseconds.");
      return md;
    }
    private List<String> taskExecutionDetails() {
      if (details.isEmpty()) return List.of();
      var md = new ArrayList<String>();
      md.add("");
      md.add("## Task Execution Details");
      md.add("");
      for (var detail : details) {
        var result = detail.result;
        md.add("### " + detail.caption);
        md.add(" - Title = " + detail.task.title());
        md.add(" - Code = " + result.code());
        md.add(" - Duration = " + result.duration());
        md.add("");
        if (!result.out().isBlank()) {
          md.add("Normal (expected) output");
          md.add("```");
          md.add(result.out().strip());
          md.add("```");
        }
        if (!result.err().isBlank()) {
          md.add("Error output");
          md.add("```");
          md.add(result.err().strip());
          md.add("```");
        }
        if (result.throwable() != null) {
          var stackTrace = new StringWriter();
          result.throwable().printStackTrace(new PrintWriter(stackTrace));
          md.add("Throwable");
          md.add("```");
          stackTrace.toString().lines().forEach(md::add);
          md.add("```");
        }
      }
      return md;
    }
    private List<String> exceptionDetails() {
      if (suppressed.isEmpty()) return List.of();
      var md = new ArrayList<String>();
      md.add("");
      md.add("## Exception Details");
      md.add("");
      md.add("- Caught " + suppressed.size() + " throwable(s).");
      md.add("");
      for (var throwable : suppressed) {
        var lines = throwable.getMessage().lines().collect(Collectors.toList());
        md.add("### " + (lines.isEmpty() ? throwable.getClass() : lines.get(0)));
        if (lines.size() > 1) md.addAll(lines);
        var stackTrace = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stackTrace));
        md.add("```text");
        stackTrace.toString().lines().forEach(md::add);
        md.add("```");
      }
      return md;
    }
    private List<String> systemProperties() {
      var md = new ArrayList<String>();
      md.add("");
      md.add("## System Properties");
      System.getProperties().stringPropertyNames().stream()
          .sorted()
          .forEach(key -> md.add(String.format("- `%s`: `%s`", key, systemProperty(key))));
      return md;
    }
    private String systemProperty(String systemPropertyKey) {
      var value = System.getProperty(systemPropertyKey);
      if (!"line.separator".equals(systemPropertyKey)) return value;
      var build = new StringBuilder();
      for (char c : value.toCharArray()) {
        build.append("0x").append(Integer.toHexString(c).toUpperCase());
      }
      return build.toString();
    }
    public Path write() {
      var markdown = toMarkdown();
      try {
        var directory = project.paths().out();
        Files.createDirectories(directory);
        return Files.write(directory.resolve("summary.md"), markdown);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    private static final class Detail {
      private final String caption;
      private final Task task;
      private final ExecutionResult result;
      private Detail(String caption, Task task, ExecutionResult result) {
        this.caption = caption;
        this.task = task;
        this.result = result;
      }
    }
  }
}
