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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/** Build modular Java project. */
public class Bach {

  /** The {@code Bach.java} version constant. */
  static final String VERSION = "2.0-RC2";

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
    var src = Path.of("src");
    if (Files.isDirectory(src)) {
      System.out.println("Scaffolding not possible: directory 'src/' already exists.");
      return;
    }
    var scanner = new Scanner(System.in);
    System.out.println();
    System.out.println("Scaffold a modular Java project?");
    System.out.println("  0 -> No.");
    System.out.println("  1 -> Create minimal `module-info.java`-only project.");
    System.out.println();
    System.out.print("Your choice: ");
    switch (scanner.nextInt()) {
      case 0:
        System.out.println("No file created.");
        return;
      case 1:
        var name = Path.of("").toAbsolutePath().getFileName();
        var module = name != null ? name.toString() : "demo";
        var folder = Files.createDirectories(src.resolve(module));
        Files.write(folder.resolve("module-info.java"), List.of("module " + module + " {}", ""));
        break;
      default:
        System.err.println("Your choice is not supported: no file created.");
        return;
    }
    System.out.println();
    System.out.println("Created the following directories and files:");
    try (var stream = Files.walk(src)) {
      stream.sorted().forEach(System.out::println);
    }
    System.out.println();
    System.out.println("Run 'bach build' to build the project.");
  }

  static void load(Path lib, String module, String version, URI uri) throws Exception {
    debug(String.format("| Loading module de.sormuras.bach %s to %s...%n", version, lib.toUri()));
    var jar = lib.resolve(module + '-' + version + ".jar");
    if (isRegularFile(jar) && !version.endsWith("SNAPSHOT")) return;
    debug(String.format("|   %s <- %s%n", jar, uri));
    createDirectories(lib);
    try (var source = uri.toURL().openStream();
        var target = newOutputStream(jar)) {
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
}
