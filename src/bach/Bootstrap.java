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

import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;

public class Bootstrap {
  public static void main(String... args) throws Exception {
    var destination = Path.of(".bach", "bootstrap");
    var pattern = DateTimeFormatter.ofPattern("yyyy.MM.dd.HHmmss").withZone(ZoneId.of("UTC"));
    var version = ModuleDescriptor.Version.parse(pattern.format(Instant.now()));

    run(
        "javac",
        "-d",
        destination.toString(),
        "--module=de.sormuras.bach",
        "--module-source-path=src/*/main/java",
        "--module-version=" + version + "-BOOTSTRAP",
        "-encoding",
        "UTF-8",
        "-Werror",
        "-X" + "lint");
    delete(Path.of(".bach/out"));
    start(
        ProcessHandle.current().info().command().orElse("java"),
        "-D" + "user.language=en",
        "--class-path", // using "--module-path" conflicts with "junit", see #111
        destination.resolve("de.sormuras.bach").toString(),
        "--add-modules",
        "ALL-SYSTEM",
        "src/bach/Build.java");
  }

  static void delete(Path root) throws Exception {
    if (Files.notExists(root)) return;
    try (var stream = Files.walk(root)) {
      var selected = stream.sorted((p, q) -> -p.compareTo(q));
      for (var path : selected.collect(Collectors.toList())) {
        Files.deleteIfExists(path);
      }
    }
  }

  static void run(String name, String... args) {
    System.out.printf(">> %s %s%n", name, String.join(" ", args));
    var tool = ToolProvider.findFirst(name).orElseThrow(() -> new RuntimeException(name));
    int code = tool.run(System.out, System.err, args);
    if (code != 0) {
      throw new Error("Non-zero exit code: " + code);
    }
  }

  static void start(String... command) throws Exception {
    System.out.println(">> " + String.join(" ", command));
    var process = new ProcessBuilder(command).inheritIO().start();
    if (process.waitFor() != 0) {
      throw new Error("Non-zero exit");
    }
  }
}
