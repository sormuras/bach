// THIS FILE WAS GENERATED ON 2019-09-08T02:40:41.806845400Z
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

// default package

import static java.lang.ModuleLayer.defineModulesWithOneLoader;
import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.INFO;
import static java.lang.System.Logger.Level.WARNING;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.SourceVersion;

public class Bach {

  public static String VERSION = "2-ea";

  /**
   * Create new Bach instance with default configuration.
   *
   * @return new default Bach instance
   */
  public static Bach of() {
    var out = new PrintWriter(System.out, true);
    var err = new PrintWriter(System.err, true);
    return new Bach(out, err, new Configuration());
  }

  /**
   * Main entry-point.
   *
   * @param args List of API method or tool names.
   */
  public static void main(String... args) {
    var bach = Bach.of();
    bach.main(args.length == 0 ? List.of("build") : List.of(args));
  }

  /** Text-output writer. */
  final PrintWriter out, err;

  /** Configuration. */
  final Configuration configuration;

  public Bach(PrintWriter out, PrintWriter err, Configuration configuration) {
    this.out = Util.requireNonNull(out, "out");
    this.err = Util.requireNonNull(err, "err");
    this.configuration = Util.requireNonNull(configuration, "configuration");
  }

  void main(List<String> args) {
    var arguments = new ArrayDeque<>(args);
    while (!arguments.isEmpty()) {
      var argument = arguments.pop();
      try {
        // Try Bach API method w/o parameter -- single argument is consumed
        var method = Util.findApiMethod(getClass(), argument);
        if (method.isPresent()) {
          method.get().invoke(this);
          continue;
        }
        // Try provided tool -- all remaining arguments are consumed
        var tool = ToolProvider.findFirst(argument);
        if (tool.isPresent()) {
          run(new Command(argument).addEach(arguments));
          return;
        }
      } catch (ReflectiveOperationException e) {
        throw new Error("Reflective operation failed for: " + argument, e);
      }
      throw new IllegalArgumentException("Unsupported argument: " + argument);
    }
  }

  String getBanner() {
    var module = getClass().getModule();
    try (var stream = module.getResourceAsStream("de/sormuras/bach/banner.txt")) {
      if (stream == null) {
        return String.format("Bach.java %s (member of %s)", VERSION, module);
      }
      var lines = new BufferedReader(new InputStreamReader(stream)).lines();
      var banner = lines.collect(Collectors.joining(System.lineSeparator()));
      return banner + " " + VERSION;
    } catch (IOException e) {
      throw new UncheckedIOException("loading banner resource failed", e);
    }
  }

  public void help() {
    out.println("F1! F1! F1!");
    out.println("Method API");
    Arrays.stream(getClass().getMethods())
        .filter(Util::isApiMethod)
        .map(m -> "  " + m.getName() + " (" + m.getDeclaringClass().getSimpleName() + ")")
        .sorted()
        .forEach(out::println);
    out.println("Provided tools");
    ServiceLoader.load(ToolProvider.class).stream()
        .map(provider -> "  " + provider.get().name())
        .sorted()
        .forEach(out::println);
  }

  public void build() {
    info();
    validate();
    resolve();
    compile();
    test();
  }

  public void clean() {
    Util.treeDelete(configuration.getWorkspaceDirectory());
  }

  public void info() {
    out.printf("Bach (%s)%n", VERSION);
    configuration.print(out);
  }

  public void validate() {
    var home = configuration.getHomeDirectory();
    Validation.validateDirectory(home);
    if (Util.list(home, Files::isDirectory).size() == 0)
      throw new Validation.Error("home contains a directory", home.toUri());
    var work = configuration.getWorkspaceDirectory();
    if (Files.exists(work)) {
      Validation.validateDirectory(work);
      if (!work.toFile().canWrite())
        throw new Validation.Error("bin is writable: %s", work.toUri());
    } else {
      var parentOfBin = work.toAbsolutePath().getParent();
      if (parentOfBin != null && !parentOfBin.toFile().canWrite())
        throw new Validation.Error("parent of work is writable", parentOfBin.toUri());
    }
    var lib = configuration.getLibraryDirectory();
    var libs = configuration.getLibraryPaths();
    Validation.validateDirectoryIfExists(lib);
    if (!libs.contains(lib)) {
      throw new Validation.Error(lib + " is member of paths", libs);
    }
    configuration.getSourceDirectories().forEach(Validation::validateDirectory);
  }

  public void resolve() {
    Resolver.resolve(this);
  }

  public void compile() {
    var main = new Realm("main", configuration);
    compile(main);
  }

  private void compile(Realm realm) {
    var modules = new TreeSet<>(realm.getDeclaredModules());
    if (modules.isEmpty()) {
      out.println("No modules declared, nothing to compile.");
      return;
    }
    var jigsaw = new Jigsaw(this);
    modules.removeAll(jigsaw.compile(realm, modules));
    if (modules.isEmpty()) {
      return;
    }
    throw new IllegalStateException("not compiled modules: " + modules);
  }

  public void test() {
    var main = new Realm("main", configuration);
    var test = new TestRealm("test", configuration, main);
    compile(test);
    var tester = new Tester(this, test);
    tester.test(test.getDeclaredModules());
  }

  public void version() {
    out.println(VERSION);
  }

  void run(Command command) {
    var name = command.getName();
    var code = run(name, (Object[]) command.toStringArray());
    if (code != 0) {
      throw new AssertionError(name + " exited with non-zero result: " + code);
    }
  }

  int run(String name, Object... arguments) {
    var strings = Arrays.stream(arguments).map(Object::toString).toArray(String[]::new);
    out.println(name + " " + String.join(" ", strings));
    var tool = ToolProvider.findFirst(name).orElseThrow();
    return tool.run(out, err, strings);
  }

  enum Property {
    /** Be verbose. */
    DEBUG("ebug", "false"),

    /** Base directory of the project. */
    PROJECT_BASE("base", ""),
    /** Name of the project. */
    PROJECT_NAME("name", "Project"),
    /** Version of the project (used for every module). */
    PROJECT_VERSION("version", "0"),

    LIBRARY_DIRECTORY("library", "lib"),
    SOURCE_DIRECTORY("source", "src"),
    TARGET_DIRECTORY("target", "bin"),

    MAVEN_REPOSITORY("maven.repository", "https://repo1.maven.org/maven2"),

    /** Options passed to all 'javac' calls. */
    TOOL_JAVAC_OPTIONS("tool.javac.options", "-encoding\nUTF-8\n-parameters\n-Xlint"),

    /** Options passed to all 'junit' calls. */
    TOOL_JUNIT_OPTIONS("tool.junit.options", "--fail-if-no-tests\n--disable-banner\n--details=tree"),
    ;

    private final String key;
    private final String defaultValue;

    Property(String key, String defaultValue) {
      this.key = key;
      this.defaultValue = defaultValue;
    }

    public String getKey() {
      return key;
    }

    String getDefaultValue() {
      return defaultValue;
    }

    String get() {
      return get(defaultValue);
    }

    String get(String defaultValue) {
      return System.getProperty(key, defaultValue);
    }
  }

  public static class Configuration {

    private final Path home;
    private final Properties properties;

    public Configuration() {
      this(Path.of(Property.PROJECT_BASE.get()));
    }

    public Configuration(Path home) {
      this.home = home;
      var file = ".bach/.properties";
      var USER = Util.load(new Properties(), Path.of(System.getProperty("user.home")).resolve(file));
      this.properties = Util.load(USER, home.resolve(System.getProperty("properties", file)));
    }

    private String get(Property property) {
      return get(property, property.get());
    }

    private String get(Property property, String defaultValue) {
      return properties.getProperty(property.getKey(), defaultValue);
    }

    List<String> lines(Property property) {
      return get(property).lines().collect(Collectors.toList());
    }

    public boolean debug() {
      return get(Property.DEBUG).equalsIgnoreCase("true");
    }

    public String getProjectName() {
      var name = Property.PROJECT_NAME;
      var dir = getHomeDirectory().toAbsolutePath().getFileName();
      return get(name, dir != null ? dir.toString() : name.getDefaultValue());
    }

    public Version getProjectVersion() {
      return Version.parse(get(Property.PROJECT_VERSION));
    }

    public Path getHomeDirectory() {
      return home;
    }

    public Path getWorkspaceDirectory() {
      return getHomeDirectory().resolve(get(Property.TARGET_DIRECTORY));
    }

    public Path getLibraryDirectory() {
      return getLibraryPaths().get(0);
    }

    public List<Path> getLibraryPaths() {
      var lib = getHomeDirectory().resolve(get(Property.LIBRARY_DIRECTORY));
      return List.of(lib);
    }

    public List<Path> getSourceDirectories() {
      var src = getHomeDirectory().resolve(get(Property.SOURCE_DIRECTORY));
      return List.of(src);
    }

    /** {@code if (module.equals("foo.bar.baz")) return URI.create("https://<path>/baz-1.3.jar")} */
    public URI getModuleUri(String module) {
      throw new UnmappedModuleException(module);
    }

    /** {@code module.startsWith("foo.bar") -> URI.create("https://dl.bintray.com/foo-bar/maven")} */
    public URI getModuleMavenRepository(@SuppressWarnings("unused") String module) {
      return URI.create(get(Property.MAVEN_REPOSITORY));
    }

    /** {@code if (module.equals("foo.bar.baz")) return "org.foo.bar:foo-baz"} */
    public String getModuleMavenGroupAndArtifact(String module) {
      throw new UnmappedModuleException(module);
    }

    public String getModuleVersion(String module) {
      throw new UnmappedModuleException(module);
    }

    void print(PrintWriter writer) {
      var home = getHomeDirectory();
      writer.printf("  home = '%s' -> %s%n", home, home.toUri());
      writer.printf("  workspace = '%s'%n", getWorkspaceDirectory());
      writer.printf("  library paths = %s%n", getLibraryPaths());
      writer.printf("  source directories = %s%n", getSourceDirectories());
      writer.printf("  project name = %s%n", getProjectName());
      writer.printf("  project version = %s%n", getProjectVersion());
    }

    static class UnmappedModuleException extends RuntimeException {
      private static final long serialVersionUID = 0;

      UnmappedModuleException(String module) {
        super("Module " + module + " is not mapped");
      }
    }
  }

  /** Command-line program argument list builder. */
  public static class Command {

    private final String name;
    private final List<String> list = new ArrayList<>();

    /** Initialize Command instance with zero or more arguments. */
    Command(String name, Object... args) {
      this.name = name;
      addEach(args);
    }

    /** Add single argument by invoking {@link Object#toString()} on the given argument. */
    Command add(Object argument) {
      list.add(argument.toString());
      return this;
    }

    /** Add two arguments by invoking {@link #add(Object)} for the key and value elements. */
    Command add(Object key, Object value) {
      return add(key).add(value);
    }

    /** Add two (or zero) arguments, the key and the paths joined by system's path separator. */
    Command add(Object key, Collection<Path> paths) {
      return add(key, paths, UnaryOperator.identity());
    }

    /** Add two (or zero) arguments, the key and the paths joined by system's path separator. */
    Command add(Object key, Collection<Path> paths, UnaryOperator<String> operator) {
      var stream = paths.stream().filter(Files::exists).map(Object::toString);
      var value = stream.collect(Collectors.joining(File.pathSeparator));
      if (value.isEmpty()) {
        return this;
      }
      return add(key, operator.apply(value));
    }

    /** Add all arguments by invoking {@link #add(Object)} for each element. */
    Command addEach(Object... arguments) {
      return addEach(List.of(arguments));
    }

    /** Add all arguments by invoking {@link #add(Object)} for each element. */
    Command addEach(Iterable<?> arguments) {
      arguments.forEach(this::add);
      return this;
    }

    /** Add a single argument iff the conditions is {@code true}. */
    Command addIff(boolean condition, Object argument) {
      return condition ? add(argument) : this;
    }

    /** Add two arguments iff the conditions is {@code true}. */
    Command addIff(boolean condition, Object key, Object value) {
      return condition ? add(key, value) : this;
    }

    /** Add two arguments iff the given optional value is present. */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    Command addIff(Object key, Optional<?> optionalValue) {
      return optionalValue.isPresent() ? add(key, optionalValue.get()) : this;
    }

    /** Let the consumer visit, usually modify, this instance iff the conditions is {@code true}. */
    Command addIff(boolean condition, Consumer<Command> visitor) {
      if (condition) {
        visitor.accept(this);
      }
      return this;
    }

    /** Return the command's name. */
    public String getName() {
      return name;
    }

    /** Return the command's arguments. */
    public List<String> getList() {
      return list;
    }

    @Override
    public String toString() {
      var args = list.isEmpty() ? "<empty>" : "'" + String.join("', '", list) + "'";
      return "Command{name='" + name + "', list=[" + args + "]}";
    }

    /** Returns an array of {@link String} containing all of the collected arguments. */
    String[] toStringArray() {
      return list.toArray(String[]::new);
    }
  }

  static class Realm {

    /** Collect information about a declared module. */
    class Info {
      final Path base;
      final Path path;
      final String module;
      final String moduleDashVersion;
      final Path resources;
      final Path sources;

      Info(Path base, Path path) {
        this.base = base; // "src/modules"
        this.path = path; // "${module}/${realm}/java/module-info.java"
        this.module = path.getName(0).toString();
        this.moduleDashVersion = module + '-' + Realm.this.configuration.getProjectVersion();
        this.resources = base.resolve(module).resolve(Realm.this.name).resolve("resources");
        this.sources = base.resolve(module).resolve(Realm.this.name).resolve("java");
      }

      Path getResources() {
        return resources;
      }

      Optional<String> getMainClass() {
        return Optional.empty();
      }

      Path getModularJar() {
        return getDestination().resolve("modules").resolve(moduleDashVersion + ".jar");
      }

      Path getSourcesJar() {
        return getDestination().resolve("sources").resolve(moduleDashVersion + "-sources.jar");
      }
    }

    private final String name;
    private final Configuration configuration;
    private final Path destination;
    private final String moduleSourcePath;
    private final Map<String, Info> declaredModules;

    Realm(String name, Configuration configuration) {
      this.name = Util.requireNonNull(name, "realm name");
      this.configuration = Util.requireNonNull(configuration, "configuration");
      this.destination = configuration.getWorkspaceDirectory().resolve(name);
      var declaredModules = new TreeMap<String, Info>();
      var moduleSourcePaths = new TreeSet<String>();
      for (var src : configuration.getSourceDirectories()) {
        try (var stream = Files.list(src)) {
          for (var path : stream.collect(Collectors.toList())) {
            var file = path.resolve(name + "/java/module-info.java");
            if (!Util.isModuleInfo(file)) {
              file = path.resolve(name + "/module/module-info.java");
            }
            if (!Util.isModuleInfo(file)) {
              continue;
            }
            var info = new Info(src, src.relativize(path));
            declaredModules.put(info.module, info);
            var offset = path.relativize(file.subpath(0, file.getNameCount() - 1)).toString();
            moduleSourcePaths.add(String.join(File.separator, src.toString(), "*", offset));
          }
        } catch (IOException e) {
          throw new UncheckedIOException("list directory failed: " + src, e);
        }
      }
      this.declaredModules = declaredModules;
      this.moduleSourcePath = String.join(File.pathSeparator, moduleSourcePaths);
    }

    void addModulePatches(Command javac, Collection<String> modules) {}

    Set<String> getDeclaredModules() {
      return declaredModules.keySet();
    }

    Info getDeclaredModuleInfo(String module) {
      return declaredModules.get(module);
    }

    Path getDestination() {
      return destination;
    }

    List<Path> getModulePaths() {
      var paths = new ArrayList<Path>();
      paths.add(getDestination().resolve("modules"));
      paths.addAll(configuration.getLibraryPaths());
      return Util.findExistingDirectories(paths);
    }

    String getModuleSourcePath() {
      return moduleSourcePath;
    }

    List<Path> getRuntimeModulePaths(Path... initialPaths) {
      return getModulePaths();
    }
  }

  /** Test all modules of the project. */
  static class Tester {

    private final Bach bach;
    private final TestRealm test;

    Tester(Bach bach, TestRealm test) {
      this.bach = bach;
      this.test = test;
    }

    @SuppressWarnings("unused")
    private void log(System.Logger.Level level, String format, Object... args) {
      if (bach.configuration.debug() || level.getSeverity() > DEBUG.getSeverity()) {
        var writer = level.getSeverity() > INFO.getSeverity() ? bach.err : bach.out;
        writer.println(String.format(format, args));
      }
    }

    void test(Collection<String> modules) {
      var console =
          ModuleFinder.of(bach.configuration.getLibraryPaths().toArray(Path[]::new))
              .find("org.junit.platform.console");
      var errors = new StringBuilder();
      for (var module : modules) {
        var info = test.getDeclaredModuleInfo(module);
        if (info == null) {
          log(WARNING, "No test module available for: %s", module);
          continue;
        }
        log(INFO, "%n%n%n%s%n%n%n", module);
        // errors.append(testModuleMainClass(module));
        if (console.isPresent()) {
          // errors.append(testClassPathDirect(module));
          // errors.append(testClassPathForked(module));
          errors.append(testModulePathDirect(info));
          // errors.append(testModulePathForked(module));
        } else {
          log(WARNING, "Module 'org.junit.platform.console' not present");
        }
      }
      if (errors.toString().replace('0', ' ').isBlank()) {
        return;
      }
      throw new AssertionError("Test run with errors: " + errors);
    }

    //    int testModuleMainClass(String module) {
    //      var mainClass =
    //          ModuleFinder.of(project.test.modularJar(module))
    //              .find(module)
    //              .orElseThrow()
    //              .descriptor()
    //              .mainClass();
    //      if (mainClass.isEmpty()) { // No main class present...
    //        return 0;
    //      }
    //      var needsPatch = project.main.declaredModules.containsKey(module);
    //      var java =
    //          new Command("java")
    //              .add("--module-path", project.test.modulePathRuntime(needsPatch))
    //              .add("--module", module);
    //      return runner.run(java);
    //    }

    //    int testClassPathDirect(String module) {
    //      var classPath = project.test.classPathRuntime(module);
    //      var urls = classPath.stream().map(Util::url).toArray(URL[]::new);
    //      var parentLoader = ClassLoader.getPlatformClassLoader();
    //      var junitLoader = new URLClassLoader("junit", urls, parentLoader);
    //      var junit = new Command("junit").addEach(configuration.lines(Property.OPTIONS_JUNIT));
    //      project.test.packages(module).forEach(path -> junit.add("--select-package", path));
    //      return launchJUnitPlatformConsole(junitLoader, junit);
    //    }

    //    int testClassPathForked(String module) {
    //      var java =
    //          new Command("java")
    //              .add("-ea")
    //              .add("--class-path", project.test.classPathRuntime(module))
    //              .add("org.junit.platform.console.ConsoleLauncher")
    //              .addEach(configuration.lines(Property.OPTIONS_JUNIT));
    //      project.test.packages(module).forEach(path -> java.add("--select-package", path));
    //      return runner.run(java);
    //    }

    private int testModulePathDirect(Realm.Info info) {
      var junit =
          new Command("junit")
              .addEach(bach.configuration.lines(Property.TOOL_JUNIT_OPTIONS))
              .add("--select-module", info.module);
      try {
        return testModulePathDirect(info, junit);
      } finally {
        var windows = System.getProperty("os.name", "?").toLowerCase().contains("win");
        if (windows) {
          System.gc();
          Util.sleep(1234);
        }
      }
    }

    //    int testModulePathForked(String module) {
    //      var needsPatch = project.main.declaredModules.containsKey(module);
    //      var java =
    //          new Command("java")
    //              .add("-ea")
    //              .add("--module-path", project.test.modulePathRuntime(needsPatch))
    //              .add("--add-modules", module);
    //      if (needsPatch) {
    //        java.add("--patch-module", module + "=" + project.main.modularJar(module));
    //      }
    //      java.add("--module")
    //          .add("org.junit.platform.console")
    //          .addEach(configuration.lines(Property.OPTIONS_JUNIT))
    //          .add("--select-module", module);
    //      return runner.run(java);
    //    }

    /** Launch JUnit Platform Console for the given "module under test". */
    private int testModulePathDirect(Realm.Info info, Command junit) {
      var modulePath = test.getRuntimeModulePaths(info.getModularJar());
      log(DEBUG, "Module path:");
      for (var element : modulePath) {
        log(DEBUG, "  -> %s", element);
      }
      var finder = ModuleFinder.of(modulePath.toArray(Path[]::new));
      log(DEBUG, "Finder finds module(s):");
      for (var reference : finder.findAll()) {
        log(DEBUG, "  -> %s", reference);
      }
      var roots = List.of(info.module, "org.junit.platform.console");
      log(DEBUG, "Root module(s):");
      for (var root : roots) {
        log(DEBUG, "  -> %s", root);
      }
      var boot = ModuleLayer.boot();
      var configuration = boot.configuration().resolveAndBind(finder, ModuleFinder.of(), roots);
      var parentLoader = ClassLoader.getSystemClassLoader();
      var controller = defineModulesWithOneLoader(configuration, List.of(boot), parentLoader);
      var junitConsoleLayer = controller.layer();
      controller.addExports(
          junitConsoleLayer.findModule("org.junit.platform.console").orElseThrow(),
          "org.junit.platform.console",
          Bach.class.getModule());
      var junitConsoleLoader = junitConsoleLayer.findLoader("org.junit.platform.console");
      var junitLoader = new URLClassLoader("junit", new URL[0], junitConsoleLoader);
      return launchJUnitPlatformConsole(junitLoader, junit);
    }

    /** Launch JUnit Platform Console passing all arguments of the given command. */
    private int launchJUnitPlatformConsole(ClassLoader loader, Command junit) {
      log(DEBUG, "Launching JUnit Platform Console: %s", junit.getList());
      log(DEBUG, "Using class loader: %s - %s", loader.getName(), loader);
      var currentThread = Thread.currentThread();
      var currentContextLoader = currentThread.getContextClassLoader();
      currentThread.setContextClassLoader(loader);
      var parent = loader;
      while (parent != null) {
        parent.setDefaultAssertionStatus(true);
        parent = parent.getParent();
      }
      try {
        var launcher = loader.loadClass("org.junit.platform.console.ConsoleLauncher");
        var params = new Class<?>[] {PrintStream.class, PrintStream.class, String[].class};
        var execute = launcher.getMethod("execute", params);
        var out = new ByteArrayOutputStream();
        var err = new ByteArrayOutputStream();
        var args = junit.toStringArray();
        var result = execute.invoke(null, new PrintStream(out), new PrintStream(err), args);
        bach.out.write(out.toString());
        bach.out.flush();
        bach.err.write(err.toString());
        bach.err.flush();
        return (int) result.getClass().getMethod("getExitCode").invoke(result);
      } catch (Exception e) {
        throw new Error("ConsoleLauncher.execute(...) failed: " + e, e);
      } finally {
        currentThread.setContextClassLoader(currentContextLoader);
      }
    }
  }

  static class TestRealm extends Realm {

    private final Realm main;

    TestRealm(String name, Configuration configuration, Realm main) {
      super(name, configuration);
      this.main = main;
    }

    void addModulePatches(Command javac, Collection<String> modules) {
      var mainModules = main.getDeclaredModules();
      for (var module : modules) {
        if (mainModules.contains(module)) {
          var patch = main.getDeclaredModuleInfo(module).sources;
          javac.add("--patch-module", module + "=" + patch);
        }
      }
    }

    List<Path> getModulePaths() {
      var paths = new LinkedHashSet<Path>();
      paths.addAll(super.getModulePaths()); // "test" realm
      paths.addAll(main.getModulePaths()); // "main" realm
      return Util.findExistingDirectories(paths);
    }

    @Override
    List<Path> getRuntimeModulePaths(Path... initialPaths) {
      var paths = new LinkedHashSet<>(List.of(initialPaths));
      paths.add(main.getDestination().resolve("modules")); // main modules
      paths.addAll(getModulePaths()); // test modules + library paths
      return Util.findExisting(paths);
    }
  }

  /** Default multi-module compiler. */
  static class Jigsaw {

    private final Bach bach;
    private final Configuration configuration;

    Jigsaw(Bach bach) {
      this.bach = bach;
      this.configuration = bach.configuration;
    }

    Collection<String> compile(Realm realm, Collection<String> modules) {
      var destination = realm.getDestination().resolve("compile/jigsaw");
      var javac =
          new Command("javac")
              .add("-d", destination)
              .addEach(configuration.lines(Property.TOOL_JAVAC_OPTIONS))
              // .addIff(realm.preview, "--enable-preview")
              // .addIff(realm.release != null, "--release", realm.release)
              .add("--module-path", realm.getModulePaths())
              .add("--module-source-path", realm.getModuleSourcePath())
              .add("--module", String.join(",", modules))
              .add("--module-version", configuration.getProjectVersion());
      realm.addModulePatches(javac, modules);
      bach.run(javac);

      for (var module : modules) {
        var info = realm.getDeclaredModuleInfo(module);
        var modularJar = info.getModularJar();
        var resources = info.getResources();

        Util.treeCreate(modularJar.getParent()); // jar doesn't create directories...
        var jarModule =
            new Command("jar")
                .add("--create")
                .add("--file", modularJar)
                .addIff(configuration.debug(), "--verbose")
                .addIff("--main-class", info.getMainClass())
                .add("-C", destination.resolve(module))
                .add(".")
                .addIff(Files.isDirectory(resources), cmd -> cmd.add("-C", resources).add("."));
        bach.run(jarModule);

        var sourcesJar = info.getSourcesJar();
        Util.treeCreate(sourcesJar.getParent()); // jar still doesn't create directories...
        var jarSources =
            new Command("jar")
                .add("--create")
                .add("--file", sourcesJar)
                .addIff(configuration.debug(), "--verbose")
                .add("--no-manifest")
                .add("-C", info.sources)
                .add(".")
                .addIff(Files.isDirectory(resources), cmd -> cmd.add("-C", resources).add("."));
        bach.run(jarSources);
      }

      return modules;
    }
  }

  /** Resolves required modules. */
  static class Resolver {

    private static final Pattern MODULE_NAME_PATTERN = Pattern.compile("(?:module)\\s+([\\w.]+)");
    private static final Pattern MODULE_REQUIRES_PATTERN =
        Pattern.compile(
            "(?:requires)" // key word
                + "(?:\\s+[\\w.]+)?" // optional modifiers
                + "\\s+([\\w.]+)" // module name
                + "(?:\\s*/\\*\\s*([\\w.\\-+]+)\\s*\\*/\\s*)?" // optional '/*' version '*/'
                + ";"); // end marker

    static void resolve(Bach bach) {
      var entries = bach.configuration.getLibraryPaths().toArray(Path[]::new);
      var library = of(ModuleFinder.of(entries));
      bach.out.println("Library of -> " + bach.configuration.getLibraryPaths());
      bach.out.println("  modules  -> " + library.modules);
      bach.out.println("  requires -> " + library.requires);

      var sources = of(bach.configuration.getSourceDirectories());
      bach.out.println("Sources of -> " + bach.configuration.getSourceDirectories());
      bach.out.println("  modules  -> " + sources.modules);
      bach.out.println("  requires -> " + sources.requires);

      var systems = of(ModuleFinder.ofSystem());

      var missing = new TreeMap<String, Set<Version>>();
      missing.putAll(sources.requires);
      missing.putAll(library.requires);
      sources.getDeclaredModules().forEach(missing::remove);
      library.getDeclaredModules().forEach(missing::remove);
      systems.getDeclaredModules().forEach(missing::remove);
      if (missing.isEmpty()) {
        return;
      }

      var transfer = new Transfer(bach.out, bach.err);
      var worker = new Worker(bach, transfer);
      do {
        bach.out.println("Loading missing modules: " + missing);
        var items = new ArrayList<Transfer.Item>();
        for (var entry : missing.entrySet()) {
          var module = entry.getKey();
          var versions = entry.getValue();
          items.add(worker.toTransferItem(module, versions));
        }
        var lib = bach.configuration.getLibraryDirectory();
        transfer.getFiles(lib, items);
        library = of(ModuleFinder.of(entries));
        missing = new TreeMap<>(library.requires);
        library.getDeclaredModules().forEach(missing::remove);
        systems.getDeclaredModules().forEach(missing::remove);
      } while (!missing.isEmpty());
    }

    /** Command-line argument factory. */
    static Resolver of(Collection<String> declaredModules, Iterable<String> requires) {
      var map = new TreeMap<String, Set<Version>>();
      for (var string : requires) {
        var versionMarkerIndex = string.indexOf('@');
        var any = versionMarkerIndex == -1;
        var module = any ? string : string.substring(0, versionMarkerIndex);
        var version = any ? null : Version.parse(string.substring(versionMarkerIndex + 1));
        map.merge(module, any ? Set.of() : Set.of(version), Util::concat);
      }
      return new Resolver(new TreeSet<>(declaredModules), map);
    }

    static Resolver of(ModuleFinder finder) {
      var declaredModules = new TreeSet<String>();
      var requiredModules = new TreeMap<String, Set<Version>>();
      finder.findAll().stream()
          .map(ModuleReference::descriptor)
          .peek(descriptor -> declaredModules.add(descriptor.name()))
          .map(ModuleDescriptor::requires)
          .flatMap(Set::stream)
          .filter(r -> !r.modifiers().contains(ModuleDescriptor.Requires.Modifier.MANDATED))
          .filter(r -> !r.modifiers().contains(ModuleDescriptor.Requires.Modifier.STATIC))
          .distinct()
          .forEach(
              requires ->
                  requiredModules.merge(
                      requires.name(),
                      requires.compiledVersion().map(Set::of).orElse(Set.of()),
                      Util::concat));
      return new Resolver(declaredModules, requiredModules);
    }

    static Resolver of(String... sources) {
      var declaredModules = new TreeSet<String>();
      var map = new TreeMap<String, Set<Version>>();
      for (var source : sources) {
        var nameMatcher = MODULE_NAME_PATTERN.matcher(source);
        if (!nameMatcher.find()) {
          throw new IllegalArgumentException("Expected module-info.java source, but got: " + source);
        }
        declaredModules.add(nameMatcher.group(1).trim());
        var requiresMatcher = MODULE_REQUIRES_PATTERN.matcher(source);
        while (requiresMatcher.find()) {
          var name = requiresMatcher.group(1);
          var version = requiresMatcher.group(2);
          map.merge(name, version == null ? Set.of() : Set.of(Version.parse(version)), Util::concat);
        }
      }
      return new Resolver(declaredModules, map);
    }

    static Resolver of(Collection<Path> sourcePaths) {
      var sources = new ArrayList<String>();
      for (var sourcePath : sourcePaths) {
        try (var stream = Files.find(sourcePath, 9, (p, __) -> Util.isModuleInfo(p))) {
          for (var moduleInfo : stream.collect(Collectors.toSet())) {
            sources.add(Files.readString(moduleInfo));
          }
        } catch (IOException e) {
          throw new UncheckedIOException("find or read failed: " + sourcePath, e);
        }
      }
      return of(sources.toArray(new String[0]));
    }

    private final Set<String> modules;
    private final Map<String, Set<Version>> requires;

    Resolver(Set<String> modules, Map<String, Set<Version>> requires) {
      this.modules = modules;
      this.requires = requires;
    }

    Set<String> getDeclaredModules() {
      return modules;
    }

    Set<String> getRequiredModules() {
      return requires.keySet();
    }

    Optional<Version> getRequiredVersion(String requiredModule) {
      var versions = requires.get(requiredModule);
      if (versions == null) {
        throw new NoSuchElementException("Module " + requiredModule + " is not mapped");
      }
      if (versions.size() > 1) {
        throw new IllegalStateException("Multiple versions: " + requiredModule + " -> " + versions);
      }
      return versions.stream().findFirst();
    }

    static class Worker {

      static class Lookup {

        final String name;
        final Properties properties;
        final Set<Pattern> patterns;
        final UnaryOperator<String> custom;

        Lookup(Transfer transfer, Path lib, String name, UnaryOperator<String> custom) {
          this.name = name;
          var uri = "https://github.com/sormuras/modules/raw/master/" + name;
          var modules = Path.of(System.getProperty("user.home")).resolve(".bach/modules");
          try {
            Files.createDirectories(modules);
          } catch (IOException e) {
            throw new UncheckedIOException("Creating directories failed: " + modules, e);
          }
          var defaultModules = transfer.getFile(URI.create(uri), modules.resolve(name));
          var defaults = Util.load(new Properties(), defaultModules);
          this.properties = Util.load(new Properties(defaults), lib.resolve(name));
          this.patterns =
              properties.keySet().stream()
                  .map(Object::toString)
                  .filter(key -> !SourceVersion.isName(key))
                  .map(Pattern::compile)
                  .collect(Collectors.toSet());
          this.custom = custom;
        }

        String get(String key) {
          try {
            return custom.apply(key);
          } catch (Configuration.UnmappedModuleException e) {
            // fall-through
          }
          var value = properties.getProperty(key);
          if (value != null) {
            return value;
          }
          for (var pattern : patterns) {
            if (pattern.matcher(key).matches()) {
              return properties.getProperty(pattern.pattern());
            }
          }
          throw new IllegalStateException("No lookup value mapped for: " + key);
        }

        @Override
        public String toString() {
          var size = properties.size();
          var names = properties.stringPropertyNames().size();
          return String.format(
              "module properties {name: %s, size: %d, names: %d}", name, size, names);
        }
      }

      final Bach bach;
      final Properties moduleUri;
      final Lookup moduleMaven, moduleVersion;

      Worker(Bach bach, Transfer transfer) {
        this.bach = bach;
        var cfg = bach.configuration;
        var lib = cfg.getLibraryDirectory();
        this.moduleUri = Util.load(new Properties(), lib.resolve("module-uri.properties"));
        this.moduleMaven =
            new Lookup(transfer, lib, "module-maven.properties", cfg::getModuleMavenGroupAndArtifact);
        this.moduleVersion =
            new Lookup(transfer, lib, "module-version.properties", cfg::getModuleVersion);
      }

      private URI getModuleUri(String module) {
        try {
          return bach.configuration.getModuleUri(module);
        } catch (Configuration.UnmappedModuleException e) {
          var uri = moduleUri.getProperty(module);
          if (uri == null) {
            return null;
          }
          return URI.create(uri);
        }
      }

      private Transfer.Item toTransferItem(String module, Set<Version> set) {
        var uri = getModuleUri(module);
        if (uri != null) {
          var file = Util.findFileName(uri);
          var version = Util.findVersion(file.orElse(""));
          return Transfer.Item.of(uri, module + version.map(v -> '-' + v).orElse("") + ".jar");
        }
        var repository = bach.configuration.getModuleMavenRepository(module);
        var maven = moduleMaven.get(module).split(":");
        var group = maven[0];
        var artifact = maven[1];
        var version = Util.singleton(set).map(Object::toString).orElse(moduleVersion.get(module));
        var mappedUri = toUri(repository.toString(), group, artifact, version);
        return Transfer.Item.of(mappedUri, module + '-' + version + ".jar");
      }

      private URI toUri(String repository, String group, String artifact, String version) {
        var file = artifact + '-' + version + ".jar";
        var uri = String.join("/", repository, group.replace('.', '/'), artifact, version, file);
        return URI.create(uri);
      }
    }
  }

  /** File transfer. */
  static class Transfer {

    static Path transfer(URI source, Path target) {
      Util.treeCreate(target.getParent());
      return new Transfer(new PrintWriter(System.out), new PrintWriter(System.err)).getFile(source, target);
    }

    static class Item {

      static Item of(URI uri, String file) {
        return new Item(uri, file);
      }

      private final URI uri;
      private final String file;

      private Item(URI uri, String file) {
        this.uri = uri;
        this.file = file;
      }
    }

    private final PrintWriter out, err;
    private final HttpClient client;

    Transfer(PrintWriter out, PrintWriter err) {
      this(out, err, HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build());
    }

    private Transfer(PrintWriter out, PrintWriter err, HttpClient client) {
      this.out = out;
      this.err = err;
      this.client = client;
    }

    void getFiles(Path path, Collection<Item> items) {
      Util.treeCreate(path);
      items.stream()
          .parallel()
          .map(item -> getFile(item.uri, path.resolve(item.file)))
          .collect(Collectors.toSet());
    }

    Path getFile(URI uri, Path path) {
      if ("file".equals(uri.getScheme())) {
        try {
          return Files.copy(Path.of(uri), path, StandardCopyOption.REPLACE_EXISTING);
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
          err.println("Couldn't get 'user:etag' file attribute: " + e);
        }
      }
      try {
        var handler = HttpResponse.BodyHandlers.ofFile(path);
        var response = client.send(request.build(), handler);
        if (response.statusCode() == 200) {
          var etagHeader = response.headers().firstValue("etag");
          if (etagHeader.isPresent()) {
            try {
              var etag = etagHeader.get();
              Files.setAttribute(path, "user:etag", StandardCharsets.UTF_8.encode(etag));
            } catch (Exception e) {
              err.println("Couldn't set 'user:etag' file attribute: " + e);
            }
          }
          var lastModifiedHeader = response.headers().firstValue("last-modified");
          if (lastModifiedHeader.isPresent()) {
            try {
              var format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
              var millis = format.parse(lastModifiedHeader.get()).getTime(); // 0 means "unknown"
              var fileTime = FileTime.fromMillis(millis == 0 ? System.currentTimeMillis() : millis);
              Files.setLastModifiedTime(path, fileTime);
            } catch (Exception e) {
              err.println("Couldn't set last modified file attribute: " + e);
            }
          }
          synchronized (out) {
            out.println(path + " <- " + uri);
          }
        }
      } catch (IOException | InterruptedException e) {
        err.println("Failed to load: " + uri + " -> " + e);
        e.printStackTrace(err);
      }
      return path;
    }
  }

  /** Static helpers. */
  static class Util {

    static <E extends Comparable<E>> Set<E> concat(Set<E> one, Set<E> two) {
      return Stream.concat(one.stream(), two.stream()).collect(Collectors.toCollection(TreeSet::new));
    }

    static Optional<Method> findApiMethod(Class<?> container, String name) {
      try {
        var method = container.getMethod(name);
        return isApiMethod(method) ? Optional.of(method) : Optional.empty();
      } catch (NoSuchMethodException e) {
        return Optional.empty();
      }
    }

    static List<Path> findExisting(Collection<Path> paths) {
      return paths.stream().filter(Files::exists).collect(Collectors.toList());
    }

    static List<Path> findExistingDirectories(Collection<Path> directories) {
      return directories.stream().filter(Files::isDirectory).collect(Collectors.toList());
    }

    static boolean isApiMethod(Method method) {
      if (method.getDeclaringClass().equals(Object.class)) return false;
      if (Modifier.isStatic(method.getModifiers())) return false;
      return method.getParameterCount() == 0;
    }

    static boolean isModuleInfo(Path path) {
      return Files.isRegularFile(path) && path.getFileName().toString().equals("module-info.java");
    }

    static List<Path> list(Path directory) {
      return list(directory, __ -> true);
    }

    static List<Path> list(Path directory, Predicate<Path> filter) {
      try {
        return Files.list(directory).filter(filter).sorted().collect(Collectors.toList());
      } catch (IOException e) {
        throw new UncheckedIOException("list directory failed: " + directory, e);
      }
    }

    static Properties load(Properties properties, Path path) {
      if (Files.isRegularFile(path)) {
        try (var reader = Files.newBufferedReader(path)) {
          properties.load(reader);
        } catch (IOException e) {
          throw new UncheckedIOException("Reading properties failed: " + path, e);
        }
      }
      return properties;
    }

    /** Extract last path element from the supplied uri. */
    static Optional<String> findFileName(URI uri) {
      var path = uri.getPath();
      return path == null ? Optional.empty() : Optional.of(path.substring(path.lastIndexOf('/') + 1));
    }

    static Optional<String> findVersion(String jarFileName) {
      if (!jarFileName.endsWith(".jar")) return Optional.empty();
      var name = jarFileName.substring(0, jarFileName.length() - 4);
      var matcher = Pattern.compile("-(\\d+(\\.|$))").matcher(name);
      return (matcher.find()) ? Optional.of(name.substring(matcher.start() + 1)) : Optional.empty();
    }

    static <C extends Collection<?>> C requireNonEmpty(C collection, String name) {
      if (requireNonNull(collection, name + " must not be null").isEmpty()) {
        throw new IllegalArgumentException(name + " must not be empty");
      }
      return collection;
    }

    static <T> T requireNonNull(T object, String name) {
      return Objects.requireNonNull(object, name + " must not be null");
    }

    static <T> Optional<T> singleton(Collection<T> collection) {
      if (collection.isEmpty()) {
        return Optional.empty();
      }
      if (collection.size() != 1) {
        throw new IllegalStateException("Too many elements: " + collection);
      }
      return Optional.of(collection.iterator().next());
    }

    /** Sleep and silently clear current thread's interrupted status. */
    static void sleep(long millis) {
      try {
        Thread.sleep(millis);
      } catch (InterruptedException e) {
        Thread.interrupted();
      }
    }

    /** @see Files#createDirectories(Path, FileAttribute[]) */
    static Path treeCreate(Path path) {
      try {
        return Files.createDirectories(path);
      } catch (IOException e) {
        throw new UncheckedIOException("create directories failed: " + path, e);
      }
    }

    /** Delete all files and directories from and including the root directory. */
    static void treeDelete(Path root) {
      treeDelete(root, __ -> true);
    }

    /** Delete selected files and directories from and including the root directory. */
    static void treeDelete(Path root, Predicate<Path> filter) {
      if (filter.test(root)) { // trivial case: delete existing empty directory or single file
        try {
          Files.deleteIfExists(root);
          return;
        } catch (IOException ignored) {
          // fall-through
        }
      }
      try (var stream = Files.walk(root)) { // default case: walk the tree...
        var selected = stream.filter(filter).sorted((p, q) -> -p.compareTo(q));
        for (var path : selected.collect(Collectors.toList())) {
          Files.deleteIfExists(path);
        }
      } catch (IOException e) {
        throw new UncheckedIOException("tree delete failed: " + root, e);
      }
    }
  }

  public static class Validation {

    public static class Error extends AssertionError {
      private static final long serialVersionUID = 0;

      Error(String expected, Object hint) {
        super(String.format("expected that %s: %s", expected, hint));
      }
    }

    static void validateDirectoryIfExists(Path path) {
      if (Files.exists(path)) validateDirectory(path);
    }

    static void validateDirectory(Path path) {
      if (!Files.isDirectory(path)) throw new Error("path is a directory", path.toUri());
    }
  }
}
