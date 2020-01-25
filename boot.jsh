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
 * Declare constants.
 */
String VERSION = "master"
var version = System.getProperty("Bach.java/version", VERSION)
var source = new URL("https://github.com/sormuras/bach/raw/" + version + "/src/.bach/")
var target = Path.of(".bach/src")
var bach11 = target.resolve("Bach11.java")
var bach14 = target.resolve("Bach14.java")
var build11 = target.resolve("Build11.java")
var build14 = target.resolve("Build14.java")

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
println("   \\/__/    \\/__/    \\/__/    \\/__/.java")
println()
println("     Java Shell Builder - " + version)
println("     https://github.com/sormuras/bach")
println()

/*
 * Download build tool and other assets from GitHub to local directory.
 */
println()
println("Download assets to " + target.toAbsolutePath() + "...")
Files.createDirectories(target)
for (var asset : Set.of(bach11, build11, bach14, build14)) {
  if (Files.exists(asset)) {
    println("  skip download -- using existing file: " + asset);
  } else {
    var remote = new URL(source, asset.getFileName().toString());
    println("Load " + remote + "...");
    try (var stream = remote.openStream()) {
      Files.copy(stream, asset, StandardCopyOption.REPLACE_EXISTING);
    }
    println("  -> " + asset);
  }
}

/*
 * Generate local launchers for JDK 11 and 14.
 */
var javac11 = "javac -d .bach/boot " + bach11 + " " + build11
var javac14 = "javac -d .bach/boot --enable-preview --release 14 " + bach14 + " " + build14
var java11 = "java -cp .bach/boot Build11"
var java14 = "java -cp .bach/boot --enable-preview Build14"
println()
println("Generating local launchers and initial configuration...")
println("  -> " + Files.write(Path.of("bach11"), List.of("/usr/bin/env " + javac11, "/usr/bin/env " + java11 + " \"$@\"")).toFile().setExecutable(true))
println("  -> " + Files.write(Path.of("bach11.bat"), List.of("@ECHO OFF", javac11, java11 + " %*")))
println("  -> " + Files.write(Path.of("bach14"), List.of("/usr/bin/env " + javac14, "/usr/bin/env " + java14 + " \"$@\"")).toFile().setExecutable(true))
println("  -> " + Files.write(Path.of("bach14.bat"), List.of("@ECHO OFF", javac14, java14 + " %*")))

/*
 * Print some help and wave goodbye.
 */
println()
println("Bach.java bootstrap finished. Use the following command to build your project:")
println()
println("    Linux: ./bach[11|14] <args...>")
println("  Windows: bach[11|14] <args...>")
println()
println("Have fun! https://github.com/sponsors/sormuras (-:")
println()

/exit
