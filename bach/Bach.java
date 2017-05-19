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
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.function.*;
import java.util.logging.*;
import java.util.stream.*;
import java.util.regex.*;
import javax.tools.*;

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
  AUXILIARY(".bach"),
  DEPENDENCIES(AUXILIARY, "dependencies"),
  TOOLS(AUXILIARY, "tools"),
  SOURCE("src"),
  MAIN_JAVA("main/java"),
  TEST_JAVA("test/java"),
  TARGET("target/bach"),
  TARGET_MAIN_SOURCE(TARGET, "main/module-source-path"),
  TARGET_MAIN_COMPILED(TARGET, "main/compiled"),
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

/**
 * Java Shell Builder.
 *
 * @noinspection WeakerAccess, UnusedReturnValue, unused
 */
public class Bach {

  /** Immutable configuration, setup, partition...*/
  public static class Score {

    final String name;
    final Level level;
    final Layout layout;
    final Map<Folder, Path> folders;
    final InputStream streamIn;
    final PrintStream streamOut;
    final PrintStream streamErr;

    Score(Builder builder) {
      this.name = builder.name;
      this.level = builder.level;
      this.layout = builder.layout;
      this.folders = Collections.unmodifiableMap(new EnumMap<>(builder.folders));
      this.streamIn = builder.streamIn;
      this.streamOut = builder.streamOut;
      this.streamErr = builder.streamErr;
    }

  }

  public static Builder builder() {
    return new Builder();
  }

  private final JavaCompiler javac;
  private final Log log;
  private final Util util;
  private final Score score;

  public Bach(Score score) {
    this.score = score;
    this.log = new Log();
    this.util = new Util();
    this.javac = log.assigned(ToolProvider.getSystemJavaCompiler(), "java compiler not available");
    log.info("project %s [%s] initialized%n", score.name, getClass());
    log.log(Level.CONFIG, "level=%s%n", score.level);
    log.log(Level.CONFIG, "pwd=`%s`%n", Paths.get(".").toAbsolutePath().normalize());
    log.log(Level.CONFIG, "layout=%s%n", score.layout);
    log.log(Level.FINEST, "folder %s%n", score.folders.keySet());
  }

  public Path path(Folder folder) {
    return score.folders.get(folder);
  }

  public Bach set(Level level) {
    log.level(level);
    return this;
  }

  public Bach clean() throws IOException {
    clean(Folder.TARGET);
    return this;
  }

  public Bach clean(Folder... folders) throws IOException {
    log.tag("clean");
    for (Folder folder : folders) {
      util.cleanTree(path(folder), false);
    }
    return this;
  }

  public Bach load(String module, URI uri) throws IOException {
    util.download(uri, path(Folder.DEPENDENCIES), module + ".jar", path -> true);
    return this;
  }


  public Bach compile() throws Exception {
    log.tag("compile").log(Level.CONFIG, "folder %s%n", score.folders.keySet());
    Path modules = path(Folder.SOURCE);
    log.check(Files.exists(modules),"folder source `%s` does not exist", modules);
    log.check(util.findDirectoryNames(modules).count() > 0, "no directory found in `%s`", modules);
    util.cleanTree(path(Folder.TARGET), true);
    switch (score.layout) {
      case BASIC:
        log.info("main%n");
        compile(modules, path(Folder.TARGET_MAIN_COMPILED));
        break;
      case FIRST:
        util.findDirectoryNames(modules).forEach(module -> {
          log.log(Level.FINE, "module %s%n", module);
          Path source = modules.resolve(module);
          // main
          util.copyTree(source.resolve(path(Folder.MAIN_JAVA)), path(Folder.TARGET_MAIN_SOURCE).resolve(module));
          // test
          util.copyTree(source.resolve(path(Folder.MAIN_JAVA)), path(Folder.TARGET_TEST_SOURCE).resolve(module));
          util.copyTree(source.resolve(path(Folder.TEST_JAVA)), path(Folder.TARGET_TEST_SOURCE).resolve(module));
          util.moveModuleInfo(path(Folder.TARGET_TEST_SOURCE).resolve(module));
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
        if (Files.exists(modules.resolve(path(Folder.TEST_JAVA)))) {
          log.info("test%n");
          util.copyTree(modules.resolve(path(Folder.MAIN_JAVA)), path(Folder.TARGET_TEST_SOURCE));
          util.copyTree(modules.resolve(path(Folder.TEST_JAVA)), path(Folder.TARGET_TEST_SOURCE));
          compile(path(Folder.TARGET_TEST_SOURCE), path(Folder.TARGET_TEST_COMPILED));
        }
        break;
      default:
        log.check(false, "unsupported module source path layout %s for: `%s`", score.layout, modules);
    }
    return this;
  }

  public int compile(Path moduleSourcePath, Path destinationPath) throws IOException {
    log.check(Files.exists(moduleSourcePath), "module source path `%s` does not exist", moduleSourcePath);
    List<String> arguments = new ArrayList<>();
    if (log.threshold <= Level.FINEST.intValue()) {
      // output messages about what the compiler is doing
      arguments.add("-verbose");
    }
    // file encoding
    arguments.add("-d");
    arguments.add(destinationPath.toString());
    // output source locations where deprecated APIs are used
    arguments.add("-deprecation");
    // generate metadata for reflection on method parameters
    arguments.add("-parameters");
    // terminate compilation if warnings occur
    arguments.add("-Werror");
    // specify character encoding used by source files
    arguments.add("-encoding");
    arguments.add("UTF-8");
    // specify where to find application modules
    arguments.add("--module-path");
    arguments.add(path(Folder.DEPENDENCIES).toString());
    // specify where to find input source files for multiple modules
    arguments.add("--module-source-path");
    arguments.add(moduleSourcePath.toString());
    log.log(Level.FINE,"javac%n");
    arguments.forEach(a -> log.log(Level.FINE,"%s%s%n", a.startsWith("-") ? "  " : "", a));
    // collect .java source files
    int[] count = {0};
    Files.walk(moduleSourcePath)
        .map(Path::toString)
        .filter(name -> name.endsWith(".java"))
        .peek(name -> count[0]++)
        .forEach(arguments::add);
    // compile
    long start = System.currentTimeMillis();
    int code = javac.run(score.streamIn, score.streamOut, score.streamErr, arguments.toArray(new String[0]));
    log.info("%d java files compiled in %d ms%n", count[0], System.currentTimeMillis() - start);
    return code;
  }

  public int run(String module, String main, String... arguments) throws Exception {
    log.tag("run").info("%s/%s%n", module, main);
    Stream<Folder> folders = Stream.of(Folder.DEPENDENCIES, Folder.TARGET_MAIN_COMPILED);
    List<String> command = new ArrayList<>();
    command.add("java");
    command.add("--module-path");
    command.add(String.join(File.pathSeparator, folders.map(f -> path(f).toString()).collect(Collectors.toList())));
    command.add("--module");
    command.add(module + "/" + main);
    command.addAll(List.of(arguments));
    command.forEach(a -> log.log(Level.FINE,"%s%s%n", a.startsWith("-") ? "  " : "", a));
    Process process = new ProcessBuilder().command(command).redirectErrorStream(true).start();
    process.getInputStream().transferTo(System.out);
    try {
      return process.waitFor();
    } catch (InterruptedException e) {
      return 1;
    }
  }

  public Bach test() throws Exception {
    log.tag("test");
    Path junitPath = path(Folder.TOOLS).resolve("junit");
    Path junitJar = util.download(URI.create("http://central.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.0.0-M4/junit-platform-console-standalone-1.0.0-M4.jar"), junitPath);
    util.findDirectoryNames(path(Folder.TARGET_TEST_COMPILED)).forEach(module -> {
      try {
        log.info("module %s%n", module);
        Path modulePath = path(Folder.TARGET_TEST_COMPILED).resolve(module);
        List<String> command = new ArrayList<>();
        command.add("java");
        command.add("-jar");
        command.add(junitJar.toString());
        command.add("--classpath");
        command.add(modulePath.toString());
        command.add("--scan-classpath");
        command.add(modulePath.toString());
        command.forEach(a -> log.log(Level.FINE,"%s%s%n", a.startsWith("-") ? "  " : "", a));
        Process process = new ProcessBuilder().command(command).redirectErrorStream(true).start();
        process.getInputStream().transferTo(System.out);
        process.waitFor();
      } catch (Exception e) {
        log.error(e, "testing %s failed", module);
      }
    });
    return this;
  }

  public Bach format() throws Exception {
    log.tag("format");
    Path path = path(Folder.SOURCE);
    log.info("format %s%n", path);
    Path formatPath = path(Folder.TOOLS).resolve("format");
    Path formatJar = util.download(URI.create("https://github.com/google/google-java-format/releases/download/google-java-format-1.3/google-java-format-1.3-all-deps.jar"), formatPath);
    try {
      List<String> command = new ArrayList<>();
      command.add("java");
      command.add("-jar");
      command.add(formatJar.toString());
      command.add("--replace");
      command.forEach(a -> log.log(Level.FINE,"%s%s%n", a.startsWith("-") ? "  " : "", a));
      // collect .java source files
      int[] count = {0};
      Files.walk(path)
              .map(Path::toString)
              .filter(name -> name.endsWith(".java"))
              .filter(name -> !name.endsWith("module-info.java"))
              .peek(name -> count[0]++)
              .forEach(command::add);
      Process process = new ProcessBuilder().command(command).redirectErrorStream(true).start();
      process.getInputStream().transferTo(System.out);
      process.waitFor();
      log.info("%d files formatted%n", count[0]);
    } catch (Exception e) {
      log.error(e, "format failed");
    }
    return this;
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
      log(Level.CONFIG,"%n");
      return this;
    }

    void check(boolean condition, String format, Object...args) {
      if (!condition) log.error(new AssertionError("check failed"), format, args);
    }

    <T> T assigned(T instance, String format, Object...args) {
      if (instance == null) log.error(new NullPointerException("check failed"), format, args);
      return instance;
    }

    private void printContext(Level level) {
      score.streamOut.printf("%7s ", tag);
      if (threshold < Level.INFO.intValue()) {
        score.streamOut.printf("%6s| ", level.getName().toLowerCase());
      }
    }

    void log(Level level, String format, Object... args) {
      if (level.intValue() < threshold) {
        return;
      }
      if (args.length == 1 && args[0] instanceof Collection) {
        for (Object arg : (Iterable<?>) args[0]) {
          printContext(level);
          if (arg instanceof Folder) {
            arg = arg + " -> " + path((Folder) arg);
          }
          score.streamOut.printf(format, arg);
        }
        return;
      }
      printContext(level);
      score.streamOut.printf(format, args);
    }

    void info(String format, Object... args) {
      log(Level.INFO, format, args);
    }

    void error(Throwable cause, String format, Object... args) {
      String message = String.format(format, args);
      log(Level.SEVERE, message);
      throw new Error(message, cause);
    }
  }

  class Util {

    void deleteIfExists(Path path) {
      try {
        Files.deleteIfExists(path);
      } catch (IOException e) {
        throw new AssertionError("should not happen", e);
      }
    }

    Stream<String> findDirectoryNames(Path root) throws IOException {
      return Files.find(root, 1, (path, attr) -> Files.isDirectory(path))
              .filter(path -> !root.equals(path))
              .map(root::relativize)
              .map(Path::toString);
    }

    Path cleanTree(Path root, boolean keepRoot) throws IOException {
      return cleanTree(root, keepRoot, __ -> true);
    }

    Path cleanTree(Path root, boolean keepRoot, Predicate<Path> filter) throws IOException {
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
      log.log(Level.FINE, "deleted tree `%s`%n", root);
      return root;
    }

    void copyTree(Path source, Path target) {
      if (!Files.exists(source)) {
        return;
      }
      log.log(Level.FINE, "copy `%s` to `%s`%n", source, target);
      try {
        Files.createDirectories(target);
        Files.walkFileTree(source, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
            new SimpleFileVisitor<>() {
              @Override
              public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
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
              public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
              }
            });
      } catch (IOException e) {
        log.error(e, "copying `%s` to `%s` failed", source, target);
      }
    }

    /**
     * Download the resource specified by its URI to the target directory.
     */
    Path download(URI uri, Path targetDirectory) throws IOException {
      return download(uri, targetDirectory, fileName(uri), targetPath -> true);
    }

    /**
     * Download the resource specified by its URI to the target directory using the provided file name.
     */
    Path download(URI uri, Path targetDirectory, String targetFileName, Predicate<Path> useTimeStamp) throws IOException {
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
              log.log(Level.FINE, "download skipped - using `%s`%n", targetPath);
              return targetPath;
            }
          }
        }
        Files.delete(targetPath);
      }
      log.log(Level.FINE, "download `%s` in progress...%n", uri);
      try (InputStream sourceStream = url.openStream(); OutputStream targetStream = Files.newOutputStream(targetPath)) {
        sourceStream.transferTo(targetStream);
      }
      Files.setLastModifiedTime(targetPath, urlLastModifiedTime);
      log.log(Level.CONFIG, "download `%s` completed%n", uri);
      log.info("stored `%s` [timestamp=%s]%n", targetPath, urlLastModifiedTime.toString());
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
      catch(IOException e) {
        log.error(e, "moving module-info failed for %s", path);
      }
      log.log(Level.FINE, "moved `%s` to `%s`%n", pathSource, "module-info.java");
    }
  }

  static class Builder {

    Level level = Level.INFO;
    String name = Paths.get(".").toAbsolutePath().normalize().getFileName().toString();
    Layout layout = Layout.AUTO;
    Map<Folder, Path> folders = Collections.emptyMap();
    private Map<Folder, Path> override = new EnumMap<>(Folder.class);
    InputStream streamIn = System.in;
    PrintStream streamOut = System.out;
    PrintStream streamErr = System.err;

    Score build() {
      if (folders == Collections.EMPTY_MAP) {
        folders = generateFolderPathMap(override);
      }
      if (!folders.keySet().equals(Set.of(Folder.values()))) {
        throw new AssertionError("key set mismatch in folders=" + folders);
      }
      if (layout == Layout.AUTO) {
        layout = layoutOf(folders.get(Folder.SOURCE));
      }
      return new Score(this);
    }

    Bach bach() {
      return new Bach(build());
    }

    Builder peek(Consumer<Builder> consumer) {
      consumer.accept(this);
      return this;
    }

    Builder name(String name) {
      this.name = name;
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

    static Map<Folder, Path> generateFolderPathMap(Map<Folder, Path> override) {
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

    static Layout layoutOf(Path root) {
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
        String moduleSource = new String(Files.readAllBytes(root.resolve(path)), "UTF-8");
        Pattern namePattern = Pattern.compile("(module)\\s+(.+)\\s*\\{.*");
        Matcher nameMatcher = namePattern.matcher(moduleSource);
        if (!nameMatcher.find()) {
          throw new IllegalArgumentException("Expected java module descriptor unit, but got: \n" + moduleSource);
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
