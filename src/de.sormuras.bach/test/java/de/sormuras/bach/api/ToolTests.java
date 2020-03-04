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

package de.sormuras.bach.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.io.File;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ToolTests {

  @Test
  void javac() {
    var javac =
        Tool.javac()
            .setCompileModulesCheckingTimestamps(List.of("a", "b", "c"))
            .setVersionOfModulesThatAreBeingCompiled(Version.parse("123"))
            .setPathsWhereToFindSourceFilesForModules(List.of(Path.of("src/{MODULE}/main")))
            .setPathsWhereToFindApplicationModules(List.of(Path.of("lib")))
            .setPathsWhereToFindMoreAssetsPerModule(Map.of("b", List.of(Path.of("src/b/test"))))
            .setEnablePreviewLanguageFeatures(true)
            .setCompileForVirtualMachineVersion(Runtime.version().feature())
            .setCharacterEncodingUsedBySourceFiles("UTF-8")
            .setOutputMessagesAboutWhatTheCompilerIsDoing(true)
            .setGenerateMetadataForMethodParameters(true)
            .setOutputSourceLocationsOfDeprecatedUsages(true)
            .setTerminateCompilationIfWarningsOccur(true)
            .setDestinationDirectory(Path.of("classes"));
    assertEquals("javac", javac.name());
    assertLinesMatch(
        List.of(
            "--module",
            "a,b,c",
            "--module-version",
            "123",
            "--module-source-path",
            String.join(File.separator, "src", "*", "main"),
            "--module-path",
            "lib",
            "--patch-module",
            "b=" + String.join(File.separator, "src", "b", "test"),
            "--release",
            String.valueOf(Runtime.version().feature()),
            "--enable-preview",
            "-parameters",
            "-deprecation",
            "-verbose",
            "-Werror",
            "-encoding",
            "UTF-8",
            "-d",
            "classes"),
        javac.arguments());
  }
}
