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

import java.lang.System.Logger.Level;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.List;

/** Bach's own build program. */
class Build {

  public static void main(String... args) {
    var printer = Bach.Printer.ofSystem(Level.ALL);
    var workspace = Bach.Workspace.of();
    var bach = new Bach(printer, workspace, HttpClient::newHttpClient);
    bach.build(project("Bach.java", "11.0-ea"));
  }

  static Bach.Project project(String name, String version) {
    var workspace = Bach.Workspace.of();
    return new Bach.Project(
        name,
        Version.parse(version),
        new Bach.Information(
            "\uD83C\uDFBC Java Shell Builder - Build modular Java projects with JDK tools",
            URI.create("https://github.com/sormuras/bach")),
        new Bach.Structure(
            List.of(
                mainRealm(workspace)
                // , testRealm(workspace) TODO Needs JavaArchive of main realm and library support
                // , testPreview(workspace) TODO Needs JavaArchive of main and test realm + library
                ),
            "main"));
  }

  static Bach.Realm mainRealm(Bach.Workspace workspace) {
    var classes = workspace.classes("main", 11);
    return new Bach.Realm(
        "main",
        List.of(
            new Bach.Unit(
                ModuleDescriptor.newModule("de.sormuras.bach").build(),
                Bach.Directory.listOf(Path.of("src/de.sormuras.bach/main")))),
        "de.sormuras.bach",
        Bach.Tool.javac(
            List.of(
                new Bach.JavaCompiler.CompileModulesCheckingTimestamps(List.of("de.sormuras.bach")),
                new Bach.JavaCompiler.CompileForJavaRelease(11),
                new Bach.JavaCompiler.ModuleSourcePathInModulePatternForm(
                    List.of("src/*/main/java")),
                new Bach.JavaCompiler.DestinationDirectory(classes) //
                ) //
            ));
  }

  static Bach.Realm testRealm(Bach.Workspace workspace) {
    var classes = workspace.classes("test", 11);
    return new Bach.Realm(
        "test",
        List.of(
            new Bach.Unit(
                ModuleDescriptor.newOpenModule("de.sormuras.bach").build(),
                Bach.Directory.listOf(Path.of("src/de.sormuras.bach/test"))),
            new Bach.Unit(
                ModuleDescriptor.newOpenModule("test.base").build(),
                Bach.Directory.listOf(Path.of("src/test.base/test")))),
        null,
        Bach.Tool.javac(
            List.of(
                new Bach.JavaCompiler.CompileModulesCheckingTimestamps(
                    List.of("de.sormuras.bach", "test.base")),
                new Bach.JavaCompiler.CompileForJavaRelease(11),
                new Bach.JavaCompiler.ModuleSourcePathInModulePatternForm(
                    List.of("src/*/test/java", "src/*/test/java-module")),
                new Bach.JavaCompiler.DestinationDirectory(classes) //
                ) //
            )
        //
        );
  }

  static Bach.Realm testPreview(Bach.Workspace workspace) {
    var classes = workspace.classes("test-preview", 14);
    return new Bach.Realm(
        "test-preview",
        List.of(
            new Bach.Unit(
                ModuleDescriptor.newOpenModule("test.modules").build(),
                Bach.Directory.listOf(Path.of("src/test.modules/test-preview")))),
        null,
        Bach.Tool.javac(
            List.of(
                new Bach.JavaCompiler.CompileModulesCheckingTimestamps(List.of("test.modules")),
                new Bach.JavaCompiler.CompileForJavaRelease(14),
                new Bach.JavaCompiler.EnablePreviewLanguageFeatures(),
                new Bach.JavaCompiler.ModuleSourcePathInModulePatternForm(
                    List.of("src/*/test-preview/java")),
                new Bach.JavaCompiler.DestinationDirectory(classes) //
                ) //
            ) //
        );
  }
}
