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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.lang.module.ModuleFinder;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;

/** Java Shell Builder. */
@SuppressWarnings("WeakerAccess")
public class Bach {

  /** Version of Bach, {@link Runtime.Version#parse(String)}-compatible. */
  public static final String VERSION = "2-ea";

  /** Convenient short-cut to {@code "user.home"} as a path. */
  static final Path USER_HOME = Path.of(System.getProperty("user.home"));

  /**
   * Main entry-point of Bach.
   *
   * @param arguments task name(s) and their argument(s)
   * @throws Error on a non-zero error code
   */
  public static void main(String... arguments) {
    var bach = new Bach();
    var args = List.of(arguments);
    var code = bach.main(args);
    if (code != 0) {
      throw new Error("Bach (" + args + ") failed with error code: " + code);
    }
  }

  private final PrintWriter out;
  private final PrintWriter err;
  private final boolean verbose;
  private final Path home;
  private final Path work;
  private final Runner runner;
  private final Project project;

  Bach() {
    this(new PrintWriter(System.out, true), new PrintWriter(System.err, true));
  }

  Bach(PrintWriter out, PrintWriter err) {
    this(
        out,
        err,
        Path.of(System.getProperty("bach.home", "")),
        Path.of(System.getProperty("bach.work", "")),
        Boolean.getBoolean("bach.verbose"));
  }

  Bach(PrintWriter out, PrintWriter err, Path home, Path work, boolean verbose) {
    this.out = out;
    this.err = err;
    this.home = home;
    this.work = work;
    this.verbose = verbose;
    this.runner = new Runner();
    this.project = new Project();
    log("Bach %s initialized.", VERSION);
  }

  void log(String format, Object... args) {
    if (verbose) {
      out.println(String.format(format, args));
    }
  }

  /** Print runtime and project-related information. */
  public void info() {
    log("Bach::info()");
    out.println("Bach information");
    out.println("  out = " + out);
    out.println("  err = " + err);
    out.println("  cwd = " + Path.of(System.getProperty("user.dir")));
    out.println("  home = '" + home + "'");
    out.println("  work = '" + work + "'");
    out.println("  verbose = " + verbose);
    project.toStrings(out::println);
    log("Bach::info() end.");
  }

  /** Build the project. */
  public void build() {
    log("Bach::build()");
    if (verbose) {
      info();
    }
    compile();
    summary();
    log("Bach::build() end.");
  }

  /** Compile all modules. */
  public void compile() {
    log("Bach::compile()");
    compile(project.main);
    compile(project.test);
    log("Bach::compile() end.");
  }

  private void compile(Project.Realm realm) {
    log("Bach::compile(%s)", realm.name);
    if (realm.modules.isEmpty()) {
      log("Bach::compile(%s) end -- no modules to compile.", realm.name);
      return;
    }
    try {
      compile(realm, realm.modules);
    } catch (Exception e) {
      throw new Error("Compiling realm " + realm.name + " failed!", e);
    }
  }

  private void compile(Project.Realm realm, List<String> modules) throws Exception {
    var javac =
        new Command("javac")
            .add("-d", realm.binClasses)
            .add("-encoding", "UTF-8")
            .add("-parameters")
            .add("-Xlint")
            .addIff(realm.preview, "--enable-preview")
            .addIff(realm.release != null, "--release", realm.release)
            .add("--module-path", realm.modulePath)
            .add("--module-source-path", realm.moduleSourcePath)
            .add("--module-version", project.version)
            .add("--module", String.join(",", modules));
    if (realm == project.test) {
      for (var module : modules) {
        if (project.main.modules.contains(module)) {
          // javac.add("--patch-module", module + "=" + project.main.binClasses.resolve(module));
          javac.add("--patch-module", module + "=" + project.main.moduleSourcePath.replace("*", module));
        }
      }
    }
    if (runner.run(javac) != 0) {
      throw new IllegalStateException("javac failed");
    }
    var realmModules = Files.createDirectories(realm.binModules);
    var realmSources = Files.createDirectories(realm.binSources);
    for (var module : modules) {
      var moduleNameDashVersion = module + '-' + project.version;
      var modularJar = realmModules.resolve(moduleNameDashVersion + ".jar");
      var sourcesJar = realmSources.resolve(moduleNameDashVersion + "-sources.jar");
      var resources = realm.srcResources;
      var jarModule =
          new Command("jar")
              .add("--create")
              .add("--file", modularJar)
              .addIff(verbose, "--verbose")
              .add("-C", realm.binClasses.resolve(module))
              .add(".")
              .addIff(Files.isDirectory(resources), cmd -> cmd.add("-C", resources).add("."));
      var jarSources =
          new Command("jar")
              .add("--create")
              .add("--file", sourcesJar)
              .addIff(verbose, "--verbose")
              .add("--no-manifest")
              .add("-C", project.src.resolve(module).resolve(realm.name).resolve("java"))
              .add(".")
              .addIff(Files.isDirectory(resources), cmd -> cmd.add("-C", resources).add("."));
      if (runner.run(jarModule) != 0) {
        log("Creating " + realm.name + " modular jar failed: ", modularJar);
        return;
      }
      if (runner.run(jarSources) != 0) {
        log("Creating " + realm.name + " sources jar failed: ", sourcesJar);
        return;
      }
    }
    log("Bach::compile(realm=%s) end.", realm.name);
  }

  /** Print build summary. */
  public void summary() {
    log("Bach::summary()");
    var path = project.main.binModules;
    if (Files.notExists(path)) {
      out.println("No module destination directory created: " + path.toUri());
      return;
    }
    var jars = Util.findFiles(List.of(path), Util::isJarFile);
    if (jars.isEmpty()) {
      out.printf("No module created for %s%n", project.name);
      return;
    }
    if (verbose) {
      for (var jar : jars) {
        runner.run(new Command("jar", "--describe-module", "--file", jar));
      }
      runner.run(
          new Command(
              "jdeps",
              "--module-path",
              project.main.binModules,
              "--check",
              String.join(",", project.main.modules)));
    }
    out.printf("%d module(s) created for %s in %s:%n", jars.size(), project.name, path.toUri());
    for (var jar : jars) {
      var module = ModuleFinder.of(jar).findAll().iterator().next().descriptor();
      out.printf(" -> %9d %s <- %s%n", Util.size(jar), jar.getFileName(), module);
    }
    log("Bach::summary() end.");
  }

  /** Print the version string. */
  public void version() {
    out.println(VERSION);
  }

  /** Main-entry point converting strings to commands and executing each. */
  int main(List<String> arguments) {
    log("Bach::main(%s)", arguments);
    if (arguments.isEmpty()) {
      build();
      return 0;
    }
    var commands = runner.commands(arguments);
    return runner.run(commands);
  }

  /** Execute the named tool and throw an error the expected and actual exit values aren't equal. */
  void run(int expected, String name, Object... arguments) {
    log("Bach::run(%d, %s, %s)", expected, name, List.of(arguments));
    var actual = run(name, arguments);
    if (expected != actual) {
      var command = name + (arguments.length == 0 ? "" : " " + List.of(arguments));
      throw new Error("Expected " + expected + ", but got " + actual + " as result of: " + command);
    }
  }

  /** Execute the named tool and return its exit value. */
  int run(String name, Object... arguments) {
    log("Bach::run(%s, %s)", name, List.of(arguments));
    return runner.run(new Command(name, arguments));
  }

  /** Runtime context. */
  class Runner {

    /** Transmute strings to commands. */
    List<Command> commands(List<String> strings) {
      var commands = new ArrayList<Command>();
      var deque = new ArrayDeque<>(strings);
      while (!deque.isEmpty()) {
        var string = deque.removeFirst();
        if ("tool".equals(string)) {
          var tool = deque.removeFirst();
          commands.add(new Command(tool).addEach(deque));
          break;
        }
        commands.add(new Command(string));
      }
      return commands;
    }

    /** Run given list of of commands sequentially and fail-fast on non-zero result. */
    int run(List<Command> commands) {
      log("Running %s command(s): %s", commands.size(), commands);
      for (var command : commands) {
        var code = runner.run(command);
        if (code != 0) {
          return code;
        }
      }
      return 0;
    }

    /** Run given command. */
    int run(Command command) {
      return run(command.name, command.toStringArray());
    }

    /**
     * Run named tool, as loaded by {@link java.util.ServiceLoader} using the system class loader.
     */
    int run(String name, String... args) {
      var providedTool = ToolProvider.findFirst(name);
      if (providedTool.isPresent()) {
        var tool = providedTool.get();
        log("Running provided tool in-process: " + tool);
        return run(tool, args);
      }

      try {
        var method = Bach.class.getMethod(name); // no parameters
        log("Invoking instance method: " + method);
        var result = method.invoke(Bach.this); // no arguments
        return result instanceof Number ? ((Number) result).intValue() : 0;
      } catch (NoSuchMethodException e) {
        // fall-through
      } catch (ReflectiveOperationException e) {
        e.printStackTrace(err);
        return 1;
      }

      log("Starting new process for '%s'", name);
      var processBuilder = newProcessBuilder(name);
      processBuilder.command().addAll(List.of(args));
      return run(processBuilder);
    }

    /** Run provided tool. */
    int run(ToolProvider tool, String... args) {
      log("Bach::run(%s, %s)", tool.name(), String.join(", ", args));
      var code = tool.run(out, err, args);
      if (code == 0) {
        log("Tool '%s' successfully run.", tool.name());
      }
      return code;
    }

    /** Create new process builder for the given command and inherit IO from current process. */
    ProcessBuilder newProcessBuilder(String command) {
      var builder = new ProcessBuilder(command).inheritIO();
      builder.environment().put("BACH_VERSION", Bach.VERSION);
      builder.environment().put("BACH_HOME", home.toString());
      builder.environment().put("BACH_WORK", work.toString());
      return builder;
    }

    /** Start new process and wait for its termination. */
    int run(ProcessBuilder builder) {
      log("Bach::run(%s)", builder);
      try {
        var process = builder.start();
        var code = process.waitFor();
        if (code == 0) {
          log("Process '%s' successfully terminated.", process);
        }
        return code;
      } catch (Exception e) {
        throw new Error("Starting process failed: " + e);
      }
    }
  }

  /** Command-line program argument list builder. */
  static class Command {

    final String name;
    final List<String> list = new ArrayList<>();

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
      var stream = paths.stream().filter(Files::isDirectory).map(Object::toString);
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

    /** Let the consumer visit, usually modify, this instance iff the conditions is {@code true}. */
    Command addIff(boolean condition, Consumer<Command> visitor) {
      if (condition) {
        visitor.accept(this);
      }
      return this;
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

  /** Project information. */
  class Project {
    final String name = home.toAbsolutePath().normalize().getFileName().toString();
    final String version = System.getProperty("bach.project.version", "1.0.0-SNAPSHOT");

    final Path bin = work.resolve(Path.of(System.getProperty("bach.bin", "bin")));
    final Path src = home.resolve(Path.of(System.getProperty("bach.src", "src")));
    final List<String> modules = Util.findDirectoryNames(src);

    final Realm main = new Realm("main");
    final Realm test = new Realm("test", main.binModules);

    void toStrings(Consumer<String> consumer) {
      consumer.accept("Project properties");
      consumer.accept("  name = " + name);
      consumer.accept("  version = " + version);
      consumer.accept("  modulesDirectory = " + src);
      consumer.accept("  modules = " + modules);
      main.toStrings(consumer);
      test.toStrings(consumer);
    }

    /** Project realm: main or test. */
    class Realm {
      final String name;
      final String moduleSourcePath;
      final List<String> modules;
      final List<Path> modulePath;
      final boolean preview;
      final String release;
      final Path srcResources;
      final Path binClasses;
      final Path binModules;
      final Path binSources;

      Realm(String name, Path... initialModulePath) {
        this.name = name;
        this.modulePath = List.of(initialModulePath);
        this.preview = Boolean.getBoolean("bach." + name + ".preview");
        this.release = System.getProperty("bach." + name + ".release", null);

        this.srcResources = src.resolve(name).resolve("resources");
        this.binClasses = bin.resolve(name).resolve("compiled").resolve("classes");
        this.binModules = bin.resolve(name).resolve("modules");
        this.binSources = bin.resolve(name).resolve("sources");

        if (Files.notExists(src)) {
          this.moduleSourcePath = src.toString();
          this.modules = List.of();
          return;
        }
        var modules = new ArrayList<String>();
        var moduleSourcePaths = new TreeSet<String>();
        var declarations = Util.findFiles(List.of(src), Util::isModuleInfo);
        for (var declaration : declarations) {
          var relative = src.relativize(declaration); //  <module>/<realm>/.../module-info.java
          var module = relative.getName(0).toString();
          var realm = relative.getName(1).toString();
          if (!name.equals(realm)) {
            continue;
          }
          modules.add(module);
          var offset = relative.subpath(1, relative.getNameCount() - 1);
          moduleSourcePaths.add(String.join(File.separator, src.toString(), "*", offset.toString()));
        }
        this.moduleSourcePath = String.join(File.pathSeparator, moduleSourcePaths);
        this.modules = List.copyOf(modules);
      }

      void toStrings(Consumer<String> consumer) {
        var prefix = "  " + name + '.';
        consumer.accept("Realm '" + name + "' properties");
        consumer.accept(prefix + "modules = " + modules);
        consumer.accept(prefix + "moduleSourcePath = " + moduleSourcePath);
      }
    }
  }

  /** Static helpers. */
  static class Util {

    /** Download a file denoted by the specified uri. */
    static Path download(Path destination, URI uri, boolean offline) throws Exception {
      // run.log(TRACE, "Downloader::download(%s)", uri);
      var fileName = extractFileName(uri);
      var target = Files.createDirectories(destination).resolve(fileName);
      var url = uri.toURL(); // fails for non-absolute uri
      if (offline) {
        // run.log(DEBUG, "Offline mode is active!");
        if (Files.exists(target)) {
          // var file = target.getFileName().toString();
          // run.log(DEBUG, "Target already exists: %s, %d bytes.", file, size(target));
          return target;
        }
        var message = "Offline mode is active and target is missing: " + target;
        // run.log(ERROR, message);
        throw new IllegalStateException(message);
      }
      return download(destination, url.openConnection());
    }

    /** Download a file using the given URL connection. */
    static Path download(Path destination, URLConnection connection) throws Exception {
      var millis = connection.getLastModified(); // 0 means "unknown"
      var lastModified = FileTime.fromMillis(millis == 0 ? System.currentTimeMillis() : millis);
      // run.log(TRACE, "Remote was modified on %s", lastModified);
      var target = destination.resolve(extractFileName(connection));
      // run.log(TRACE, "Local target file is %s", target.toUri());
      // var file = target.getFileName().toString();
      if (Files.exists(target)) {
        var fileModified = Files.getLastModifiedTime(target);
        // run.log(TRACE, "Local last modified on %s", fileModified);
        if (fileModified.equals(lastModified)) {
          // run.log(TRACE, "Timestamp match: %s, %d bytes.", file, size(target));
          connection.getInputStream().close(); // release all opened resources
          return target;
        }
        // run.log(DEBUG, "Local target file differs from remote source -- replacing it...");
      }
      try (var sourceStream = connection.getInputStream()) {
        try (var targetStream = Files.newOutputStream(target)) {
          // run.log(DEBUG, "Transferring %s...", file);
          sourceStream.transferTo(targetStream);
        }
        Files.setLastModifiedTime(target, lastModified);
      }
      // run.log(DEBUG, "Downloaded %s, %d bytes.", file, Files.size(target));
      return target;
    }

    /** Extract last path element from the supplied uri. */
    static String extractFileName(URI uri) {
      var path = uri.getPath(); // strip query and fragment elements
      return path.substring(path.lastIndexOf('/') + 1);
    }

    /** Extract target file name either from 'Content-Disposition' header or. */
    static String extractFileName(URLConnection connection) {
      var contentDisposition = connection.getHeaderField("Content-Disposition");
      if (contentDisposition != null && contentDisposition.indexOf('=') > 0) {
        return contentDisposition.split("=")[1].replaceAll("\"", "");
      }
      try {
        return extractFileName(connection.getURL().toURI());
      } catch (URISyntaxException e) {
        throw new Error("URL connection returned invalid URL?!", e);
      }
    }

    /** List names of all directories found in given directory. */
    static List<String> findDirectoryNames(Path directory) {
      return findDirectoryEntries(directory, Files::isDirectory);
    }

    /** List paths of all entries found in given directory after applying the filter. */
    static List<String> findDirectoryEntries(Path directory, DirectoryStream.Filter<Path> filter) {
      var names = new ArrayList<String>();
      try (var stream = Files.newDirectoryStream(directory, filter)) {
        stream.forEach(entry -> names.add(entry.getFileName().toString()));
      } catch (Exception e) {
        throw new Error("Scanning directory entries failed: " + e);
      }
      return names;
    }

    /** List paths of all entries found in given directory after applying the glob pattern. */
    static List<Path> findDirectoryEntries(Path directory, String glob) {
      var paths = new ArrayList<Path>();
      try (var stream = Files.newDirectoryStream(directory, glob)) {
        stream.forEach(paths::add);
      } catch (Exception e) {
        throw new Error("Scanning directory entries failed: " + e, e);
      }
      return paths;
    }

    /** List all regular files matching the given filter. */
    static List<Path> findFiles(Collection<Path> roots, Predicate<Path> filter) {
      var files = new ArrayList<Path>();
      for (var root : roots) {
        try (var stream = Files.walk(root)) {
          stream.filter(Files::isRegularFile).filter(filter).forEach(files::add);
        } catch (Exception e) {
          throw new Error("Scanning directory '" + root + "' failed: " + e, e);
        }
      }
      return files;
    }

    /** List all regular Java files in given root directory. */
    static List<Path> findJavaFiles(Path root) {
      return findFiles(List.of(root), Util::isJavaFile);
    }

    /** Test supplied path for pointing to a Java source compilation unit. */
    static boolean isJavaFile(Path path) {
      if (Files.isRegularFile(path)) {
        var name = path.getFileName().toString();
        if (name.endsWith(".java")) {
          return name.indexOf('.') == name.length() - 5; // single dot in filename
        }
      }
      return false;
    }

    /** Test supplied path for pointing to a Java source compilation unit. */
    static boolean isJarFile(Path path) {
      return Files.isRegularFile(path) && path.getFileName().toString().endsWith(".jar");
    }

    /** Test supplied path for pointing to a Java module declaration source compilation unit. */
    static boolean isModuleInfo(Path path) {
      return Files.isRegularFile(path) && path.getFileName().toString().equals("module-info.java");
    }

    /** Join supplied paths into a single string joined by current path separator. */
    static String join(Collection<?> paths) {
      return paths.stream().map(Object::toString).collect(Collectors.joining(File.pathSeparator));
    }

    /** Join supplied paths into a single string joined by current path separator. */
    static String join(Path path, Path... more) {
      if (more.length == 0) {
        return path.toString();
      }
      var strings = new String[1 + more.length];
      strings[0] = path.toString();
      for (var i = 0; i < more.length; i++) {
        strings[1 + i] = more[i].toString();
      }
      return String.join(File.pathSeparator, strings);
    }

    /** Returns the size of a file in bytes. */
    static long size(Path path) {
      try {
        return Files.size(path);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    /** Delete all files and directories from and including the root directory. */
    static void treeDelete(Path root) throws Exception {
      treeDelete(root, __ -> true);
    }

    /** Delete selected files and directories from and including the root directory. */
    static void treeDelete(Path root, Predicate<Path> filter) throws Exception {
      // trivial case: delete existing empty directory or single file
      if (filter.test(root)) {
        try {
          Files.deleteIfExists(root);
          return;
        } catch (DirectoryNotEmptyException ignored) {
          // fall-through
        }
      }
      // default case: walk the tree...
      try (var stream = Files.walk(root)) {
        var selected = stream.filter(filter).sorted((p, q) -> -p.compareTo(q));
        for (var path : selected.collect(Collectors.toList())) {
          Files.deleteIfExists(path);
        }
      }
    }
  }
}
