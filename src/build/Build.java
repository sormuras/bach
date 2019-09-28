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

import java.io.File;
import java.io.PrintWriter;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;
import java.util.List;

/** Use {@code Bach.java} to build {@code de.sormuras.bach} module. */
public class Build {
  public static void main(String... args) {
    PrintWriter out = new PrintWriter(System.out, true);
    PrintWriter err = new PrintWriter(System.err, true);
    new Bach(out, err, true, project("2-ea")).build();
  }

  private static Bach.Project project(String version) {
    var main = main();
    var test = test(main);
    return new Bach.Project(
        Path.of(""),
        Path.of("bin"),
        "bach",
        ModuleDescriptor.Version.parse(version),
        new Bach.Project.Library(Path.of("lib")),
        List.of(main, test));
  }

  private static Bach.Project.Realm main() {
    return new Bach.Project.Realm(
        "main",
        false,
        11,
        String.join(File.separator, "src", "modules", "*", "main", "java"),
        List.of(
            new Bach.Project.ModuleSourceUnit(
                Bach.Project.ModuleInfoReference.of(
                    Path.of("src/modules/de.sormuras.bach/main/java/module-info.java")),
                List.of(Path.of("src/modules/de.sormuras.bach/main/java")),
                List.of(Path.of("src/modules/de.sormuras.bach/main/resources")))));
  }

  private static Bach.Project.Realm test(Bach.Project.Realm main) {
    return new Bach.Project.Realm(
        "test",
        true,
        Runtime.version().feature(),
        String.join(File.separator, "src", "modules", "*", "test", "java"),
        List.of(
            new Bach.Project.ModuleSourceUnit(
                Bach.Project.ModuleInfoReference.of(
                    Path.of("src/modules/it/test/java/module-info.java")),
                List.of(Path.of("src/modules/it/test/java")),
                List.of(Path.of("src/modules/it/test/resources")))),
        main);
  }
}
