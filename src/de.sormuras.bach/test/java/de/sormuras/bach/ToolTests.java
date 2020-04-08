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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ToolTests {

  @Nested
  class ToStrings {

    @Test
    void withZeroArgumentsReturnsListOfSizeOne() {
      var strings = Tool.toStrings("tool");
      assertEquals(1, strings.size(), strings.toString());
      assertEquals("tool", strings.get(0));
    }

    @Test
    void withOneArgumentReturnsListOfSizeOne() {
      var strings = Tool.toStrings("tool", "--version");
      assertEquals(1, strings.size(), strings.toString());
      assertEquals("tool --version", strings.get(0));
    }

    @Test
    void withTwoArgumentReturnsListOfSizeThree() {
      var strings = Tool.toStrings("tool", "--option", "value");
      assertEquals(3, strings.size(), strings.toString());
      assertLinesMatch(List.of("tool with 2 arguments:", "\t--option", "\t\tvalue"), strings);
    }

    @Test
    void withMoreThenTwoArgumentsReturnsListOfManyIndentedStrings() {
      var strings = Tool.toStrings("tool", "-a", "1", "--b", "2", "-c", "--d");
      assertEquals(7, strings.size(), strings.toString());
      assertLinesMatch(
          List.of("tool with 6 arguments:", "\t-a", "\t\t1", "\t--b", "\t\t2", "\t-c", "\t--d"),
          strings);
    }
  }

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

  @Nested
  class Any {

    @Test
    void empty() {
      var empty = Tool.of("any");
      assertTrue(empty.args().isEmpty());
      assertLinesMatch(List.of("any"), empty.toStrings());
    }

    @Test
    void touchAllAdders() {
      var tool =
          Tool.of("any", 0x0)
              .add(1)
              .add("key", "value")
              .add("alpha", "beta", "gamma")
              .add(true, "first")
              .add(true, "second", "more")
              .add(false, "suppressed")
              .forEach(List.of('a', 'b', 'c'), Tool.Any::add);
      assertEquals("any", tool.name());
      assertLinesMatch(
          List.of(
              "0", "1", "key", "value", "alpha", "beta", "gamma", "first", "second", "more", "a",
              "b", "c"),
          tool.args());
    }
  }
}
