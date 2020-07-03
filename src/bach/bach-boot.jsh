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

var version = System.getProperty("version", "11.3-M1")
var lib = Path.of(".bach/lib")

/open PRINTING

println("    ___      ___      ___      ___   ")
println("   /\\  \\    /\\  \\    /\\  \\    /\\__\\")
println("  /::\\  \\  /::\\  \\  /::\\  \\  /:/__/_")
println(" /::\\:\\__\\/::\\:\\__\\/:/\\:\\__\\/::\\/\\__\\")
println(" \\:\\::/  /\\/\\::/  /\\:\\ \\/__/\\/\\::/  /")
println("  \\::/  /   /:/  /  \\:\\__\\    /:/  /")
println("   \\/__/    \\/__/    \\/__/    \\/__/.java " + version)
println()
println(" Java " + Runtime.version() + " on " + System.getProperty("os.name"))
println()

int java(Object... args) throws Exception {
  var java = "java"; // TODO find sibling to jshell executable...
  var processBuilder = new ProcessBuilder(java);
  Arrays.stream(args).forEach(arg -> processBuilder.command().add(arg.toString()));
  processBuilder.redirectErrorStream(true);
  var process = processBuilder.start();
  process.getInputStream().transferTo(System.out);
  return process.waitFor();
}

Path load(String file, String uri) throws Exception {
  var path = Path.of(file).toAbsolutePath().normalize();
  Files.createDirectories(path.getParent());
  try (var stream = new URL(uri).openStream()) { Files.copy(stream, path); }
  return path;
}

if (java.lang.module.ModuleFinder.of(lib).find("de.sormuras.bach").isEmpty()) {
  println();
  println("Load module de.sormuras.bach via JitPack");
  println("    https://jitpack.io/com/github/sormuras/bach/" + version + "/build.log");
  load(".bach/lib/de.sormuras.bach@" + version + ".jar", "https://jitpack.io/com/github/sormuras/bach/" + version + "/bach-" + version + ".jar");
}

if (Files.notExists(Path.of(".bach/.gitignore"))) {
  println();
  println("Generate default .gitignore configuration.");
  Files.write(Path.of(".bach/.gitignore"), List.of("/workspace/", "/lib/"));
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
      "",
      "class Build {",
      "  public static void main(String... args) {",
      "    Bach.of(project -> project.withVersion(\"1-ea\")).buildProject();",
      "  }",
      "}"));
}

println()
int code = java("--module-path", lib.toString(), "--module", "de.sormuras.bach", "help")

println()
println("Bach.java initialized. Use the following commands to build your project:")
println()
println("- java --module-path " + lib + " --module de.sormuras.bach <action>")
println("    Launch Bach.java's main program.")
println()
println("- java --module-path " + lib + " --add-modules de.sormuras.bach " + build)
println("    Launch your custom build program.")
println("    Edit " + build.toUri())
println("    to customize your build program.")

println()
println("Have fun! https://github.com/sponsors/sormuras (-:")
println()
/exit code
