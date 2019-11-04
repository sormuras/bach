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

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.spi.ToolProvider;

public class Build {
  public static void main(String... args) throws Exception {
    System.out.printf("Building Bach.java...%n");
    var build = new Build();
    build.run(
        "javac",
        "-d",
        Path.of("bin/build/classes").toString(),
        Path.of("src/bach/Bach.java").toString());
    var junit = build.assemble(Path.of("bin/build/lib"));
    build.run(
        "javac",
        "-d",
        Path.of("bin/build/classes").toString(),
        "-cp",
        String.join(File.pathSeparator, Path.of("bin/build/classes").toString(), junit.toString()),
        Path.of("src/test/BachTests.java").toString(),
        Path.of("src/test/CommandTests.java").toString(),
        Path.of("src/test/Log.java").toString(),
        Path.of("src/test/ModulesTests.java").toString(),
        Path.of("src/test/ProjectTests.java").toString());
    build.start("java", "-ea", "--show-version", "-jar", junit.toString(), "--scan-class-path", "--class-path=" + Path.of("bin/build/classes"));
  }

  final HttpClient http = HttpClient.newHttpClient();

  Path assemble(Path lib) throws Exception {
    Files.createDirectories(lib);
    return load(lib, "org.junit.platform", "junit-platform-console-standalone", "1.6.0-M1");
  }

  Path load(Path lib, String group, String artifact, String version) throws Exception {
    var repository = "https://repo1.maven.org/maven2";
    var file = artifact + '-' + version + ".jar";
    var source = String.join("/", repository, group.replace('.', '/'), artifact, version, file);
    var target = Files.createDirectories(lib).resolve(file);
    if (Files.exists(target)) {
      return target;
    }
    var request = HttpRequest.newBuilder(URI.create(source)).GET().build();
    var response = http.send(request, HttpResponse.BodyHandlers.ofFile(target));
    if (response.statusCode() == 200) {
      return target;
    }
    throw new Error("Non-200 status code: " + response);
  }

  void run(String name, String... args) {
    var strings = args.length == 0 ? "" : '"' + String.join("\", \"", args) + '"';
    System.out.printf("| %s(%s)%n", name, strings);
    var tool = ToolProvider.findFirst(name).orElseThrow();
    int code = tool.run(System.out, System.err, args);
    if (code != 0) {
      throw new RuntimeException("Non-zero exit code: " + code);
    }
  }

  void start(String... command) throws Exception {
    var line = String.join(" ", command);
    System.out.printf("| %s%n", line);
    var process = new ProcessBuilder(command).inheritIO().start();
    if (process.waitFor() != 0) {
      throw new Error("Non-zero exit code for " + line);
    }
  }
}
