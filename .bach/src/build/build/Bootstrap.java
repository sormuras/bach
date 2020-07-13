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

package build;

import java.io.File;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.spi.ToolProvider;

public class Bootstrap {
  public static void main(String... args) throws Exception {
    var destination = Path.of(".bach/workspace/classes/bootstrap");
    var pattern = DateTimeFormatter.ofPattern("yyyy.MM.dd.HHmm").withZone(ZoneId.of("UTC"));
    var version = ModuleDescriptor.Version.parse(pattern.format(Instant.now()));

    step("Bootstrap");

    run(
        "javac",
        "-d",
        "" + destination,
        "--module=build,de.sormuras.bach",
        "--module-source-path=.bach/src" + File.pathSeparator + "src/*/main/java",
        "--module-version=" + version + "-BOOTSTRAP",
        "-encoding",
        "UTF-8",
        "-W" + "error",
        "-X" + "lint");

    step("Build");

    var java = ProcessHandle.current().info().command().orElse("java");
    start(
        java,
        "-ea",
        "-D" + "user.language=en",
        "-D" + "java.util.logging.config.file=src/logging.properties",
        "-D" + "ebug",
        "--enable-preview",
        "--module-path=" + destination,
        "--module=build/build.Build");

    var modules = Path.of(".bach/workspace/modules");
    if (Files.notExists(modules)) {
      System.err.println("Modules directory not found: " + modules);
      return;
    }

    step("Smoke-test module de.sormuras.bach");

    start(java, "--module-path", modules.toString(), "--module", "de.sormuras.bach", "help");

    step("Boostrap finished.");
  }

  static void step(String caption) {
    System.out.println();
    System.out.println("#");
    System.out.println("# " + caption);
    System.out.println("#");
    System.out.println();
  }

  static void run(String name, String... args) {
    System.out.printf(">> %s %s%n", name, String.join(" ", args));
    var tool = ToolProvider.findFirst(name).orElseThrow(() -> new Error(name));
    int code = tool.run(System.out, System.err, args);
    if (code != 0) throw new Error("Non-zero exit code: " + code);
  }

  static void start(String... command) throws Exception {
    System.out.println(">> " + String.join(" ", command));
    var process = new ProcessBuilder(command).inheritIO().start();
    int code = process.waitFor();
    if (code != 0) throw new Error("Non-zero exit code: " + code);
  }
}
