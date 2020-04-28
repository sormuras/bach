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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.spi.ToolProvider;

class BuildJigsawQuickStart {

  public static void main(String... args) throws Exception {
    var base = Path.of("doc", "project", "JigsawQuickStart");

    var module = "com.greetings";
    var classes = Files.createDirectories(base.resolve("build/classes"));
    runTool("javac", "--module=" + module, "--module-source-path=" + base, "-d", classes);

    var modules = Files.createDirectories(base.resolve("build/modules"));
    var file = modules.resolve(module + ".jar");
    runTool("jar", "--create", "--file=" + file, "-C", classes.resolve(module), ".");

    var image = deleteDirectories(base.resolve("build/image"));
    var addModules = "--add-modules=" + module;
    var modulePath = "--module-path=" + modules;
    var launcher = "--launcher=greet=" + module + "/com.greetings.Main";
    var output = "--output=" + image;
    runTool("jlink", addModules, modulePath, launcher, output);
  }

  static void runTool(String name, Object... arguments) {
    var tool = ToolProvider.findFirst(name).orElseThrow();
    var args = new String[arguments.length];
    for (int i = 0; i < args.length; i++) args[i] = arguments[i].toString();
    System.out.printf("%-8s %s%n", name, String.join(" ", args));
    var code = tool.run(System.out, System.err, args);
    if (code != 0) throw new AssertionError("Non-zero exit code: " + code);
  }

  static Path deleteDirectories(Path root) throws IOException {
    if (Files.notExists(root)) return root;
    try (var stream = Files.walk(root)) {
      var paths = stream.sorted((p, q) -> -p.compareTo(q));
      for (var path : paths.toArray(Path[]::new)) Files.deleteIfExists(path);
    }
    return root;
  }
}
