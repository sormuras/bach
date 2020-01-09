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

// default package

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.isRegularFile;
import static java.nio.file.Files.newOutputStream;

import java.net.URI;
import java.nio.file.CopyOption;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** Build modular Java project. */
public class Bach {

  /** The {@code Bach.java} version constant. */
  static final String VERSION = "2.0-ea";

  /** Debug flag. */
  static final boolean DEBUG = Boolean.getBoolean("ebug") || "".equals(System.getProperty("ebug"));

  /** The version of module {@code de.sormuras.bach} to load and use. */
  static final String BACH_VERSION = System.getProperty("Bach.java/version", VERSION);

  /** This program is launched via JEP 330, if it exists. */
  static final Path BUILD = Path.of(System.getProperty("Bach.java/build", "src/bach/Build.java"));

  /** Directory to store module {@code de.sormuras.bach-{VERSION}.jar} to. */
  static final Path LIB = Path.of(System.getProperty("Bach.java/lib", "lib"));

  /** Transfer output stream of started process to this {@code System.out} stream. */
  static final boolean TRANSFER_IO = Boolean.getBoolean("Bach.java/transferIO");

  public static void main(String... args) throws Exception {
    var version = BACH_VERSION.endsWith("-ea") ? "master-SNAPSHOT" : BACH_VERSION;
    System.out.println();
    System.out.println("Bach.java // https://github.com/sormuras/bach");
    System.out.println();
    debug(". BEGIN");
    debug("|   -DBach.java/version=" + BACH_VERSION + " -> " + version);
    debug("|   -DBach.java/lib=" + LIB);
    debug("|   -DBach.java/transferIO=" + TRANSFER_IO);
    debug("| Build program");
    debug("|   path: " + BUILD);
    debug("|   exists: " + isRegularFile(BUILD));
    debug("| Arguments");
    debug("|   args: " + List.of(args));
    debug("|");

    /*
     * Scaffold only?
     */
    if (args.length == 1 && args[0].equalsIgnoreCase("scaffold")) {
      scaffold();
      return;
    }

    var uri = "https://jitpack.io/com/github/sormuras/bach/{VERSION}/bach-{VERSION}.jar";
    load(LIB, "de.sormuras.bach", version, URI.create(uri.replace("{VERSION}", version)));

    var java = new ArrayList<String>();
    java.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
    if (DEBUG) java.add("-Debug");
    java.add("-D" + "user.language=en");
    java.add("--module-path=" + LIB);
    java.add("--add-modules=" + "ALL-SYSTEM");
    if (isRegularFile(BUILD)) {
      java.add("--add-modules=de.sormuras.bach");
      java.add(BUILD.toString());
    } else {
      java.add("--module");
      java.add("de.sormuras.bach");
    }
    java.addAll(List.of(args));
    start(java);
    debug("|");
    debug("' END.");
    System.out.println(
        "Thanks for using Bach.java!"
            + " Support its development at https://github.com/sponsors/sormuras (-:");
  }

  static void debug(String message) {
    if (DEBUG) System.out.println(message);
  }

  static void scaffold() throws Exception {
    var base = Path.of("");
    if (Files.isDirectory(base.resolve("src"))) {
      System.out.println("Scaffolding not possible: directory src already exists");
      return;
    }
    var scanner = new Scanner(System.in);
    System.out.println();
    System.out.println("Scaffold a modular Java project?");
    System.out.println("  0 -> No.");
    System.out.println("  1 -> Create minimal `module-info.java`-only project.");
    System.out.println("  2 -> Single module project with main and test realm.");
    System.out.println("  3 -> https://github.com/sormuras/bach-air");
    System.out.println("  4 -> https://github.com/sormuras/bach-javafx");
    System.out.println("  5 -> https://github.com/sormuras/bach-hansolos-spacefx");
    System.out.println("  6 -> https://github.com/sormuras/bach-lwjgl");
    System.out.println();
    System.out.print("Your choice: ");
    switch (scanner.nextInt()) {
      case 0:
        System.out.println("No file created.");
        return;
      case 1:
        {
          var folder = Files.createDirectories(base.resolve("src/minimal"));
          Files.write(folder.resolve("module-info.java"), List.of("module minimal {}", ""));
        }
        break;
      case 2:
        {
          var main = Files.createDirectories(base.resolve("src/demo/main/java"));
          Files.write(main.resolve("module-info.java"), List.of("module demo {}", ""));
          var test = Files.createDirectories(base.resolve("src/demo/test/java"));
          Files.write(
              test.resolve("module-info.java"),
              List.of(
                  "open /*test*/ module demo /*extends main module*/ {",
                  "  requires org.junit.jupiter;",
                  "}",
                  ""));
          var it = Files.createDirectories(base.resolve("src/it/test/java"));
          Files.write(
              it.resolve("module-info.java"),
              List.of(
                  "open /*test*/ module it {",
                  "  requires demo;",
                  "  requires org.junit.jupiter;",
                  "}",
                  ""));
        }
        break;
      case 3:
        {
          var zip = Path.of("bach-air.zip");
          load(zip, URI.create("https://github.com/sormuras/bach-air/archive/master.zip"));
          unzip(zip, base, "bach-air-master");
          Files.delete(zip);
        }
        break;
      case 4:
        {
          var zip = Path.of("bach-javafx.zip");
          load(zip, URI.create("https://github.com/sormuras/bach-javafx/archive/master.zip"));
          unzip(zip, base, "bach-javafx-master");
          Files.delete(zip);
        }
        break;
      case 5:
        {
          var zip = Path.of("bach-hansolos-spacefx.zip");
          load(
              zip,
              URI.create("https://github.com/sormuras/bach-hansolos-spacefx/archive/master.zip"));
          unzip(zip, base, "bach-hansolos-spacefx-master");
          Files.delete(zip);
        }
        break;
      case 6:
        {
          var zip = Path.of("bach-lwjgl.zip");
          load(zip, URI.create("https://github.com/sormuras/bach-lwjgl/archive/master.zip"));
          unzip(zip, base, "bach-lwjgl-master");
          Files.delete(zip);
        }
        break;
      default:
        System.err.println("Your choice is not supported: no file created.");
        return;
    }
    System.out.println();
    System.out.println("Created the following directories and files:");
    try (var stream = Files.walk(Path.of(""))) {
      stream.sorted().forEach(System.out::println);
    }
    System.out.println();
    System.out.println("Run 'bach build' to build the project.");
  }

  /** Copy selected files and directories from source to target directory. */
  static void copy(Path source, Path target, Predicate<Path> filter) {
    if (!Files.exists(source)) {
      throw new IllegalArgumentException("source must exist: " + source);
    }
    if (!Files.isDirectory(source)) {
      throw new IllegalArgumentException("source must be a directory: " + source);
    }
    if (Files.exists(target)) {
      if (target.equals(source)) return;
      if (!Files.isDirectory(target)) {
        throw new IllegalArgumentException("target must be a directory: " + target);
      }
      if (target.startsWith(source)) { // copy "a/" to "a/b/"...
        throw new IllegalArgumentException("target must not a child of source");
      }
    }
    var options = Set.of(StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
    try (var stream = Files.walk(source).sorted()) {
      var paths = stream.filter(filter).collect(Collectors.toList());
      for (var path : paths) {
        var destination = target.resolve(source.relativize(path).toString());
        var lastModified = Files.getLastModifiedTime(path);
        if (Files.isDirectory(path)) {
          Files.createDirectories(destination);
          Files.setLastModifiedTime(destination, lastModified);
          continue;
        }
        if (Files.exists(destination)) {
          if (lastModified.equals(Files.getLastModifiedTime(destination))) {
            continue;
          }
        }
        Files.copy(path, destination, options.toArray(CopyOption[]::new));
      }
    } catch (Exception e) {
      throw new RuntimeException("copy failed: " + source + " -> " + target, e);
    }
  }

  static void load(Path lib, String module, String version, URI uri) throws Exception {
    debug(String.format("| Loading module de.sormuras.bach %s to %s...%n", version, lib.toUri()));
    var jar = lib.resolve(module + '-' + version + ".jar");
    if (isRegularFile(jar) && !version.endsWith("SNAPSHOT")) return;
    load(jar, uri);
  }

  static void load(Path file, URI uri) throws Exception {
    debug(String.format("|   %s <- %s%n", file, uri));
    createDirectories(file.toAbsolutePath().getParent());
    try (var source = uri.toURL().openStream();
        var target = newOutputStream(file)) {
      source.transferTo(target);
    }
  }

  static void start(List<String> command) throws Exception {
    debug("| Starting: " + String.join(" ", command));
    var builder = new ProcessBuilder(command);
    if (TRANSFER_IO) builder.redirectErrorStream(true);
    else builder.inheritIO();
    var process = builder.start();
    if (TRANSFER_IO) process.getInputStream().transferTo(System.out);
    int code = process.waitFor();
    debug("|");
    if (code != 0) throw new Error("Non-zero exit code: " + code);
  }

  /** Unzip file to specified destination directory. */
  static Path unzip(Path zip, Path destination, String... more) throws Exception {
    var loader = Bach.class.getClassLoader();
    try (var zipFileSystem = FileSystems.newFileSystem(zip, loader)) {
      var root = zipFileSystem.getPath(zipFileSystem.getSeparator(), more);
      copy(root, destination, __ -> true);
    }
    return destination;
  }
}
