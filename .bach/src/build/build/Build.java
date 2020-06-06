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
import de.sormuras.bach.Scanner;
import de.sormuras.bach.Sequencer;

/**
 * Bach's own build program.
 */
class Build {

  public static void main(String... args) {
    Bach.of(
            scanner -> scanner.offset("src").limit(5).layout(Scanner.Layout.MAIN_TEST_PREVIEW),
            project ->
                project
                    .title("\uD83C\uDFBC Bach.java")
                    .version(Bach.VERSION.toString())
                    .compileForJavaRelease(11)
                    .terminateCompilationIfWarningsOccur(true)
                    .requires("org.junit.platform.console"))
        .build(
            (arguments, project, map) -> {
              Sequencer.Tuner.defaults(arguments, project, map);
              var tool = map.get("tool");
              // any tool call
              if ("javadoc".equals(tool)) {
                arguments.add("-link", "https://docs.oracle.com/en/java/javase/11/docs/api");
              }
              // "test-preview" realm tool cool
              if ("test-preview".equals(map.get("realm"))) {
                if ("javac".equals(tool)) arguments.put("-Xlint:-preview");
              }
            })
        .assertSuccessful()
        .printModuleStats();
  }
}
