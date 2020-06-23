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
import de.sormuras.bach.project.MainSources;
import de.sormuras.bach.project.SourceUnit;
import de.sormuras.bach.project.Project;
import de.sormuras.bach.tool.Jar;
import de.sormuras.bach.tool.Javac;
import de.sormuras.bach.tool.Javadoc;
import java.nio.file.Path;

/** Bach's own build program. */
class Build {

  public static void main(String... args) {
    var base = Base.of();
    var version = Bach.VERSION;
    var release = 11;
    var project =
        Project.of("bach", version.toString())
            .with(base)
            .with(JavaRelease.of(release))
            // .with(Documentation.of("\uD83C\uDFBC Bach.java"))
            // .requires("org.junit.platform.console")
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
                            .withRecommendedWarnings()
                            .withWarnings("all", "-preview")
                            .withTerminateCompilationIfWarningsOccur())
                    .with(
                        Javadoc.of()
                            .with("-d", base.workspace("documentation", "api"))
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
                                        base.classes("", release, "de.sormuras.bach"), "."))));

    Bach.of(project).build();
  }
}
