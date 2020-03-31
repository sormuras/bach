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

println("| Java " + Runtime.version())

println("| /open src/bach/SingleFileSourceCodeGenerator.java")
/open src/bach/SingleFileSourceCodeGenerator.java
SingleFileSourceCodeGenerator.main()

println("| /open .bach/src/Bach.java")
/open .bach/src/Bach.java
var bach = new Bach()
println("| Script uses " + bach)

println("| Declare default build program class")
class Build {
  public static void main(String... args) {
    throw new UnsupportedOperationException("Default build program is not yet implemented");
  }
}

println("| /open .bach/src/Build.java")
/open .bach/src/Build.java

println("| Build.main()")
var code = 0
try {
  Build.main();
} catch (Throwable throwable) {
  System.err.println("Build.main() failed: " + throwable);
  code = 1;
}

println("| /exit " + code)
/exit code
