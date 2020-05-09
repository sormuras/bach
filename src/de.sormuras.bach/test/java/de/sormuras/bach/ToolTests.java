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

package de.sormuras.bach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import de.sormuras.bach.tool.Jar;
import de.sormuras.bach.tool.Javac;
import de.sormuras.bach.tool.Javadoc;
import de.sormuras.bach.tool.Jlink;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ToolTests {

  @ParameterizedTest
  @ValueSource(classes = {Javac.class, Javadoc.class, Jar.class, Jlink.class})
  void defaultToolInstanceYieldsEmptyListOfArguments(Class<? extends Tool> tool) throws Exception {
    var instance = tool.getConstructor().newInstance();
    assertEquals(List.of(), List.of(instance.toolArguments()));
  }

  @Test
  void touchAllPropertiesOfJavac() {
    var javac =
        new Javac()
            .setCompileModulesCheckingTimestamps(Set.of("foo.bar", "foo.baz"))
            .setVersionOfModulesThatAreBeingCompiled(ModuleDescriptor.Version.parse("1.2.3"))
            .setPathsWhereToFindSourceFiles(Map.of("foo.bar", List.of(Path.of("foo-src"))))
            .setPatternsWhereToFindSourceFiles(List.of("src/*/main/java"))
            .setPathsWhereToFindApplicationModules(List.of(Path.of("lib")))
            .setPathsWhereToFindMoreAssetsPerModule(Map.of("foo.baz", List.of(Path.of("baz-src"))))
            .setCharacterEncodingUsedBySourceFiles("UTF-8")
            .setCompileForVirtualMachineVersion(Runtime.version().feature())
            .setEnablePreviewLanguageFeatures(true)
            .setGenerateMetadataForMethodParameters(true)
            .setOutputSourceLocationsOfDeprecatedUsages(true)
            .setOutputMessagesAboutWhatTheCompilerIsDoing(true)
            .setTerminateCompilationIfWarningsOccur(true)
            .setDestinationDirectory(Path.of("classes"));
    assertLinesMatch(
        List.of(
            "--module",
            "foo.bar,foo.baz",
            "--module-version",
            "1.2.3",
            "--module-source-path",
            "src/*/main/java",
            "--module-source-path",
            "foo.bar=foo-src",
            "--patch-module",
            "foo.baz=baz-src",
            "--module-path",
            "lib",
            "-encoding",
            "UTF-8",
            "--release",
            "" + Runtime.version().feature(),
            "--enable-preview",
            "-parameters",
            "-deprecation",
            "-verbose",
            "-Werror",
            "-d",
            "classes"),
        List.of(javac.toolArguments()));
  }
}
