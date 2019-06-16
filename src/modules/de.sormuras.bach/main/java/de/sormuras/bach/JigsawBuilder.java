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
/** Build, i.e. compile and package, a modular Java project. */
public /*STATIC*/ class JigsawBuilder implements Action {

  @Override
  public void perform(Bach bach) throws Exception {
    var home = bach.run.home;
    var work = bach.run.work;
    var lib = home.resolve("lib");
    var src = home.resolve("src");

    var libTest = lib.resolve("test");
    var libTestJUnitPlatform = lib.resolve("test-junit-platform");
    var libTestRuntimeOnly = lib.resolve("test-runtime-only");
    var sourceMainResources = src.resolve(Path.of("modules", "de.sormuras.bach", "main/resources"));
    var sourceTestResources = src.resolve(Path.of("modules", "de.sormuras.bach", "test/resources"));
    var targetMainClasses = work.resolve("main/classes");
    var targetMainModules = Files.createDirectories(work.resolve("main/modules"));
    var targetTestClasses = work.resolve("test/classes");
    var targetTestModules = Files.createDirectories(work.resolve("test/modules"));

    bach.run.run(
        new Command("javac")
            .add("-d", targetMainClasses)
            .add("--module-source-path", src + "/modules/*/main/java")
            .add("--module-version", "1-" + bach.project.version)
            .add("--module", "de.sormuras.bach"));

    bach.run.run(
        new Command("jar")
            .add("--create")
            .addIff(bach.run.debug, "--verbose")
            .add("--file", targetMainModules.resolve("de.sormuras.bach.jar"))
            .add("-C", targetMainClasses.resolve("de.sormuras.bach"))
            .add(".")
            .addIff(
                Files.isDirectory(sourceMainResources),
                cmd -> cmd.add("-C", sourceMainResources).add(".")));

    bach.run.run(
        new Command("javac")
            .add("-d", targetTestClasses)
            .add("--module-source-path", src + "/modules/*/test/java")
            .add("--module-version", "1-" + bach.project.version)
            .add("--module-path", List.of(targetMainModules, libTest))
            .add("--module", "integration"));

    bach.run.run(
        new Command("jar")
            .add("--create")
            .addIff(bach.run.debug, "--verbose")
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
                List.of(
                    targetTestModules,
                    targetMainModules,
                    libTest,
                    libTestJUnitPlatform,
                    libTestRuntimeOnly))
            .add("--add-modules", "integration")
            .add("--module", "org.junit.platform.console")
            .add("--scan-modules");
    bach.run.log(INFO, "%s %s", test.name, String.join(" ", test.list));
  }
}
