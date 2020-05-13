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
 * Zero-installation Bach.java build script.
 */

/open PRINTING

println("    ___      ___      ___      ___   ")
println("   /\\  \\    /\\  \\    /\\  \\    /\\__\\")
println("  /::\\  \\  /::\\  \\  /::\\  \\  /:/__/_")
println(" /::\\:\\__\\/::\\:\\__\\/:/\\:\\__\\/::\\/\\__\\")
println(" \\:\\::/  /\\/\\::/  /\\:\\ \\/__/\\/\\::/  /")
println("  \\::/  /   /:/  /  \\:\\__\\    /:/  /")
println("   \\/__/    \\/__/    \\/__/    \\/__/.java")
println()
println("| Java " + Runtime.version() + " on " + System.getProperty("os.name"))
println("|")

// The "/open" directive below this comment requires a constant String literal as its argument.
// Due to this restriction, the URL points
//   a) to the "master" tag/branch and
//   b) to ".bach/src/Bach.java", which requires JDK 11 or later.

println("| /open https://github.com/sormuras/bach/raw/master/.bach/src/Bach.java")
/open https://github.com/sormuras/bach/raw/master/.bach/src/Bach.java
println("| Bach.java " + Bach.VERSION)

var build = Path.of(".bach/src/Build.java")
if (Files.notExists(build)) {
  println("| Create default build program " + build);
  Files.createDirectories(build.getParent());
  Files.write(build, List.of(
      "class Build {",
      "  public static void main(String... args) {",
      "    Bach.of(project -> project).build().assertSuccessful();",
      "  }",
      "}"));
  Files.readAllLines(build).forEach(line -> println("| " + line));
}

println("| /open " + build)
/open .bach/src/Build.java

println("| Build.main()")
var code = 0
try {
  Build.main(new String[0]);
} catch (Throwable throwable) {
  println(throwable);
  code = 1;
}

println("|")
println("| Have fun! https://github.com/sponsors/sormuras (-:")
println("|")
println("| /exit " + code)
/exit code
