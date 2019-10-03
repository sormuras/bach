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
import java.net.URI;
import java.nio.file.Path;
import java.util.List;

/** Use {@code Bach.java} to build {@code de.sormuras.bach} module. */
public class Build {
  public static void main(String... args) {
    PrintWriter out = new PrintWriter(System.out, true);
    PrintWriter err = new PrintWriter(System.err, true);
    new Bach(out, err, true, project("1.9-ea")).build();
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
    var main = Path.of("src/modules/de.sormuras.bach/main");
    return new Bach.Project.Realm(
        "main",
        false,
        11,
        String.join(File.separator, "src", "modules", "*", "main", "java"),
        new Bach.Project.ToolArguments(
            Bach.Project.ToolArguments.JAVAC,
            new Bach.Project.Deployment(
                "bintray-sormuras-maven",
                URI.create("https://api.bintray.com/maven/sormuras/maven/bach/;publish=0"))),
        List.of(
            new Bach.Project.ModuleUnit(
                Bach.Project.ModuleInfo.of(main.resolve("java/module-info.java")),
                List.of(Bach.Project.Source.of(main.resolve("java"))),
                List.of(main.resolve("resources")),
                main.resolve("maven/pom.xml"))));
  }

  private static Bach.Project.Realm test(Bach.Project.Realm main) {
    var test = Path.of("src/modules/it/test");
    return new Bach.Project.Realm(
        "test",
        true,
        Runtime.version().feature(),
        String.join(File.separator, "src", "modules", "*", "test", "java"),
        Bach.Project.ToolArguments.of(),
        List.of(
            new Bach.Project.ModuleUnit(
                Bach.Project.ModuleInfo.of(test.resolve("java/module-info.java")),
                List.of(Bach.Project.Source.of(test.resolve("java"))),
                List.of(test.resolve("resources")),
                null)),
        main);
  }
}
