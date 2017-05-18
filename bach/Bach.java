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

import static java.util.Objects.requireNonNull;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.function.*;
import java.util.logging.*;
import java.util.stream.*;
import javax.tools.*;

enum Layout {
  /** {@code src/<module>} */
  BASIC,
  /** {@code src/[main|test]/[java|resources]/<module>} */
  COMMON,
  /** {@code src/<module>/[main|test]/[java|resources]} */
  IDEA,
}

enum Folder {
  DEPENDENCIES("dependencies"),
  SOURCE("src"),
  MAIN_JAVA("main/java"),
  TEST_JAVA("test/java"),
  TARGET("target/bach"),
  TARGET_MAIN_SOURCE("main/module-source-path", TARGET),
  TARGET_MAIN_COMPILED("main/compiled", TARGET),
  TARGET_TEST_SOURCE("test/module-source-path", TARGET),
  TARGET_TEST_COMPILED("test/compiled", TARGET),
  TARGET_TOOLS("tools", TARGET),
  ;

  final List<Folder> parents;
  final Path path;

  Folder(String path, Folder... parents) {
    this.path = Paths.get(path);
    this.parents = List.of(parents);
  }
}

/**
 * Java Shell Builder.
 *
 * @noinspection WeakerAccess, UnusedReturnValue, unused
 */
public class Bach {

  private final JavaCompiler javac;
  private final EnumMap<Folder, Path> folders;
  private final StandardStreams standardStreams;
  private final Log log;
  private final Util util;
  private final Layout layout;

  public Bach() {
    this(Level.FINE, Layout.COMMON);
  }

  public Bach(Level initialLevel, Layout layout) {
    this.util = new Util();
    this.folders = util.defaultFolders();
    this.standardStreams = new StandardStreams();
    this.javac = requireNonNull(ToolProvider.getSystemJavaCompiler(), "java compiler not available");
    this.log = new Log().level(initialLevel).tag("init");
    this.layout = requireNonNull(layout, "layout must not be null");
    log.info("%s initialized%n", getClass());
    log.log(Level.CONFIG, "level=%s%n", initialLevel);
    log.log(Level.CONFIG, "layout=%s%n", layout);
    log.log(Level.CONFIG, "pwd=`%s`%n", Paths.get(".").toAbsolutePath().normalize());
    log.log(Level.CONFIG, "folder %s%n", folders.keySet());
  }

  public Bach set(Folder folder, Path path) {
    folders.put(folder, path);
    return this;
  }

  public Path get(Folder folder) {
    if (folder.parents.isEmpty()) {
      return folders.get(folder);
    }
    Iterator<Folder> iterator = folder.parents.iterator();
    Path path = folders.get(iterator.next());
    while (iterator.hasNext()) {
      path = path.resolve(folders.get(iterator.next()));
    }
    return path.resolve(folders.get(folder));
  }

  public Bach set(Level level) {
    log.level(level);
    return this;
  }

  public Bach clean() throws IOException {
    log.tag("clean");
    util.cleanTree(get(Folder.TARGET), false);
    return this;
  }

  public Bach load(String module, URI uri) throws IOException {
    util.download(uri, get(Folder.DEPENDENCIES), module + ".jar", path -> true);
    return this;
  }

  private void check(boolean condition, String format, Object...args) {
    if (!condition) {
      log.error(null, format, args);
    }
  }

  public Bach compile() throws Exception {
    log.tag("compile").log(Level.CONFIG, "folder %s%n", folders.keySet());
    Path modules = get(Folder.SOURCE);
    check(Files.exists(modules),"folder source `%s` does not exist", modules);
    check(util.findDirectoryNames(modules).count() > 0, "no directory found in `%s`", modules);
    Path tools = get(Folder.TARGET_TOOLS);
    util.cleanTree(get(Folder.TARGET), true, path -> !path.startsWith(tools));
    switch (layout) {
      case BASIC:
        log.info("main%n");
        compile(modules, get(Folder.TARGET_MAIN_COMPILED));
        break;
      case COMMON:
        log.info("main%n");
        compile(modules.resolve(get(Folder.MAIN_JAVA)), get(Folder.TARGET_MAIN_COMPILED));
        if (Files.exists(modules.resolve(get(Folder.TEST_JAVA)))) {
          log.info("test%n");
          util.copyTree(modules.resolve(get(Folder.MAIN_JAVA)), get(Folder.TARGET_TEST_SOURCE));
          util.copyTree(modules.resolve(get(Folder.TEST_JAVA)), get(Folder.TARGET_TEST_SOURCE));
          compile(get(Folder.TARGET_TEST_SOURCE), get(Folder.TARGET_TEST_COMPILED));
        }
        break;
      case IDEA:
        util.findDirectoryNames(modules).forEach(module -> {
          log.log(Level.FINE, "module %s%n", module);
          Path source = modules.resolve(module);
          // main
          util.copyTree(source.resolve(get(Folder.MAIN_JAVA)), get(Folder.TARGET_MAIN_SOURCE).resolve(module));
          // test
          util.copyTree(source.resolve(get(Folder.MAIN_JAVA)), get(Folder.TARGET_TEST_SOURCE).resolve(module));
          util.copyTree(source.resolve(get(Folder.TEST_JAVA)), get(Folder.TARGET_TEST_SOURCE).resolve(module));
          util.moveModuleInfo(get(Folder.TARGET_TEST_SOURCE).resolve(module));
        });
        log.info("main%n");
        compile(get(Folder.TARGET_MAIN_SOURCE), get(Folder.TARGET_MAIN_COMPILED));
        if (Files.exists(get(Folder.TARGET_TEST_SOURCE))) {
          log.info("test%n");
          compile(get(Folder.TARGET_TEST_SOURCE), get(Folder.TARGET_TEST_COMPILED));
        }
        break;
      default:
        check(false, "unsupported module source path layout %s for: `%s`", layout, modules);
    }
    return this;
  }

  public int compile(Path moduleSourcePath, Path destinationPath) throws IOException {
    check(Files.exists(moduleSourcePath), "module source path `%s` does not exist", moduleSourcePath);
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
    arguments.add(get(Folder.DEPENDENCIES).toString());
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
    int code = javac.run(standardStreams.in, standardStreams.out, standardStreams.err, arguments.toArray(new String[0]));
    log.info("%d java files compiled in %d ms%n", count[0], System.currentTimeMillis() - start);
    return code;
  }

  public int run(String module, String main, String... arguments) throws Exception {
    log.tag("run").info("%s/%s%n", module, main);
    Stream<Folder> folders = Stream.of(Folder.DEPENDENCIES, Folder.TARGET_MAIN_COMPILED);
    List<String> command = new ArrayList<>();
    command.add("java");
    command.add("--module-path");
    command.add(String.join(File.pathSeparator, folders.map(f -> get(f).toString()).collect(Collectors.toList())));
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
    Path junitPath = get(Folder.TARGET_TOOLS).resolve("junit");
    Path junitJar = util.download(URI.create("http://central.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.0.0-M4/junit-platform-console-standalone-1.0.0-M4.jar"), junitPath);
    util.findDirectoryNames(get(Folder.TARGET_TEST_COMPILED)).forEach(module -> {
      try {
        log.info("module %s%n", module);
        Path modulePath = get(Folder.TARGET_TEST_COMPILED).resolve(module);
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
    Path path = get(Folder.SOURCE);
    log.info("format %s%n", path);
    Path formatPath = get(Folder.TARGET_TOOLS).resolve("format");
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

    private void printContext(Level level) {
      standardStreams.out.printf("%7s ", tag);
      if (threshold < Level.INFO.intValue()) {
        standardStreams.out.printf("%6s| ", level.getName().toLowerCase());
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
            arg = arg + " -> " + get((Folder) arg);
          }
          standardStreams.out.printf(format, arg);
        }
        return;
      }
      printContext(level);
      standardStreams.out.printf(format, args);
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

    EnumMap<Folder, Path> defaultFolders() {
      EnumMap<Folder, Path> folders = new EnumMap<>(Folder.class);
      for (Folder folder : Folder.values()) {
        folders.put(folder, folder.path);
      }
      return folders;
    }

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
      URL url = requireNonNull(uri, "uri must not be null").toURL();
      requireNonNull(targetDirectory, "targetDirectory must be null");
      if (requireNonNull(targetFileName, "targetFileName must be null").isEmpty()) {
        throw new IllegalArgumentException("targetFileName must be blank");
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

  class StandardStreams {
    InputStream in = System.in;
    PrintStream out = System.out;
    PrintStream err = System.err;
  }
}
