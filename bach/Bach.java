/*
 * Copyright 2017 Christian Stein
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

// no package

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/**
 * Java Shell Builder.
 *
 * @noinspection WeakerAccess, unused
 */
public class Bach {

  public static void main(String... args) throws Exception {
    System.out.printf("%n%s%n%n", "BASIC");
    new Bach(Level.INFO, Layout.BASIC)
            .set(Folder.SOURCE, Paths.get("demo/basic"))
            .set(Folder.TARGET, Paths.get("target/bach/basic"))
            .compile()
            .run("com.greetings", "com.greetings.Main");

    System.out.printf("%n%s%n%n", "COMMON");
    new Bach(Level.INFO, Layout.COMMON)
            .set(Folder.SOURCE, Paths.get("demo/common"))
            .set(Folder.TARGET, Paths.get("target/bach/common"))
            .compile()
            .run("com.greetings", "com.greetings.Main");

    System.out.printf("%n%s%n%n", "IDEA");
    new Bach(Level.INFO, Layout.IDEA)
            .set(Folder.SOURCE, Paths.get("demo/idea"))
            .set(Folder.TARGET, Paths.get("target/bach/idea"))
            .load("org.junit.jupiter.api", URI.create("http://central.maven.org/maven2/org/junit/jupiter/junit-jupiter-api/5.0.0-M4/junit-jupiter-api-5.0.0-M4.jar"))
            .load("org.junit.platform.commons", URI.create("http://central.maven.org/maven2/org/junit/platform/junit-platform-commons/1.0.0-M4/junit-platform-commons-1.0.0-M4.jar"))
            // .load("org.opentest4j", URI.create("http://central.maven.org/maven2/org/opentest4j/opentest4j/1.0.0-M2/opentest4j-1.0.0-M2.jar"))
            .compile()
            .run("com.greetings", "com.greetings.Main");
  }

  enum Layout {
    /**
     * {@code src/<module>}
     */
    BASIC,

    /**
     * {@code src/[main|test]/[java|resources]/<module>}
     */
    COMMON,

    /**
     * {@code src/<module>/[main|test]/[java|resources]}
     */
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
    ;

    final List<Folder> parents;
    final Path path;

    Folder(String path, Folder... parents) {
      this.path = Paths.get(path);
      this.parents = List.of(parents);
    }
  }

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

  public Bach compile() throws IOException {
    log.tag("compile").log(Level.CONFIG, "folder %s%n", folders.keySet());
    Path modules = get(Folder.SOURCE);
    if (Files.notExists(modules)) {
      throw new Error("folder source `" + modules + "` does not exist");
    }
    if (util.findDirectoryNames(modules).count() == 0) {
      throw new Error("no directory found in `" + modules + "`");
    }
    util.cleanTree(get(Folder.TARGET), true);
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
        });
        log.info("main%n");
        compile(get(Folder.TARGET_MAIN_SOURCE), get(Folder.TARGET_MAIN_COMPILED));
        if (Files.exists(get(Folder.TARGET_TEST_SOURCE))) {
          log.info("test%n");
          compile(get(Folder.TARGET_TEST_SOURCE), get(Folder.TARGET_TEST_COMPILED));
        }
        break;
      default:
        throw new Error("unsupported module source path layout " + layout + " for: `" + modules + "`");
    }
    return this;
  }

  public int compile(Path moduleSourcePath, Path destinationPath) throws IOException {
    if (Files.notExists(moduleSourcePath)) {
      throw new Error("module source path `" + moduleSourcePath + "` does not exist!");
    }
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

  public int jar() {
    throw new UnsupportedOperationException("jar() not implemented, yet");
  }

  public int run(String module, String main) throws Exception {
    log.tag("run").info("%s/%s%n", module, main);
    Stream<Folder> folders = Stream.of(Folder.DEPENDENCIES, Folder.TARGET_MAIN_COMPILED);
    List<String> command = new ArrayList<>();
    command.add("java");
    command.add("--module-path");
    command.add(String.join(File.pathSeparator, folders.map(f -> get(f).toString()).collect(Collectors.toList())));
    command.add("--module");
    command.add(module + "/" + main);
    command.forEach(a -> log.log(Level.FINE,"%s%s%n", a.startsWith("-") ? "  " : "", a));
    Process process = new ProcessBuilder().command(command).redirectErrorStream(true).start();
    process.getInputStream().transferTo(System.out);
    try {
      return process.waitFor();
    } catch (InterruptedException e) {
      return 1;
    }
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
      if (Files.notExists(root)) {
        if (keepRoot) {
          Files.createDirectories(root);
        }
        return root;
      }
      Files.walk(root)
          .filter(p -> !(keepRoot && root.equals(p)))
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
        throw new Error("Copying " + source + " to " + target + " failed: " + e, e);
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
              return targetPath;
            }
          }
        }
        Files.delete(targetPath);
      }
      try (InputStream sourceStream = url.openStream(); OutputStream targetStream = Files.newOutputStream(targetPath)) {
        sourceStream.transferTo(targetStream);
      }
      Files.setLastModifiedTime(targetPath, urlLastModifiedTime);
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
        Files.move(pathSource, path.resolve("module-info.java"));
        log.log(Level.FINE, "moved `%s` to `%s`%n", pathSource, "module-info.java");
      }
      catch(IOException e) {
        throw new Error("Moving module-info failed: " + path, e);
      }
    }
  }

  class StandardStreams {
    InputStream in = System.in;
    PrintStream out = System.out;
    PrintStream err = System.err;
  }
}
