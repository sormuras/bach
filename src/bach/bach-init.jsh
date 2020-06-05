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

/open https://github.com/sormuras/bach/raw/master/BUILDING

if (java.lang.module.ModuleFinder.of(Path.of("lib")).find("de.sormuras.bach").isEmpty()) {
  println();
  println("Load module de.sormuras.bach via JitPack");
  println("\thttps://jitpack.io/com/github/sormuras/bach/master-SNAPSHOT/build.log";
  get("lib", URI.create("https://jitpack.io/com/github/sormuras/bach/master-SNAPSHOT/bach-master-SNAPSHOT.jar"));
}

/open .bach/src/Bach.java
println()
exe("java", "--module-path", "lib", "--module", "de.sormuras.bach", "help")

println()
println("Bach.java initialized. Use the following commands to build your project:")
println()
println("- java ...")
println("    Launch Bach.java's main program.")
println()
println("- java ...")
println("    Launch your custom build program.")
println("    Edit ...")
println("    to customize your build program even further.")
println()
println("Have fun! https://github.com/sponsors/sormuras (-:")
println()

/exit
