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

/*
 * Read input and declare constants.
 */
var version = System.getProperty("version", "master")
var upgrade = Boolean.parseBoolean(System.getProperty("upgrade", "false"))

/*
 * Compute and set variables.
 */
var source = "https://github.com/sormuras/bach/raw/" + version
var target = Path.of(System.getProperty("target", ".bach"))
var bach = target.resolve("src/Bach.java")
var build = target.resolve("src/Build.java")
var script = target.resolve("build.jsh")

/*
 * Source printing-related methods into this JShell session.
 */
/open PRINTING

/*
 * Banner!
 */
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

/*
 * Download Bach.java and other assets from GitHub to local project directory.
 */
println()
println("Download assets to " + target.toAbsolutePath() + "...")
for (var asset : List.of(bach, script)) {
  if (Files.notExists(asset) || upgrade) {
    Files.createDirectories(asset.getParent());
    var remote = new URL(source + '/' + asset.toString().replace('\\', '/'));
    println("  | " + remote + "...");
    try (var stream = remote.openStream()) {
      Files.copy(stream, asset, StandardCopyOption.REPLACE_EXISTING);
    }
    println("  +-> " + asset.toUri());
  } else {
    println("  | reuse existing " + asset.toUri());
  }
}

/*
 * Generate default build program.
 */
if (Files.notExists(build)) {
  println();
  println("Write default build program " + build);
  Files.createDirectories(build.getParent());
  Files.write(build, List.of(
      "class Build {",
      "  public static void main(String... args) {",
      "    Bach.of(project -> project).build().assertSuccessful();",
      "  }",
      "}"));
  Files.readAllLines(build).forEach(line -> println("\t" + line));
}

/*
 * Generate default git ignore configuration file.
 */
println()
println("Generate default .gitignore configuration.")
Files.write(target.resolve(".gitignore"), List.of("/workspace/"))

/*
 * Smoke test Bach.java by printing its version and help text.
 */
/open .bach/src/Bach.java
println()
Bach.main("help")

/*
 * Print some help and wave goodbye.
 */
println()
println("Bach.java bootstrap finished. Use the following commands to build your project:")
println()
println("- java " + bach)
println("    Launch Bach.java's main program.")
println()
println("- jshell " + target.resolve("build.jsh"))
println("    Launch your custom build program.")
println("    Edit " + build.toUri())
println("    to customize your build program even further.")
println()
println("Have fun! https://github.com/sponsors/sormuras (-:")
println()

/exit
