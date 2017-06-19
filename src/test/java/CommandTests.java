/*
 * Bach - Java Shell Builder
 * Copyright (C) 2017 Christian Stein
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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class CommandTests {

  @Test
  void dump() {
    List<String> expectedLines =
        List.of(
            "executable",
            "--some-option",
            "  value",
            "-single-flag-without-values",
            "0",
            ">> 1..3 >>",
            "4",
            "... [omitted 4 arguments]",
            "9");
    Bach.Command command = new Bach.Command("executable");
    command.add("--some-option").add("value");
    command.add("-single-flag-without-values");
    command.mark(5);
    command.addAll("0", "1", "2", "3", "4");
    command.addAll("5", "6", "7", "8", "9");
    List<String> actualLines = new ArrayList<>();
    command.dump(actualLines::add);
    assertLinesMatch(expectedLines, actualLines);
  }

  @Test
  void addConditional() {
    Bach.Command command = new Bach.Command("executable").add(true, "true").add(false, "false");
    List<String> arguments = List.of(command.toArgumentsArray());
    assertEquals(1, arguments.size());
    assertEquals("true", arguments.get(0));
  }

  @Test
  void addFolders() {
    Bach.Command command =
        new Bach.Command("executable")
            .add(folder -> folder.location.path, Bach.Folder.AUXILIARY, Bach.Folder.DEPENDENCIES)
            .addAll(Stream.empty());
    List<String> arguments = List.of(command.toArgumentsArray());
    assertEquals(1, arguments.size());
    assertEquals(".bach" + File.pathSeparator + "dependencies", arguments.get(0));
  }

  @Test
  void addJavaSourceFiles() {
    Bach.Command command = new Bach.Command("executable");
    command.addAll(Paths.get("src"), Bach.Util::isJavaSourceFile);
    List<String> arguments = List.of(command.toArgumentsArray());
    assertTrue(arguments.size() > 4);
  }

  @Test
  void addOptionsWithEmptyClass() {
    Bach.Command command = new Bach.Command("executable");
    command.addOptions(new Object());
    assertTrue(command.arguments.isEmpty());
  }

  @Test
  @SuppressWarnings("unused")
  void addOptionsWithAnonymousClass() {
    Object options =
        new Object() {
          @Bach.Util.OptionName("--ZETA")
          boolean z = true;

          Boolean flag1 = Boolean.TRUE;
          byte hex = 13;
          int value = 42;
          Boolean flag2 = Boolean.FALSE;

          List<String> hex() {
            return List.of("--prime-as-hex", "0x" + Integer.toHexString(hex));
          }
        };
    Bach.Command command = new Bach.Command("executable");
    command.addOptions(options).add("final");
    assertAll(
        "Options are reflected, ordered by name and added to the command instance",
        () -> assertEquals("-flag1", command.arguments.get(0)),
        () -> assertEquals("true", command.arguments.get(1)),
        () -> assertEquals("-flag2", command.arguments.get(2)),
        () -> assertEquals("false", command.arguments.get(3)),
        () -> assertEquals("--prime-as-hex", command.arguments.get(4)),
        () -> assertEquals("0xd", command.arguments.get(5)),
        () -> assertEquals("-value", command.arguments.get(6)),
        () -> assertEquals("42", command.arguments.get(7)),
        () -> assertEquals("--ZETA", command.arguments.get(8)),
        () -> assertEquals("final", command.arguments.get(9)));
  }

  //  void toolJavacOptions() {
  //    List<String> strings = new ArrayList<>();
  //    Bach bach = new Bach();
  //    Bach.Tool.JavacOptions options = bach.tool.new JavacOptions();
  //    options.additionalArguments = List.of("-J-Da=b");
  //    options.deprecation = true;
  //    options.encoding = StandardCharsets.US_ASCII;
  //    options.failOnWarnings = true;
  //    options.parameters = true;
  //    options.verbose = true;
  //    assert options.encoding().size() == 2;
  //    assert options.modulePaths().size() == 2;
  //    Bach.Command command = bach.command("javac");
  //    command.addOptions(options);
  //    command.dumpToPrinter((format, args) -> strings.add(String.format('|' + format,
  // args).trim()));
  //    assert Objects.equals("|javac", strings.get(0));
  //    assert Objects.equals("|-J-Da=b", strings.get(1));
  //    assert Objects.equals("|-deprecation", strings.get(2));
  //    assert Objects.equals("|-encoding", strings.get(3));
  //    assert Objects.equals("|  US-ASCII", strings.get(4));
  //    assert Objects.equals("|-Werror", strings.get(5));
  //    assert Objects.equals("|--module-path", strings.get(6));
  //    assert Objects.equals("|  .bach" + File.separator + "dependencies", strings.get(7));
  //    assert Objects.equals("|-parameters", strings.get(8));
  //    assert Objects.equals("|-verbose", strings.get(9));
  //  }

  //  void toolJavadocOptions() {
  //    List<String> strings = new ArrayList<>();
  //    Bach bach = new Bach();
  //    Bach.Tool.JavadocOptions options = bach.tool.new JavadocOptions();
  //    options.quiet = true;
  //    Bach.Command command = bach.command("javadoc");
  //    command.addOptions(options);
  //    command.dumpToPrinter((format, args) -> strings.add(String.format('|' + format,
  // args).trim()));
  //    assert Objects.equals("|javadoc", strings.get(0));
  //    assert Objects.equals("|-quiet", strings.get(1));
  //  }

  //  void toolJarOptions() {
  //    List<String> strings = new ArrayList<>();
  //    Bach bach = new Bach();
  //    Bach.Tool.JarOptions options = bach.tool.new JarOptions();
  //    options.noCompress = true;
  //    options.verbose = true;
  //    Bach.Command command = bach.command("jar");
  //    command.addOptions(options);
  //    command.dumpToPrinter((format, args) -> strings.add(String.format('|' + format,
  // args).trim()));
  //    assert Objects.equals("|jar", strings.get(0));
  //    assert Objects.equals("|--no-compress", strings.get(1));
  //    assert Objects.equals("|--verbose", strings.get(2));
  //  }

}
