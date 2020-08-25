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

if (java.lang.module.ModuleFinder.of(Path.of(".bach/lib")).find("de.sormuras.bach").isEmpty()) {
  var version = System.getProperty("version", "11.8");
  var uri = version.endsWith("SNAPSHOT")
              ? "https://jitpack.io/com/github/sormuras/bach/" + version + "/bach-" + version + ".jar"
              : "https://repo.maven.apache.org/maven2/de/sormuras/bach/de.sormuras.bach/" + version + "/de.sormuras.bach-" + version + ".jar";
  var jar = Files.createDirectories(Path.of(".bach/lib")).resolve("de.sormuras.bach@" + version + ".jar");
  try (var stream = new URL(uri).openStream()) { Files.copy(stream, jar); }
}

/env --module-path .bach/lib --add-modules de.sormuras.bach

import de.sormuras.bach.*
import de.sormuras.bach.project.*
import de.sormuras.bach.tool.*

/open PRINTING

println("    ___      ___      ___      ___   ")
println("   /\\  \\    /\\  \\    /\\  \\    /\\__\\")
println("  /::\\  \\  /::\\  \\  /::\\  \\  /:/__/_")
println(" /::\\:\\__\\/::\\:\\__\\/:/\\:\\__\\/::\\/\\__\\")
println(" \\:\\::/  /\\/\\::/  /\\:\\ \\/__/\\/\\::/  /")
println("  \\::/  /   /:/  /  \\:\\__\\    /:/  /")
println("   \\/__/    \\/__/    \\/__/    \\/__/.java")
println()
println("             Bach " + Bach.VERSION)
println("     Java Runtime " + Runtime.version())
println(" Operating System " + System.getProperty("os.name"))
println()
