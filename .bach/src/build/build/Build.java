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
import de.sormuras.bach.project.ModuleSource;
import de.sormuras.bach.project.Project;
import de.sormuras.bach.tool.Jar;
import de.sormuras.bach.tool.Javac;
import java.nio.file.Path;

/** Bach's own build program. */
class Build {

  public static void main(String... args) {
    var base = Base.of();
    var release = 11;
    var project =
        Project.of("bach", Bach.VERSION.toString())
            .with(base)
            .with(JavaRelease.of(release))
            // .title("\uD83C\uDFBC Bach.java")
            // .requires("org.junit.platform.console")
            .with(
                MainSources.of()
                    .with(
                        Javac.of()
                            .with("-d", base.classes("", release))
                            .with("--module", "de.sormuras.bach")
                            .with("--module-source-path", "src/*/main/java")
                            .withCompileForJavaRelease(release)
                            .withRecommendedWarnings()
                            .withWarnings("all", "-preview")
                            .withTerminateCompilationIfWarningsOccur()
                            .with("--module-version", Bach.VERSION)
                            .with("-encoding", "UTF-8"))
                    .with(
                        ModuleSource.of(Path.of("src/de.sormuras.bach/main/java"))
                            .with(
                                Jar.of(
                                        base.modules("")
                                            .resolve("de.sormuras.bach@" + Bach.VERSION + ".jar"))
                                    .with("--verbose")
                                    .withChangeDirectoryAndIncludeFiles(
                                        base.classes("", release, "de.sormuras.bach"), "."))));

    Bach.of(project).build();
  }
}
