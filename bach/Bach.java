/*
 * Java Shell Builder
 * Copyright (C) 2017 Christian Stein
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

// no package

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.function.*;
import java.util.logging.*;
import java.util.spi.*;
import java.util.stream.*;
import java.util.regex.*;

/** Source directory module tree layout/scheme. */
enum Layout {
  /** Auto-detect at configuration time. */
  AUTO,
  /** Module folder first, no tests: {@code src/<module>} */
  BASIC,
  /** Module folders first: {@code src/<module>/[main|test]/[java|resources]} */
  FIRST,
  /** Module folders last: {@code src/[main|test]/[java|resources]/<module>} */
  TRAIL,
}

enum Folder {
  JDK_HOME("jdk-9"),
  JDK_HOME_BIN(JDK_HOME, "bin"),
  JDK_HOME_MODS(JDK_HOME, "jmods"),
  AUXILIARY(".bach"),
  DEPENDENCIES(AUXILIARY, "dependencies"),
  TOOLS(AUXILIARY, "tools"),
  SOURCE("src"),
  MAIN_JAVA("main/java"),
  MAIN_RESOURCES("main/resources"),
  TEST_JAVA("test/java"),
  TEST_RESOURCES("test/resources"),
  TARGET("target/bach"),
  TARGET_MAIN_SOURCE(TARGET, "main/module-source-path"),
  TARGET_MAIN_COMPILED(TARGET, "main/compiled"),
  TARGET_MAIN_JAR(TARGET, "main/archives"),
  TARGET_TEST_SOURCE(TARGET, "test/module-source-path"),
  TARGET_TEST_COMPILED(TARGET, "test/compiled"),
  ;

  final Folder parent;
  final Path path;

  Folder(String path) {
    this(null, path);
  }

  Folder(Folder parent, String path) {
    this.parent = parent;
    this.path = Paths.get(path);
  }
}

enum Tool {
  FORMAT("https://jitpack.io/com/github/sormuras/google-java-format/google-java-format/validate-SNAPSHOT/google-java-format-validate-SNAPSHOT-all-deps.jar"),
  JUNIT("http://central.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.0.0-M4/junit-platform-console-standalone-1.0.0-M4.jar");
  URI uri;
  Tool(String uri) {
    this.uri = URI.create(uri);
  }
}

/**
 * Java Shell Builder.
 *
 * @noinspection WeakerAccess, UnusedReturnValue, unused
 */
public class Bach {

  /** Immutable configuration, setup, partition...*/
  public static class Score {

    final Charset charset;
    final String name;
    final String version;
    final Level level;
    final Layout layout;
    final Map<Folder, Path> folders;
    final Map<String, String> mains;
    final InputStream streamIn;
    final PrintStream streamOut;
    final PrintStream streamErr;

    Score(Builder builder) {
      this.charset = builder.charset;
      this.name = builder.name;
      this.version = builder.version;
      this.level = builder.level;
      this.layout = builder.layout;
      this.folders = Collections.unmodifiableMap(new EnumMap<>(builder.folders));
      this.mains = Collections.unmodifiableMap(new HashMap<>(builder.moduleEntryPointMap));
      this.streamIn = builder.streamIn;
      this.streamOut = builder.streamOut;
      this.streamErr = builder.streamErr;
    }

  }

  public static Builder builder() {
    return new Builder();
  }

  public static Bach create() {
    return builder().build();
  }

  private final Log log;
  private final Util util;
  private final Score score;

  public Bach(Score score) {
    this.score = score;
    this.log = new Log();
    this.util = new Util();
    log.info("project %s [%s] initialized%n", score.name, getClass());
    log.print(Level.CONFIG, "version=%s%n", score.version);
    log.print(Level.CONFIG, "level=%s%n", score.level);
    log.print(Level.CONFIG, "pwd=`%s`%n", Paths.get(".").toAbsolutePath().normalize());
    log.print(Level.CONFIG, "layout=%s%n", score.layout);
    log.print(Level.FINEST, "folder %s%n", score.folders.keySet());
  }

  public Path path(Folder folder) {
    return score.folders.get(folder);
  }

  public Path path(Tool tool) {
    return util.tools.computeIfAbsent(tool, util::load);
  }

  public Bach set(Level level) {
    log.level(level);
    return this;
  }

  public Bach clean() throws Exception {
    clean(Folder.TARGET);
    return this;
  }

  public Bach clean(Folder... folders) throws Exception {
    log.tag("clean");
    for (Folder folder : folders) {
      util.cleanTree(path(folder), false);
    }
    return this;
  }

  public Bach load(String module, URI uri) throws Exception {
    util.download(uri, path(Folder.DEPENDENCIES), module + ".jar", path -> true);
    return this;
  }

  public Bach compile() throws Exception {
    log.tag("compile").print(Level.CONFIG, "folder %s%n", score.folders.keySet());
    Path modules = path(Folder.SOURCE);
    log.check(Files.exists(modules),"folder source `%s` does not exist", modules);
    log.check(util.findDirectoryNames(modules).count() > 0, "no directory found in `%s`", modules);
    util.cleanTree(path(Folder.TARGET), true);
    switch (score.layout) {
      case BASIC:
        log.info("main%n");
        compile(modules, path(Folder.TARGET_MAIN_COMPILED));
        util.copyTree(modules, path(Folder.TARGET_MAIN_COMPILED)); // TODO exclude .java files?
        break;
      case FIRST:
        util.findDirectoryNames(modules).forEach(module -> {
          log.fine("module %s%n", module);
          Path source = modules.resolve(module);
          // main
          util.copyTree(source.resolve(path(Folder.MAIN_JAVA)), path(Folder.TARGET_MAIN_SOURCE).resolve(module));
          util.copyTree(source.resolve(path(Folder.MAIN_RESOURCES)), path(Folder.TARGET_MAIN_COMPILED).resolve(module));
          // test
          util.copyTree(source.resolve(path(Folder.MAIN_JAVA)), path(Folder.TARGET_TEST_SOURCE).resolve(module));
          util.copyTree(source.resolve(path(Folder.TEST_JAVA)), path(Folder.TARGET_TEST_SOURCE).resolve(module));
          util.moveModuleInfo(path(Folder.TARGET_TEST_SOURCE).resolve(module));
          util.copyTree(source.resolve(path(Folder.TEST_RESOURCES)), path(Folder.TARGET_TEST_COMPILED).resolve(module));
        });
        log.info("main%n");
        compile(path(Folder.TARGET_MAIN_SOURCE), path(Folder.TARGET_MAIN_COMPILED));
        if (Files.exists(path(Folder.TARGET_TEST_SOURCE))) {
          log.info("test%n");
          compile(path(Folder.TARGET_TEST_SOURCE), path(Folder.TARGET_TEST_COMPILED));
        }
        break;
      case TRAIL:
        log.info("main%n");
        compile(modules.resolve(path(Folder.MAIN_JAVA)), path(Folder.TARGET_MAIN_COMPILED));
        util.copyTree(modules.resolve(path(Folder.MAIN_RESOURCES)), path(Folder.TARGET_MAIN_COMPILED));
        if (Files.exists(modules.resolve(path(Folder.TEST_JAVA)))) {
          log.info("test%n");
          util.copyTree(modules.resolve(path(Folder.MAIN_JAVA)), path(Folder.TARGET_TEST_SOURCE));
          util.copyTree(modules.resolve(path(Folder.TEST_JAVA)), path(Folder.TARGET_TEST_SOURCE));
          compile(path(Folder.TARGET_TEST_SOURCE), path(Folder.TARGET_TEST_COMPILED));
          util.copyTree(modules.resolve(path(Folder.TEST_RESOURCES)), path(Folder.TARGET_TEST_COMPILED));
        }
        break;
      default:
        log.check(false, "unsupported module source path layout %s for: `%s`", score.layout, modules);
    }
    return this;
  }

  public Bach compile(Path moduleSourcePath, Path destinationPath) throws Exception {
    log.tag("compile");
    log.check(Files.exists(moduleSourcePath), "module source path `%s` does not exist", moduleSourcePath);
    Command command = Command.of("javac")
    // output messages about what the compiler is doing
        .add(log.threshold <= Level.FINEST.intValue(), "-verbose")
    // file encoding
        .add("-d")
        .add(destinationPath.toString())
    // output source locations where deprecated APIs are used
        .add("-deprecation")
    // generate metadata for reflection on method parameters
        .add("-parameters")
    // terminate compilation if warnings occur
        .add("-Werror")
    // specify character encoding used by source files
        .add("-encoding")
        .add(score.charset.name())
    // specify where to find application modules
        .add("--module-path")
        .add(path(Folder.DEPENDENCIES))
    // specify where to find input source files for multiple modules
        .add("--module-source-path")
        .add(moduleSourcePath);
    // collect .java source files
    command.limit(10);
    int[] count = {0};
    Files.walk(moduleSourcePath)
        .map(Path::toString)
        .filter(name -> name.endsWith(".java"))
        .peek(name -> count[0]++)
        .forEach(command::add);
    // compile
    execute(command);
    return this;
  }

  public Bach run(String module, String main, String... arguments) {
    return run(Folder.TARGET_MAIN_COMPILED, module, main, arguments);
  }

  public Bach runCompiled(String module, String... arguments) {
    return run(Folder.TARGET_MAIN_COMPILED, module, score.mains.getOrDefault(module, module + ".Main"), arguments);
  }

  public Bach runJar(String module, String... arguments) {
    return run(Folder.TARGET_MAIN_JAR, module, score.mains.get(module), arguments);
  }

  public Bach run(Folder folder, String module, String main, String... arguments) {
    String entryPoint = main == null ? module : module + "/" + main;
    log.tag("run").info("%s%n", entryPoint);
    return execute(Command.of("java")
    // options
        .add("-Dfile.encoding=" + score.charset.name())
    // module-path: a list of directories in which each directory is a directory of modules
        .add("--module-path")
        .add(this::path, folder, Folder.DEPENDENCIES)
    // name of the initial module to resolve and execute
        .add("--module")
        .add(entryPoint)
    // set internal dump mark and limit
        .limit(20)
    // arguments passed to the main method separated by spaces
        .addAll((Object[]) arguments));
  }

  public Bach execute(String executable, Object... command) {
    log.tag("execute");
    return execute(Command.of(executable).addAll(command));
  }

  public Bach execute(Command command) {
    command.dump(log, Level.FINE);
    try {
      long start = System.currentTimeMillis();
      int exitValue;
      Optional<ToolProvider> tool = ToolProvider.findFirst(command.name);
      if (tool.isPresent()) {
        log.fine("running provided `%s` tool in-process...%n", command.name);
        exitValue = tool.get().run(score.streamOut, score.streamErr, command.toArray());
      } else {
        log.fine("starting external `%s` tool in new process...%n", command.name);
        Process process = command.newProcessBuilder().redirectErrorStream(true).start();
        process.getInputStream().transferTo(score.streamOut);
        exitValue = process.waitFor();
      }
      log.fine("%s finished after %d ms%n", command.name, System.currentTimeMillis() - start);
      if (exitValue != 0) {
        log.fail(RuntimeException::new, "exit value of %d indicates an error", exitValue);
      }
    } catch (Exception e) {
      if (log.isLevelSuppressed(Level.FINE)) {
        command.dump(log, Level.SEVERE);
      }
      log.fail(e, "execute(command `%s`) failed", command.name);
    }
    return this;
  }

  public Bach test(String... options) throws Exception {
    return test(true, options);
  }

  public Bach test(boolean additional, String... options) throws Exception {
    log.tag("test");
    List<String> modules = util.findDirectoryNames(path(Folder.TARGET_TEST_COMPILED)).collect(Collectors.toList());
    for (String module : modules) {
      test(module, additional, options);
    }
    return this;
  }

  public Bach test(String module, boolean additional, String... options) throws Exception {
    log.tag("test").info("module %s%n", module);
    Path modulePath = path(Folder.TARGET_TEST_COMPILED).resolve(module);
    Command command = Command.of(path(Folder.JDK_HOME_BIN), "java")
            .add("-ea")
            .add("-Dfile.encoding=" + score.charset.name())
            .add("-jar")
            .add(path(Tool.JUNIT));
    util.findDirectories(path(Folder.TARGET_TEST_COMPILED))
            .forEach(m -> command.add(additional, "--classpath").add(additional, m));
    command.add(additional, "--scan-classpath")
            .add(additional, modulePath)
            .limit(10)
            .addAll((Object[]) options);
    return execute(command);
  }

  public Bach format() throws Exception {
    return format(true);
  }

  public Bach format(boolean validate) throws Exception {
    return format(validate, path(Folder.SOURCE));
  }

  public Bach format(boolean validate, Path... paths) throws Exception {
    log.tag("format");
    Command command = Command.of(path(Folder.JDK_HOME_BIN), "java")
        .add("-jar")
        .add(path(Tool.FORMAT))
        .add(validate ? "--validate" : "--replace")
        .limit(10);
    // collect valid .java source files
    Predicate<Path> validJavaFilePath = path -> {
      String name = path.getFileName().toString();
      if (name.equals("module-info.java")) {
        return false; // see https://github.com/google/google-java-format/issues/75
      }
      //noinspection SimplifiableIfStatement
      if (name.chars().filter(c -> c == '.').count() != 1) {
        return false;
      }
      return name.endsWith(".java");
    };
    int[] count = {0};
    for (Path path : paths) {
      log.fine("%s `%s`...%n", validate ? "validating" : "formatting", path);
      Files.walk(path)
              .filter(validJavaFilePath)
              .map(Path::toString)
              .peek(name -> count[0]++)
              .forEach(command::add);
      execute(command);
    }
    log.info("%d files %s%n", count[0], validate ? "validated" : "formatted");
    return this;
  }

  public Bach jar() throws Exception {
    log.tag("jar");
    List<String> modules = util.findDirectoryNames(path(Folder.TARGET_MAIN_COMPILED)).collect(Collectors.toList());
    for (String module : modules) {
      Path modulePath = path(Folder.TARGET_MAIN_COMPILED).resolve(module);
      String main = score.mains.getOrDefault(module, module + "." + "Main");
      if (Files.notExists(modulePath.resolve(main.replace('.', '/') + ".class"))) {
        if (score.mains.containsKey(module)) {
          log.fail(IllegalStateException::new, "entry-point `%s` not found in `%s`", main, modulePath);
        }
        main = null;
      }
      jar(module + ".jar", module, main, score.version);
    }
    return this;
  }

  public Bach jar(String fileName, String module, String main, String version) throws Exception {
    log.tag("jar");
    Files.createDirectories(path(Folder.TARGET_MAIN_JAR));
    Path jarFile = path(Folder.TARGET_MAIN_JAR).resolve(fileName);
    Command command = Command.of("jar")
            // output messages about what the jar command is doing
            .add(log.threshold <= Level.FINEST.intValue(), "--verbose")
            // creates the archive
            .add("--create")
            // specifies the archive file name
            .add("--file").add(jarFile);
            if (main != null && !main.isEmpty()) {
              // specifies the application entry point for stand-alone applications
              command.add("--main-class").add(main);
            }
            if (version != null && !version.isEmpty()) {
              // specifies the module version
              command.add("--module-version").add(version);
            }
            // changes to the specified directory and includes the files specified at the end of the command line
            command.add("-C").add(path(Folder.TARGET_MAIN_COMPILED).resolve(module)).add(".");
    ToolProvider jarTool = ToolProvider.findFirst("jar").orElseThrow(() -> new IllegalStateException("can not find tool: jar"));
    jarTool.run(score.streamOut, score.streamErr, command.toArray());

    if (log.isLevelActive(Level.FINE)) {
      jarTool.run(score.streamOut, score.streamErr, "--describe-module", "--file", jarFile.toString());
    }
    return this;
  }

  /**
   * Assemble and optimize a set of modules and their dependencies into a custom runtime image.
   */
  public Bach link(String module, String name) {
    log.tag("link").info("%s%n", name);
    Path target = path(Folder.TARGET).resolve(name);
    Command command = Command.of("jlink")
            // specifies the module path
            .add("--module-path")
            .add(this::path, Folder.TARGET_MAIN_JAR, Folder.DEPENDENCIES, Folder.JDK_HOME_MODS)
            // adds the named module to the default set of root modules (the default set of root modules is empty)
            .add("--add-modules")
            .add(module)
            // specifies the launcher command name for the module
            .add("--launcher")
            .add(name + "=" + module)
            // excludes header files
            .add("--no-header-files")
            // excludes man pages
            .add("--no-man-pages")
            // strip debug information from the output image
            .add("--strip-debug")
            // exclude native commands (such as java or java.exe) from the image
            // .add("--strip-native-commands")
            // compress all resources in the output image: 0=no compression, 1=constant string sharing, 2=ZIP
            .add("--compress")
            .add(2)
            // saves options in the specified file
            .add("--save-opts")
            .add(target.resolve("jlink.options.txt"))
            // specifies the location of the generated runtime image
            .add("--output")
            .add(target);
    execute(command);
    return this;
  }

  public Bach visit(Visitor visitor) {
    log.tag("visit");
    try {
      visitor.visit(this);
    }
    catch (Exception e) {
      log.fail(e, "visit failed");
    }
    return this;
  }

  @FunctionalInterface
  interface Visitor {
    void visit(Bach bach) throws Exception;
  }

  class Log {
    int threshold;
    String tag;
    final PrintStream out;

    Log() {
      this.out = Bach.this.score.streamOut;
      level(Bach.this.score.level);
      tag("init");
    }

    Log level(Level level) {
      this.threshold = level.intValue();
      return this;
    }

    Log tag(String tag) {
      if (Objects.equals(this.tag, tag)) {
        return this;
      }
      this.tag = tag;
      print(Level.CONFIG,"%n");
      return this;
    }

    void check(boolean condition, String format, Object...args) {
      if (!condition) log.fail(AssertionError::new, format, args);
    }

    <T> T assigned(T instance, String format, Object...args) {
      if (instance == null) log.fail(NullPointerException::new, format, args);
      return instance;
    }

    private void print(String format, Object... args) {
      score.streamOut.printf(format, args);
    }

    private void printTag(Level level) {
      print("%7s ", tag);
      if (threshold < Level.INFO.intValue()) {
        print("%6s| ", level.getName().toLowerCase());
      }
    }

    boolean isLevelActive(Level level) {
      return !isLevelSuppressed(level);
    }

    boolean isLevelSuppressed(Level level) {
      return level.intValue() < threshold;
    }

    void print(Level level, String format, Object... args) {
      if (isLevelSuppressed(level)) {
        return;
      }
      if (args.length == 1 && args[0] instanceof Collection) {
        for (Object arg : (Iterable<?>) args[0]) {
          printTag(level);
          if (arg instanceof Folder) {
            arg = arg + " -> " + path((Folder) arg);
          }
          print(format, arg);
        }
        return;
      }
      printTag(level);
      print(format, args);
    }

    void fine(String format, Object... args) {
      print(Level.FINE, format, args);
    }

    void info(String format, Object... args) {
      print(Level.INFO, format, args);
    }

    <T> T fail(Function<String, Throwable> supplier, String format, Object... args) {
      return fail(supplier.apply(String.format(format, args)), format, args);
    }

    <T> T fail(Throwable cause, String format, Object... args) {
      String message = String.format(format, args);
      print(Level.SEVERE, message + "%n");
      throw new Error(message, cause);
    }
  }

  class Util {

    EnumMap<Tool, Path> tools = new EnumMap<>(Tool.class);

    Path load(Tool tool) {
      Path toolPath = path(Folder.TOOLS).resolve(tool.name().toLowerCase());
      try {
        return util.download(tool.uri, toolPath);
      }
      catch (Exception e) {
        log.fail(e, "loading tool %s from uri %s failed", tool, tool.uri);
        throw new Error(e);
      }
    }

    void deleteIfExists(Path path) {
      try {
        Files.deleteIfExists(path);
      } catch (Exception e) {
        throw new AssertionError("should not happen", e);
      }
    }

    Stream<Path> findDirectories(Path root) throws Exception {
      return Files.find(root, 1, (path, attr) -> Files.isDirectory(path))
              .filter(path -> !root.equals(path));
    }

    Stream<String> findDirectoryNames(Path root) throws Exception {
      return findDirectories(root)
              .map(root::relativize)
              .map(Path::toString);
    }

    Path cleanTree(Path root, boolean keepRoot) throws Exception {
      return cleanTree(root, keepRoot, __ -> true);
    }

    Path cleanTree(Path root, boolean keepRoot, Predicate<Path> filter) throws Exception {
      if (Files.notExists(root)) {
        if (keepRoot) {
          Files.createDirectories(root);
        }
        return root;
      }
      Files.walk(root)
          .filter(p -> !(keepRoot && root.equals(p)))
          .filter(filter)
          .sorted((p, q) -> -p.compareTo(q))
          .forEach(this::deleteIfExists);
      log.fine("deleted tree `%s`%n", root);
      return root;
    }

    void copyTree(Path source, Path target) {
      if (!Files.exists(source)) {
        return;
      }
      log.fine("copy `%s` to `%s`%n", source, target);
      try {
        Files.createDirectories(target);
        Files.walkFileTree(source, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
            new SimpleFileVisitor<>() {
              @Override
              public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes bfa) throws IOException {
                Path targetdir = target.resolve(source.relativize(dir));
                try {
                  Files.copy(dir, targetdir);
                } catch (FileAlreadyExistsException e) {
                  if (!Files.isDirectory(targetdir)) {
                    throw e;
                  }
                }
                return FileVisitResult.CONTINUE;
              }

              @Override
              public FileVisitResult visitFile(Path file, BasicFileAttributes bfa) throws IOException {
                Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
              }
            });
      } catch (Exception e) {
        log.fail(e, "copying `%s` to `%s` failed", source, target);
      }
    }

    /**
     * Download the resource specified by its URI to the target directory.
     */
    Path download(URI uri, Path targetDirectory) throws Exception {
      return download(uri, targetDirectory, fileName(uri), targetPath -> true);
    }

    /**
     * Download the resource specified by its URI to the target directory using the provided file name.
     */
    Path download(URI uri, Path targetDirectory, String targetFileName, Predicate<Path> useTimeStamp) throws Exception {
      URL url = log.assigned(uri, "uri").toURL();
      log.assigned(targetDirectory, "targetDirectory");
      if (log.assigned(targetFileName, "targetFileName").isEmpty()) {
        throw new IllegalArgumentException("targetFileName must not be blank");
      }
      Files.createDirectories(targetDirectory);
      Path targetPath = targetDirectory.resolve(targetFileName);
      URLConnection urlConnection = url.openConnection();
      FileTime urlLastModifiedTime = FileTime.fromMillis(urlConnection.getLastModified());
      if (Files.exists(targetPath)) {
        if (Files.getLastModifiedTime(targetPath).equals(urlLastModifiedTime)) {
          if (Files.size(targetPath) == urlConnection.getContentLengthLong()) {
            if (useTimeStamp.test(targetPath)) {
              log.fine("download skipped - using `%s`%n", targetPath);
              return targetPath;
            }
          }
        }
        Files.delete(targetPath);
      }
      log.fine("download `%s` in progress...%n", uri);
      try (InputStream sourceStream = url.openStream(); OutputStream targetStream = Files.newOutputStream(targetPath)) {
        sourceStream.transferTo(targetStream);
      }
      Files.setLastModifiedTime(targetPath, urlLastModifiedTime);
      log.print(Level.CONFIG, "download `%s` completed%n", uri);
      log.info("stored `%s` [%s]%n", targetPath, urlLastModifiedTime.toString());
      return targetPath;
    }

    /**
     * Extract the file name from the uri.
     */
    String fileName(URI uri) {
      String urlString = uri.getPath();
      return urlString.substring(urlString.lastIndexOf('/') + 1).split("\\?")[0].split("#")[0];
    }

    void moveModuleInfo(Path path) {
      if (!Files.exists(path)) {
        return;
      }
      Path pathSource = path.resolve("module-info.test");
      if (!Files.exists(pathSource)) {
        return;
      }
      try {
        Files.move(pathSource, path.resolve("module-info.java"), StandardCopyOption.REPLACE_EXISTING);
      }
      catch(Exception e) {
        log.fail(e, "moving module-info failed for %s", path);
      }
      log.fine("moved `%s` to `%s`%n", pathSource, "module-info.java");
    }
  }

  static class Command {

    static Command of(String name) {
      return new Command(name);
    }

    static Command of(Path path, String name) {
      return new Command(path.resolve(name).toString());
    }

    final String name;
    final ArrayList<String> arguments = new ArrayList<>();
    int indentOff = Integer.MAX_VALUE;
    int dumpLimit = Integer.MAX_VALUE;

    Command(String name) {
      this.name = name;
    }

    Command add(Object value) {
      arguments.add(value.toString());
      return this;
    }

    Command add(boolean condition, Object value) {
      if (condition) {
        add(value);
      }
      return this;
    }

    Command addAll(Object... values) {
      for (Object value: values) {
        add(value);
      }
      return this;
    }

    Command add(Collection<?> collection, String separator) {
      return add(collection.stream(), separator);
    }

    Command add(Stream<?> stream, String separator) {
      return add(stream.map(Object::toString).collect(Collectors.joining(separator)));
    }

    Command add(Function<Folder, Path> mapper, Folder... folders) {
      return add(Arrays.stream(folders).map(mapper), File.pathSeparator);
    }

    List<String> arguments() {
      return arguments;
    }

    ProcessBuilder newProcessBuilder() {
      ArrayList<String> command = new ArrayList<>(1 + arguments.size());
      command.add(name);
      command.addAll(arguments);
      return new ProcessBuilder(command);
    }

    String[] toArray() {
      return arguments.toArray(new String[arguments.size()]);
    }

    Command limit(int limit) {
      this.indentOff = arguments.size();
      this.dumpLimit = arguments.size() + limit;
      return this;
    }

    Command dump(Log log, Level level) {
      if (log.isLevelSuppressed(level)) {
        return this;
      }
      return dump((format, args) -> log.print(level, format, args));
    }

    private static Object[] args(Object... args) {
      return args;
    }

    Command dump(BiConsumer<String, Object[]> printer) {
      ListIterator<String> iterator = arguments.listIterator();
      printer.accept("%s%n", args(name));
      while (iterator.hasNext()) {
        String argument = iterator.next();
        int nextIndex = iterator.nextIndex();
        String indent = nextIndex > indentOff || argument.startsWith("-") ? "" : "  ";
        printer.accept("%s%s%n", args(indent, argument));
        if (nextIndex >= dumpLimit) {
          printer.accept("%s... [omitted %d arguments]%n", args(indent, arguments.size() - nextIndex - 1));
          printer.accept("%s%s%n", args(indent, arguments.get(arguments.size() - 1)));
          break;
        }
      }
      return this;
    }
  }

  static class Builder {

    private static final Path UNDEFINED_PATH = Paths.get(".");

    Level level = Level.INFO;
    String name = Paths.get(".").toAbsolutePath().normalize().getFileName().toString();
    Path jdkHome = UNDEFINED_PATH;
    String version = "";
    Layout layout = Layout.AUTO;
    Map<Folder, Path> folders = Collections.emptyMap();
    private Map<Folder, Path> override = new EnumMap<>(Folder.class);
    InputStream streamIn = System.in;
    PrintStream streamOut = System.out;
    PrintStream streamErr = System.err;

    Bach build() {
      return new Bach(score());
    }

    Charset charset = StandardCharsets.UTF_8; // Charset.forName(System.getProperty("file.encoding", "UTF-8"))

    Builder charset(Charset charset) {
      this.charset = charset;
      return this;
    }

    Score score() {
      if (jdkHome == UNDEFINED_PATH) {
        jdkHome = buildJdkHome();
      }
      override(Folder.JDK_HOME, jdkHome);
      if (folders == Collections.EMPTY_MAP) {
        folders = buildFolderPathMap(override);
      }
      if (!folders.keySet().equals(Set.of(Folder.values()))) {
        throw new AssertionError("key set mismatch in folders=" + folders);
      }
      if (layout == Layout.AUTO) {
        layout = buildLayout(folders.get(Folder.SOURCE));
      }
      return new Score(this);
    }

    Builder peek(Consumer<Builder> consumer) {
      consumer.accept(this);
      return this;
    }

    Builder name(String name) {
      this.name = name;
      return this;
    }

    Builder version(String version) {
      this.version = version;
      return this;
    }

    Builder log(Level level) {
      this.level = level;
      return this;
    }

    Builder layout(Layout layout) {
      this.layout = layout;
      return this;
    }

    Builder folders(Map<Folder, Path> folders) {
      this.folders = folders;
      return this;
    }

    Builder override(Folder folder, Path path) {
      override.put(folder, path);
      return this;
    }

    final Map<String, String> moduleEntryPointMap = new HashMap<>();

    Builder main(String module, String main) {
      this.moduleEntryPointMap.put(module, main);
      return this;
    }

    static Path buildJdkHome() {
      // try current process information: <JDK_HOME>/bin/java[.exe]
      Path executable = ProcessHandle.current().info().command().map(Paths::get).orElse(null);
      if (executable != null) {
        Path path = executable.getParent(); // <JDK_HOME>/bin
        if (path != null) {
          return path.getParent(); // <JDK_HOME>
        }
      }
      // next, examine system environment...
      String jdkHome = System.getenv("JDK_HOME");
      if (jdkHome != null) {
        return Paths.get(jdkHome);
      }
      String javaHome = System.getenv("JAVA_HOME");
      if (javaHome != null) {
        return Paths.get(javaHome);
      }
      // still here? not so good... try with default (not-existent) path
      return Folder.JDK_HOME.path;
    }

    static Map<Folder, Path> buildFolderPathMap(Map<Folder, Path> override) {
      Map<Folder, Path> map = new EnumMap<>(Folder.class);
      for (Folder folder : Folder.values()) {
        if (override != Collections.EMPTY_MAP && override.containsKey(folder)) {
          map.put(folder, override.get(folder));
          continue;
        }
        if (folder.parent == null) {
          map.put(folder, folder.path);
          continue;
        }
        Path path = map.getOrDefault(folder.parent, folder.parent.path);
        map.put(folder, path.resolve(folder.path));
      }
      return map;
    }

    static Layout buildLayout(Path root) {
      if (Files.notExists(root)) {
        return Layout.AUTO;
      }
      try {
        Path path = Files.find(root, 10, (p, a) -> p.endsWith("module-info.java"))
                .map(root::relativize)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no module descriptor found in " + root));
        // trivial case: <module>/module-info.java
        if (path.getNameCount() == 2) {
          return Layout.BASIC;
        }
        // nested case: extract module name and check whether the relative path starts with it
        String moduleSource = new String(Files.readAllBytes(root.resolve(path)), StandardCharsets.UTF_8);
        Pattern namePattern = Pattern.compile("(module)\\s+(.+)\\s*\\{.*");
        Matcher nameMatcher = namePattern.matcher(moduleSource);
        if (!nameMatcher.find()) {
          throw new IllegalArgumentException("expected java module descriptor unit, but got: \n" + moduleSource);
        }
        String moduleName = nameMatcher.group(2).trim();
        return path.startsWith(moduleName) ? Layout.FIRST : Layout.TRAIL;
      }
      catch (Exception e) {
        throw new Error("detection failed " + e, e);
      }
    }
  }
}

// https://github.com/sormuras/bach#raw-mode
//
//   To use "bach" in raw mode, uncomment the following command block and edit
//   it to your project's needs. Launch build with `jshell Bach.java`.

/*
{
Bach.builder()
    .override(Folder.SOURCE, Paths.get("source"))
  .build()
    .compile()
    .run("org.foo.bar", "org.foo.bar.Main");
}
/exit
*/
