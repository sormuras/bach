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

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.isRegularFile;
import static java.nio.file.Files.newOutputStream;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Build modular Java project. */
public class Bach {

  /** This program is launched via JEP 330, if no argument is passed */
  public static final Path DEFAULT_BUILD_PROGRAM = Path.of("src/bach/Build.java");

  public static void main(String... args) throws Exception {
    var version = System.getProperty("bach.version", "master-SNAPSHOT");
    var lib = Path.of(System.getProperty("bach.lib", ".bach/build/lib"));
    System.out.printf("Loading module Bach.java %s to %s...%n", version, lib.toUri());
    load(
        lib,
        "de.sormuras.bach",
        version,
        URI.create(
            "https://jitpack.io/com/github/sormuras/bach/"
                + version
                + "/bach-"
                + version
                + ".jar"));

    var java = new ArrayList<String>();
    java.add(ProcessHandle.current().info().command().orElse("java"));
    java.add("-D" + "user.language=en");
    java.add("--module-path=" + lib);
    if (args.length == 0) {
      if (isRegularFile(DEFAULT_BUILD_PROGRAM)) {
        java.add("--add-modules=de.sormuras.bach");
        java.add(DEFAULT_BUILD_PROGRAM.toString());
      } else {
        java.add("--module");
        java.add("de.sormuras.bach/de.sormuras.bach");
      }
    }
    if (args.length == 1) {
      java.add("--add-modules=de.sormuras.bach");
      java.add(Path.of(args[0]).toString()); // "etc/CustomBuild.java"
    }
    start(java);
  }

  static void load(Path lib, String module, String version, URI uri) throws Exception {
    var jar = lib.resolve(module + '-' + version + ".jar");
    if (isRegularFile(jar) && !version.endsWith("SNAPSHOT")) return;
    System.out.printf("%s <- %s%n", jar, uri);
    createDirectories(lib);
    try (var source = uri.toURL().openStream();
        var target = newOutputStream(jar)) {
      source.transferTo(target);
    }
  }

  static void start(List<String> command) throws Exception {
    System.out.printf("%s%n", String.join(" ", command));
    var process = new ProcessBuilder(command).inheritIO().start();
    int code = process.waitFor();
    if (code != 0) throw new Error("Non-zero exit code: " + code);
  }
}
