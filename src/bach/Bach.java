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

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.isRegularFile;
import static java.nio.file.Files.newOutputStream;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Build modular Java project. */
public class Bach {

  /** The {@code Bach.java} version constant. */
  static final String VERSION = "2.0-RC1";

  /** Debug flag. */
  static final boolean DEBUG = Boolean.getBoolean("ebug") || "".equals(System.getProperty("ebug"));

  /** The version of module {@code de.sormuras.bach} to load and use. */
  static final String BACH_VERSION = System.getProperty("Bach.java/version", VERSION);

  /** Directory to store module {@code de.sormuras.bach-{VERSION}.jar} to. */
  static final Path LIB = Path.of(System.getProperty("Bach.java/lib", ".bach/build/lib"));

  /** Transfer output stream of started process to this {@code System.out} stream. */
  static final boolean TRANSFER_IO = Boolean.getBoolean("Bach.java/transferIO");

  /** This program is launched via JEP 330, if no argument is passed to the main method. */
  static final Path DEFAULT_BUILD_PROGRAM = Path.of("src/bach/Build.java");

  public static void main(String... args) throws Exception {
    var version = BACH_VERSION.endsWith("-ea") ? "master-SNAPSHOT" : BACH_VERSION;
    System.out.println();
    System.out.println("Bach.java // https://github.com/sormuras/bach");
    System.out.println();
    debug(". BEGIN");
    debug("|   -DBach.java/version=" + BACH_VERSION + " -> " + version);
    debug("|   -DBach.java/lib=" + LIB);
    debug("|   -DBach.java/transferIO=" + TRANSFER_IO);
    debug("| Default build program");
    debug("|   path: " + DEFAULT_BUILD_PROGRAM);
    debug("|   exists: " + isRegularFile(DEFAULT_BUILD_PROGRAM));
    debug("| Arguments");
    debug("|   args: " + List.of(args));
    debug("|");

    var uri = "https://jitpack.io/com/github/sormuras/bach/{VERSION}/bach-{VERSION}.jar";
    load("de.sormuras.bach", version, URI.create(uri.replace("{VERSION}", version)));

    var java = new ArrayList<String>();
    java.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
    if (DEBUG) java.add("-Debug");
    java.add("-D" + "user.language=en");
    java.add("--module-path=" + LIB);
    java.add("--add-modules=" + "ALL-SYSTEM");
    if (args.length == 0) {
      if (isRegularFile(DEFAULT_BUILD_PROGRAM)) {
        java.add("--add-modules=de.sormuras.bach");
        java.add(DEFAULT_BUILD_PROGRAM.toString());
      } else {
        java.add("--module");
        java.add("de.sormuras.bach");
      }
    }
    if (args.length == 1) {
      java.add("--add-modules=de.sormuras.bach");
      java.add(Path.of(args[0]).toString()); // "etc/CustomBuild.java"
    }
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

  static void load(String module, String version, URI uri) throws Exception {
    debug(String.format("| Loading module de.sormuras.bach %s to %s...%n", version, LIB.toUri()));
    var jar = LIB.resolve(module + '-' + version + ".jar");
    if (isRegularFile(jar) && !version.endsWith("SNAPSHOT")) return;
    debug(String.format("|   %s <- %s%n", jar, uri));
    createDirectories(LIB);
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
