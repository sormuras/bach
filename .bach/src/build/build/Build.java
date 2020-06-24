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

package build;

import de.sormuras.bach.Bach;
import de.sormuras.bach.project.Base;
import de.sormuras.bach.project.JavaRelease;
import de.sormuras.bach.project.Locator;
import de.sormuras.bach.project.MainSources;
import de.sormuras.bach.project.SourceDirectory;
import de.sormuras.bach.project.SourceUnit;
import de.sormuras.bach.project.Project;
import de.sormuras.bach.project.TestSources;
import de.sormuras.bach.tool.Jar;
import de.sormuras.bach.tool.Javac;
import de.sormuras.bach.tool.Javadoc;
import java.io.File;
import java.nio.file.Path;

/** Bach's own build program. */
class Build {

  public static void main(String... args) {
    var base = Base.of();
    var version = Bach.VERSION;
    var release = 11;
    var feature = Runtime.version().feature();
    var project =
        Project.of("bach", version.toString())
            .with(base)
            .with(JavaRelease.of(release))
            // .with(Documentation.of("\uD83C\uDFBC Bach.java"))
            .with(
                MainSources.of()
                    .with(
                        Javac.of()
                            .with("-d", base.classes("", release))
                            .with("--module", "de.sormuras.bach")
                            .with("--module-source-path", "src/*/main/java")
                            .with("--module-version", version)
                            .withCompileForJavaRelease(release)
                            .with("-encoding", "UTF-8")
                            .with("-parameters")
                            .withRecommendedWarnings()
                            .withTerminateCompilationIfWarningsOccur())
                    .with(
                        Javadoc.of()
                            .with("-d", base.documentation("api"))
                            .with("--module", "de.sormuras.bach")
                            .with("--module-source-path", "src/*/main/java")
                            .with("-encoding", "UTF-8")
                            .with("-locale", "en")
                            .with("-Xdoclint")
                            .with("-Xwerror") // https://bugs.openjdk.java.net/browse/JDK-8237391
                            .with("--show-module-contents", "all")
                            .with("-link", "https://docs.oracle.com/en/java/javase/11/docs/api"))
                    .with(
                        SourceUnit.of(Path.of("src/de.sormuras.bach/main/java"))
                            .with(
                                Jar.of(
                                        base.modules("")
                                            .resolve("de.sormuras.bach@" + version + ".jar"))
                                    .with("--verbose")
                                    .with("--main-class", "de.sormuras.bach.Main")
                                    .withChangeDirectoryAndIncludeFiles(
                                        base.classes("", release, "de.sormuras.bach"), ".")
                                    .withChangeDirectoryAndIncludeFiles(
                                        Path.of("src/de.sormuras.bach/main/java"), "."))))
            .with(
                TestSources.of()
                    .with(
                        Javac.of()
                            .with("-d", base.classes("test", feature))
                            .with("--module", "de.sormuras.bach,test.base")
                            .with("--module-version", version + "-test")
                            .with(
                                "--module-source-path",
                                "src/*/test/java" + File.pathSeparator + "src/*/test/java-module")
                            .with("--module-path", "lib")
                            .with(
                                "--patch-module",
                                "de.sormuras.bach="
                                    + base.modules("")
                                        .resolve("de.sormuras.bach@" + version + ".jar"))
                            .with("-encoding", "UTF-8")
                            .withWarnings("all", "-preview")
                            .withRecommendedWarnings()
                            .withTerminateCompilationIfWarningsOccur())
                    .with(
                        SourceUnit.of(Path.of("src/de.sormuras.bach/test/java-module"))
                            .with(SourceDirectory.of(Path.of("src/de.sormuras.bach/test/java")))
                            .with(
                                Jar.of(
                                        base.modules("test")
                                            .resolve("de.sormuras.bach@" + version + "-test.jar"))
                                    .withChangeDirectoryAndIncludeFiles(
                                        base.classes("test", feature, "de.sormuras.bach"), ".")
                                    .withChangeDirectoryAndIncludeFiles(
                                        base.classes("", release, "de.sormuras.bach"), ".")),
                        SourceUnit.of(Path.of("src/test.base/test/java"))
                            .with(
                                Jar.of(
                                        base.modules("test")
                                            .resolve("test.base@" + version + "-test.jar"))
                                    .withChangeDirectoryAndIncludeFiles(
                                        base.classes("test", feature, "test.base"), "."))))
            .with(
                Locator.ofJUnitPlatform("commons", "1.7.0-M1"),
                Locator.ofJUnitPlatform("console", "1.7.0-M1"),
                Locator.ofJUnitPlatform("engine", "1.7.0-M1"),
                Locator.ofJUnitPlatform("launcher", "1.7.0-M1"),
                Locator.ofJUnitPlatform("reporting", "1.7.0-M1"),
                Locator.ofJUnitPlatform("testkit", "1.7.0-M1"))
            .with(
                Locator.ofJUnitJupiter("", "5.7.0-M1"),
                Locator.ofJUnitJupiter("api", "5.7.0-M1"),
                Locator.ofJUnitJupiter("engine", "5.7.0-M1"),
                Locator.ofJUnitJupiter("params", "5.7.0-M1"))
            .with(
                Locator.ofCentral("org.apiguardian.api", "org.apiguardian:apiguardian-api:1.1.0"),
                Locator.ofCentral("org.opentest4j", "org.opentest4j:opentest4j:1.2.0"))
            .withRequires("org.junit.platform.console");

    Bach.of(project).build();
  }
}
