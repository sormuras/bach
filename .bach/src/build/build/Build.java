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
import de.sormuras.bach.project.MainSources;
import de.sormuras.bach.project.ModuleSource;
import de.sormuras.bach.project.Project;
import java.nio.file.Path;

/** Bach's own build program. */
class Build {

  public static void main(String... args) {
    var project =
        Project.of("bach", Bach.VERSION.toString())
            // .title("\uD83C\uDFBC Bach.java")
            // .compileForJavaRelease(11)
            // .terminateCompilationIfWarningsOccur(true)
            // .requires("org.junit.platform.console")
            .with(
                MainSources.of().with(ModuleSource.of(Path.of("src/de.sormuras.bach/main/java"))));

    Bach.of(project).build();
  }
}
