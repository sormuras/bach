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

package de.sormuras.bach;

import static java.lang.System.Logger.Level.INFO;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/*BODY*/
public /*STATIC*/ class JigsawBuilder implements Action {

  @Override
  public void perform(Bach bach) throws Exception {
    var libTest = Path.of("lib/test");
    var libTestRuntimeOnly = Path.of("lib/test-runtime-only");
    var sourceMainResources = Path.of("src/modules", "de.sormuras.bach", "main/resources");
    var sourceTestResources = Path.of("src/modules", "de.sormuras.bach", "test/resources");
    var targetMainClasses = Path.of("target/build/main/classes");
    var targetMainModules = Files.createDirectories(Path.of("target/build/main/modules"));
    var targetTestClasses = Path.of("target/build/test/classes");
    var targetTestModules = Files.createDirectories(Path.of("target/build/test/modules"));

    bach.run.run(
        new Command("javac")
            .add("-d", targetMainClasses)
            .add("--module-source-path", "src/modules/*/main/java")
            .add("--module-version", "1-" + bach.project.version)
            .add("--module", "de.sormuras.bach"));

    bach.run.run(
        new Command("jar")
            .add("--create")
            .addIff(true, "--verbose")
            .add("--file", targetMainModules.resolve("de.sormuras.bach.jar"))
            .add("-C", targetMainClasses.resolve("de.sormuras.bach"))
            .add(".")
            .addIff(
                Files.isDirectory(sourceMainResources),
                cmd -> cmd.add("-C", sourceMainResources).add(".")));

    bach.run.run(
        new Command("javac")
            .add("-d", targetTestClasses)
            .add("--module-source-path", "src/modules/*/test/java")
            .add("--module-version", "1-" + bach.project.version)
            .add("--module-path", List.of(targetMainModules, libTest))
            .add("--module", "integration"));

    bach.run.run(
        new Command("jar")
            .add("--create")
            .addIff(true, "--verbose")
            .add("--file", targetTestModules.resolve("integration.jar"))
            .add("-C", targetTestClasses.resolve("integration"))
            .add(".")
            .addIff(
                Files.isDirectory(sourceTestResources),
                cmd -> cmd.add("-C", sourceTestResources).add(".")));

    var test =
        new Command("java")
            .add(
                "--module-path",
                List.of(targetTestModules, targetMainModules, libTest, libTestRuntimeOnly))
            .add("--add-modules", "integration")
            .add("--module", "org.junit.platform.console")
            .add("--scan-modules");
    bach.run.log(INFO, "%s %s", test.name, String.join(" ", test.list));
  }
}
