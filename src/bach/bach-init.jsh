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

/open https://github.com/sormuras/bach/raw/11.9/src/bach/bach-boot.jsh

var gitignore = Path.of(".bach/.gitignore")
if (Files.notExists(gitignore)) {
  println();
  println("Generate default " + gitignore + " configuration.");
  Files.write(gitignore, List.of("/workspace/", "/lib/"));
}

var build = Path.of(".bach/src/build/build/Build.java")
if (Files.notExists(build)) {
  println();
  println("Write default build program " + build);
  Files.createDirectories(build.getParent());
  Files.write(Path.of(".bach/src/build/module-info.java"), List.of("module build {","  requires de.sormuras.bach;","}"));
  Files.write(build, List.of(
      "package build;",
      "",
      "import de.sormuras.bach.Bach;",
      "import de.sormuras.bach.Configuration;",
      "import de.sormuras.bach.Project;",
      "",
      "class Build {",
      "  public static void main(String... args) {",
      "    var configuration = Configuration.ofSystem();",
      "    var project = Project.ofCurrentDirectory();",
      "    new Bach(configuration, project).build();",
      "  }",
      "}"));
}

if (Files.notExists(Path.of(".bach/build"))) {
  println();
  println("Generate local launchers in directory .bach");
  var command = "java --module-path " + Path.of(".bach/lib") + " --add-modules de.sormuras.bach " + build;
  Files.write(Path.of(".bach/build"), List.of("/usr/bin/env " + command + " \"$@\"")).toFile().setExecutable(true);
  Files.write(Path.of(".bach/build.bat"), List.of("@ECHO OFF", command + " %*"));
}

Bach.main("info")

println()
println("Bach.java initialized. Use the following commands to build your project:")
println()
println("- java --module-path " + Path.of(".bach/lib") + " --module de.sormuras.bach <action>")
println("    Launch Bach.java's main program.")
println()
println("- .bach/build[.bat]")
println("    Launch your custom build program.")
println("    Edit " + build.toUri())
println("    to customize your build program.")

println()
println("Have fun! https://github.com/sponsors/sormuras (-:")
println()

/exit
