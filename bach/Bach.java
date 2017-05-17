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

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.logging.Level;

import static java.util.Objects.requireNonNull;

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
    new Bach(Level.FINE, Layout.COMMON)
            .set(Folder.SOURCE, Paths.get("demo/common"))
            .set(Folder.TARGET, Paths.get("target/bach/common"));
            // .compile();
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
    TILED,
  }

  enum Folder {
    DEPENDENCIES("deps"),
    SOURCE("src"),
    TARGET("target/bach"),
    ;

    final Path defaultPath;

    Folder(String defaultPath) {
      this.defaultPath = Paths.get(defaultPath);
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
    log.log(Level.CONFIG, "folder %s%n", folders.entrySet());
  }

  public Bach set(Folder folder, Path path) {
    folders.put(folder, path);
    return this;
  }

  public Path get(Folder folder) {
    return folders.get(folder);
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

  public Bach prepareCommon(Path modules) {
    log.log(Level.CONFIG, "jigsaw %s%n", modules);
//    Path target = get(Folder.TARGET).resolve(get(Folder.TARGET_PREPARED));
//    Path preparedMain = target.resolve(get(Folder.TARGET_PREPARED_SOURCE_MAIN));
//    Path preparedTest = target.resolve(get(Folder.TARGET_PREPARED_SOURCE_TEST));
//    util.copyTree(modules.resolve(module + "/main/java"), preparedMain.resolve(module));
//    util.copyTree(modules.resolve(module + "/main/resources"), target.resolve("main/resources/" + module));
//    util.copyTree(modules.resolve(module + "/test/java"), preparedTest.resolve(module));
//    util.copyTree(modules.resolve(module + "/test/resources"), target.resolve("test/resources/" + module));
//    util.moveModuleInfo(preparedTest.resolve(module));
    util.copyTree(modules.resolve("main/java"), get(Folder.TARGET).resolve("main/module-source-path"));
    util.copyTree(modules.resolve("main/resources"), get(Folder.TARGET).resolve("main/module-source-path"));
    util.copyTree(modules.resolve("test/java"), get(Folder.TARGET).resolve("test/module-source-path"));
    util.copyTree(modules.resolve("test/resources"), get(Folder.TARGET).resolve("test/module-source-path"));
    return this;
  }

  // modules/<module>/[main|test]/[java|resources]
  public Bach prepareIDEA(Path modules, String module) {
    log.log(Level.CONFIG, "idea %s%n", module);
    //Path target = get(Folder.TARGET).resolve(get(Folder.TARGET_PREPARED));
    //Path preparedMain = target.resolve(get(Folder.TARGET_PREPARED_SOURCE_MAIN));
    //Path preparedTest = target.resolve(get(Folder.TARGET_PREPARED_SOURCE_TEST));
    //util.copyTree(modules.resolve(module + "/main/java"), preparedMain.resolve(module));
    //util.copyTree(modules.resolve(module + "/main/resources"), target.resolve("main/resources/" + module));
    //util.copyTree(modules.resolve(module + "/test/java"), preparedTest.resolve(module));
    //util.copyTree(modules.resolve(module + "/test/resources"), target.resolve("test/resources/" + module));
    //util.moveModuleInfo(preparedTest.resolve(module));
    return this;
  }

  public Bach compile() throws IOException {
    log.tag("compile").log(Level.CONFIG, "folder %s%n", folders.entrySet());
    Path modules = get(Folder.SOURCE);
    if (Files.notExists(modules)) {
      throw new Error("folder source `" + modules + "` does not exist");
    }
    Path compiled = util.cleanTree(get(Folder.TARGET).resolve("compiled"), true);
    switch (layout) {
      case BASIC:
        compile(get(Folder.SOURCE), compiled);
        break;
      case COMMON:
        prepareCommon(modules);
        log.info("main%n");
        compile(get(Folder.SOURCE), compiled.resolve("main/exploded"));
        log.info("test%n");
        // TODO util.copyTree(folders.get(Folder.SOURCE_MAIN_JAVA), folders.get(Folder.SOURCE_TEST_JAVA), true);
        // TODO compile(folders.get(Folder.SOURCE_TEST_JAVA), compiled.resolve("test/exploded"));
        break;
      case TILED:
        Files.find(modules, 1, (path, attr) -> Files.isDirectory(path))
                .filter(path -> !modules.equals(path))
                .map(path -> modules.relativize(path).toString())
                .forEach(module -> prepareIDEA(modules, module));
      default:
        throw new Error("unsupported module source path layout "+layout+" for: `" + modules + "`");
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
    List<String> command = new ArrayList<>();
    command.add("java");
    command.add("--module-path");
    List<String> modulePath = List.of(get(Folder.DEPENDENCIES).toString(), get(Folder.TARGET).resolve("compiled").toString());
    command.add(String.join(File.pathSeparator, modulePath));
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
        folders.put(folder, folder.defaultPath);
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
      copyTree(source, target, false);
    }

    void copyTree(Path source, Path target, boolean ignoreExistingFiles) {
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
                try {
                  Files.copy(file, target.resolve(source.relativize(file)));
                } catch (FileAlreadyExistsException e) {
                  if (!ignoreExistingFiles) {
                    throw e;
                  }
                }
                return FileVisitResult.CONTINUE;
              }
            });
      } catch (IOException e) {
        throw new Error("Copying " + source + " to " + target + " failed: " + e, e);
      }
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
