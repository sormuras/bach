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
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;

class CommandTests {

  private Bach bach = new Bach();

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
    Bach.Command command = bach.new Command("executable");
    command.add("--some-option");
    command.add("value");
    command.add("-single-flag-without-values");
    command.mark(5);
    List.of("0", "1", "2", "3", "4").forEach(command::add);
    List.of("5", "6", "7", "8", "9").forEach(command::add);
    assertLinesMatch(expectedLines, command.dump());
  }

  @Test
  void addPathsAsSingleOption() {
    Bach.Command command = bach.new Command("paths");
    List<String> expected = List.of("paths", "-p", "  a" + File.pathSeparator + "b");
    List<String> actual = command.add("-p").add(List.of(Paths.get("a"), Paths.get("b"))).dump();
    assertLinesMatch(expected, actual);
  }

  @Test
  void addAllSourceFiles() {
    Bach.Command command = bach.new Command("sources").mark(99);
    List<Path> roots = List.of(Paths.get("src/main"), Paths.get("src/test"));
    String actual = String.join("\n", command.addAll(roots, Files::isRegularFile).dump());
    assertTrue(actual.contains("Bach.java"));
    assertTrue(actual.contains("BachTests.java"));
  }

  @Test
  void addOptionsWithEmptyClass() {
    Bach.Command command = bach.new Command("executable");
    command.addAllOptions(new Object()).dump();
    assertTrue(command.arguments.isEmpty());
  }

  @Test
  @SuppressWarnings("unused")
  void addOptionsWithAnonymousClass() {
    Object options =
        new Object() {
          @Bach.CommandOption("--ZETA")
          boolean z = true;

          Boolean flag1 = Boolean.TRUE;
          byte hex = 13;
          int value = 42;
          Boolean flag2 = Boolean.FALSE;

          transient String unused = "hidden";
          private Byte hidden = Byte.valueOf("123");

          void hex(Bach.Command command) {
            command.add("--prime-as-hex");
            command.add("0x" + Integer.toHexString(hex));
          }
        };
    Bach.Command command = bach.new Command("executable");
    command.addAllOptions(options, fields -> fields.sorted(Comparator.comparing(Field::getName)));
    command.add("final");
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

  @Test
  void dumpJavacOptions() {
    List<String> expectedLines =
        List.of(
            "|javac",
            "|-deprecation",
            "|-d",
            "|  " + Paths.get("target", "bach"),
            "|-encoding",
            "|  US-ASCII",
            "|-Werror",
            "|-parameters",
            "|-verbose");
    Bach bach = new Bach();
    Bach.JavacOptions options = bach.new JavacOptions();
    options.deprecation = true;
    options.encoding = StandardCharsets.US_ASCII;
    options.failOnWarnings = true;
    options.parameters = true;
    options.verbose = true;
    List<String> actualLines = new ArrayList<>();
    Bach.Command command = bach.new Command("javac");
    command.addAllOptions(options);
    command.dump(message -> actualLines.add('|' + message));
    assertLinesMatch(expectedLines, actualLines);
  }

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
