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

var boot = Path.of(".bach/lib/bach-boot.jsh")
if (Files.notExists(boot)) {
  var version = System.getProperty("version", "11.5");
  var uri = "https://github.com/sormuras/bach/raw/" + version + "/src/bach/bach-boot.jsh";
  Files.createDirectories(boot.getParent());
  try (var stream = new URL(uri).openStream()) { Files.copy(stream, boot); }
}

/open .bach/lib/bach-boot.jsh

var code = 0
try {
  var build = Path.of(".bach/src/build/build/Build.java");
  if (Files.exists(build)) {
    var java = ProcessHandle.current().info().command().orElse("java");
    var processBuilder = new ProcessBuilder(java, "--module-path", ".bach/lib", "--add-modules", "de.sormuras.bach", build.toString());
    processBuilder.redirectErrorStream(true);
    var process = processBuilder.start();
    process.getInputStream().transferTo(System.out);
    code = process.waitFor();
  }
  else {
    Bach.of(project -> project).build();
  }
} catch (Throwable throwable) {
  println(throwable);
  code = 1;
}

println()
println("Have fun! https://github.com/sponsors/sormuras (-:")
println()

/exit code
