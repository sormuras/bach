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
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.sormuras.bach.tool.JavaCompiler.CompileForJavaRelease;
import de.sormuras.bach.tool.JavaCompiler.CompileModulesCheckingTimestamps;
import de.sormuras.bach.tool.JavaCompiler.DestinationDirectory;
import de.sormuras.bach.tool.JavaCompiler.EnablePreviewLanguageFeatures;
import de.sormuras.bach.tool.JavaCompiler.ModuleSourcePathInModulePatternForm;
import de.sormuras.bach.tool.JavaCompiler.ModuleSourcePathInModuleSpecificForm;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

class JavaCompilerTests {

  @Test
  void canonical() {
    var modules = List.of("a", "b", "c");

    var javac =
        Tool.javac(
            List.of(
                new CompileModulesCheckingTimestamps(modules),
                // VersionOfModulesThatAreBeingCompiled(Version.parse("123"))
                // PathsWhereToFindSourceFilesForModules(List.of(Path.of("src/{MODULE}/main")))
                new ModuleSourcePathInModuleSpecificForm("a", List.of(Path.of("src/a/main/java"))),
                new ModuleSourcePathInModuleSpecificForm("b", List.of(Path.of("src/b/java"))),
                new ModuleSourcePathInModuleSpecificForm("c", List.of(Path.of("src/c"))),
                new ModuleSourcePathInModulePatternForm(List.of("src/*/{test,test-preview}/java", "more")),
                // PathsWhereToFindApplicationModules(List.of(Path.of("lib")))
                // PathsWhereToFindMoreAssetsPerModule(Map.of("b", List.of(Path.of("src/b/test"))))
                new EnablePreviewLanguageFeatures(),
                new CompileForJavaRelease(Runtime.version().feature()),
                // CharacterEncodingUsedBySourceFiles("UTF-8")
                // OutputMessagesAboutWhatTheCompilerIsDoing(true)
                // GenerateMetadataForMethodParameters(true)
                // OutputSourceLocationsOfDeprecatedUsages(true)
                // TerminateCompilationIfWarningsOccur(true)
                // new Tool.Option.KeyValueOption<Integer>("i", 3),
                new DestinationDirectory(Path.of("classes"))));

    assertThrows(NoSuchElementException.class, () -> javac.get(Option.class));
    assertEquals(modules, javac.get(CompileModulesCheckingTimestamps.class).modules());
    assertEquals("classes", javac.get(DestinationDirectory.class).value().toString());

    assertEquals("javac", javac.name());
    assertLinesMatch(
        List.of(
            "--module",
            "a,b,c",
            // "--module-version",
            // "123",
            "--module-source-path",
            "a=" + String.join(File.separator, "src", "a", "main", "java"),
            "--module-source-path",
            "b=" + String.join(File.separator, "src", "b", "java"),
            "--module-source-path",
            "c=" + String.join(File.separator, "src", "c"),
            "--module-source-path",
            "src/*/{test,test-preview}/java" + File.pathSeparator + "more",
            // "--module-path",
            // "lib",
            // "--patch-module",
            // "b=" + String.join(File.separator, "src", "b", "test"),
            "--enable-preview",
            "--release",
            "" + Runtime.version().feature(),
            // "-parameters",
            // "-deprecation",
            // "-verbose",
            // "-Werror",
            // "-encoding",
            // "UTF-8",
            "-d",
            "classes"),
        javac.toArgumentStrings());
  }
}
