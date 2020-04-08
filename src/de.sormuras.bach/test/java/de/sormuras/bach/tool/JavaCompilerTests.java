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

package de.sormuras.bach.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.io.File;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JavaCompilerTests {

  @Test
  void javac() {
    var javac =
        new JavaCompiler()
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
            "javac with 21 arguments:",
            "\t--module",
            "\t\ta,b,c",
            "\t--module-version",
            "\t\t123",
            "\t--module-source-path",
            "\t\t" + String.join(File.separator, "src", "*", "main"),
            "\t--module-path",
            "\t\tlib",
            "\t--patch-module",
            "\t\tb=" + String.join(File.separator, "src", "b", "test"),
            "\t--release",
            "\t\t" + Runtime.version().feature(),
            "\t--enable-preview",
            "\t-parameters",
            "\t-deprecation",
            "\t-verbose",
            "\t-Werror",
            "\t-encoding",
            "\t\tUTF-8",
            "\t-d",
            "\t\tclasses"),
        javac.toStrings());
  }
}
