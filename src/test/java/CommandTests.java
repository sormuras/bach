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

  private List<String> dump(Bach.Command command) {
    List<String> lines = new ArrayList<>();
    command.dump(lines::add);
    return lines;
  }

  @Test
  void dump() {
    List<String> expectedLines =
        List.of(
            "executable",
            "--some-option",
            "  value",
            "-single-flag-without-values",
            "0",
            ">> 1..4 >>",
            "5",
            "... [omitted 3 arguments]",
            "9");
    Bach.Command command = bach.new Command("executable");
    command.add("--some-option");
    command.add("value");
    command.add("-single-flag-without-values");
    command.mark(5);
    List.of("0", "1", "2", "3", "4").forEach(command::add);
    List.of("5", "6", "7", "8", "9").forEach(command::add);
    assertLinesMatch(expectedLines, dump(command));
  }

  @Test
  void addPathsAsSingleOption() {
    Bach.Command command = bach.new Command("paths");
    List<String> expected = List.of("paths", "-p", "  a" + File.pathSeparator + "b");
    List<String> actual = dump(command.add("-p").add(List.of(Paths.get("a"), Paths.get("b"))));
    assertLinesMatch(expected, actual);
  }

  @Test
  void addAllSourceFiles() {
    Bach.Command command = bach.new Command("sources").mark(99);
    List<Path> roots = List.of(Paths.get("src/main"), Paths.get("src/test"));
    String actual = String.join("\n", dump(command.addAll(roots, Files::isRegularFile)));
    assertTrue(actual.contains("Bach.java"));
    assertTrue(actual.contains("BachTests.java"));
  }

  @Test
  void addOptionsWithEmptyClass() {
    Bach.Command command = bach.new Command("executable");
    command.addAllOptions(new Object()).mark(12);
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
            "|  out",
            "|-encoding",
            "|  US-ASCII",
            "|-Werror",
            "|-parameters",
            "|-verbose");
    Bach bach = new Bach();
    Bach.JavacOptions options = bach.new JavacOptions();
    options.deprecation = true;
    options.destinationPath = Paths.get("out");
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

  @Test
  void toolJavadocOptions() {
    List<String> expectedLines = List.of("|javadoc", "|-quiet");
    Bach bach = new Bach();
    Bach.JavadocOptions options = bach.new JavadocOptions();
    options.quiet = true;
    List<String> actualLines = new ArrayList<>();
    Bach.Command command = bach.new Command("javadoc");
    command.addAllOptions(options);
    command.dump(message -> actualLines.add('|' + message));
    assertLinesMatch(expectedLines, actualLines);
  }

  @Test
  void toolJarOptions() {
    List<String> expectedLines =
        List.of(
            "|jar",
            "|--list",
            "|--file",
            "|  fleet.jar",
            "|--main-class",
            "|  uss.Enterprise",
            "|--module-version",
            "|  1701",
            "|--no-compress",
            "|--verbose",
            "|-C",
            "|  classes",
            "|.");
    Bach bach = new Bach();
    Bach.JarOptions options = bach.new JarOptions();
    options.mode = "--list";
    options.file = Paths.get("fleet.jar");
    options.main = "uss.Enterprise";
    options.version = "1701";
    options.noCompress = true;
    options.verbose = true;
    options.path = Paths.get("classes");
    List<String> actualLines = new ArrayList<>();
    Bach.Command command = bach.new Command("jar");
    command.addAllOptions(options);
    command.mark(1);
    command.add(".");
    command.dump(message -> actualLines.add('|' + message));
    assertLinesMatch(expectedLines, actualLines);
  }

  @Test
  void toolJlinkOptions() {
    List<String> expectedLines =
        List.of(
            "|jlink",
            "|--module-path",
            "|  mods",
            "|--output",
            "|  target" + File.separator + "image");
    Bach bach = new Bach();
    Bach.JlinkOptions options = bach.new JlinkOptions();
    options.modulePaths = List.of(Paths.get("mods"));
    options.output = Paths.get("target", "image");
    List<String> actualLines = new ArrayList<>();
    Bach.Command command = bach.new Command("jlink");
    command.addAllOptions(options);
    command.dump(message -> actualLines.add('|' + message));
    assertLinesMatch(expectedLines, actualLines);
  }
}
