//usr/bin/env jshell --show-version "$0" "$@"; exit $?

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

/*
 * Declare constants and helper methods.
 */
var version = "master"
var target = Files.createDirectories(Paths.get(".bach/" + version))

/open PRINTING

int start(String executable, String... args) throws Exception {
 	var processBuilder = new ProcessBuilder(executable);
 	Arrays.stream(args).forEach(processBuilder.command()::add);
 	processBuilder.redirectErrorStream(true);
 	var process = processBuilder.start();
 	process.getInputStream().transferTo(System.out);
 	return process.waitFor();
 }

/*
 * Banner!
 */
println(" ________   ________   ________   ___  ___     ")
println("|\\   __  \\ |\\   __  \\ |\\   ____\\ |\\  \\|\\  \\    ")
println("\\ \\  \\|\\ /_\\ \\  \\|\\  \\\\ \\  \\___| \\ \\  \\\\\\  \\   ")
println(" \\ \\   __  \\\\ \\   __  \\\\ \\  \\     \\ \\   __  \\  ")
println("  \\ \\  \\|\\  \\\\ \\  \\ \\  \\\\ \\  \\____ \\ \\  \\ \\  \\ ")
println("   \\ \\_______\\\\ \\__\\ \\__\\\\ \\_______\\\\ \\__\\ \\__\\")
println("    \\|_______| \\|__|\\|__| \\|_______| \\|__|\\|__|")
println()
println("     Java Shell Builder - " + version)
println()

/*
 * Download "Bach.java" and other assets from GitHub to local directory.
 */
println()
println("Downloading assets to " + target + "...")
println()
var context = new URL("https://github.com/sormuras/bach/raw/" + version + "/src/bach/")
for (var asset : Set.of(target.resolve("Bach.java"), target.resolve("Bach.jsh"))) {
  if (Files.exists(asset) && !version.equals("master")) continue;
  var source = new URL(context, asset.getFileName().toString());
  println("Loading " + source + "...");
  try (var stream = source.openStream()) {
    Files.copy(stream, asset, StandardCopyOption.REPLACE_EXISTING);
  }
  println("     -> " + asset.toAbsolutePath());
}

println()
println("Executing Bach.java BOOT...")
println()
var bach = target.resolve("Bach.java").toString()
var code = start("java", "-Dbach.log.level=WARNING", bach, "BOOT")

println()
println("Bootstrap and initial build finished. You now may use the following command:")
println("")
println("java " + bach + " <actions>")
println()
code += start("java", "-Dbach.log.level=OFF", bach, "HELP")

/exit code
