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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.spi.ToolProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JdkToolTests {

  @BeforeEach
  void clear() {
    Bach.log.level = Bach.Log.Level.OFF;
  }

  private List<String> dump(Bach.Command command) {
    List<String> lines = new ArrayList<>();
    command.dump(lines::add);
    return lines;
  }

  private String run(Bach.Command command) {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream(2000);
    PrintStream out = new PrintStream(bytes);
    command.setStandardStreams(out, out).run();
    return bytes.toString();
  }

  private String run(String executable, Object... arguments) {
    return run(new Bach.Command(executable).addAll(List.of(arguments)));
  }

  @Test
  void addSingleArguments() {
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
    Bach.Command command = new Bach.Command("executable");
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
    Bach.Command command = new Bach.Command("paths");
    assertSame(command, command.add("-p"));
    assertSame(command, command.add(List.of(Paths.get("a"), Paths.get("b"))));
    List<String> expected = List.of("paths", "-p", "  a" + File.pathSeparator + "b");
    List<String> actual = dump(command);
    assertLinesMatch(expected, actual);
  }

  @Test
  void addAllSourceFiles() {
    List<Path> roots = List.of(Paths.get("src/bach"), Paths.get("src/test"));
    Bach.Command command = new Bach.Command("sources").mark(99);
    assertSame(command, command.addAll(roots, Bach.Basics::isJavaFile));
    String actual = String.join("\n", dump(command));
    assertTrue(actual.contains("Bach.java"));
    assertFalse(actual.contains("Bach.jsh"));
    assertTrue(actual.contains("JdkToolTests.java"));
  }

  @Test
  void addAllOptionsUsingInstanceOfObject() {
    Bach.Command command = new Bach.Command("executable");
    command.addAllOptions(new Object()).mark(0);
    assertEquals("executable", command.executable);
    assertTrue(command.arguments.isEmpty());
  }

  @Test
  @SuppressWarnings("unused")
  void addOptionsWithAnonymousClass() {
    Object options =
        new Object() {
          @Bach.Command.Option("--ZETA")
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
    Bach.Command command = new Bach.Command("executable");
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
  void runJavaWithVersion() {
    assertTrue(run("java", "-version").contains(Runtime.version().toString()));
    assertTrue(run("java", "--version").contains(Runtime.version().toString()));
  }

  @Test
  void runJavaWithOptionThatDoesNotExist() {
    AssertionError error = assertThrows(AssertionError.class, () -> run("java", "--foo"));
    assertEquals("expected an exit code of zero, but got: 1", error.getMessage());
  }

  @Test
  void runToolThatDoesNotExist() {
    Error f = assertThrows(Error.class, () -> run("tool, that doesn't exist", 1, 2, 3));
    assertEquals("executing `tool, that doesn't exist` failed", f.getMessage());
  }

  @Test
  void runJavaWithLongCommandLine() {
    Bach.Command command = new Bach.JdkTool.Java().toCommand();
    command.add("-version");
    for (int i = 0; i <= 4000; i++) {
      command.add(String.format("arg-%04d", i));
    }
    assertTrue(run(command).contains(Runtime.version().toString()));
  }

  @Test
  void dumpJavacOptions() {
    List<String> expectedLines =
        List.of(
            "javac",
            "--class-path",
            "  classes",
            "-g",
            "-deprecation",
            "-d",
            "  out",
            "-encoding",
            "  US-ASCII",
            "-Werror",
            "--patch-module",
            "  foo=bar",
            "--module-path",
            "  mods",
            "--module-source-path",
            "  src",
            "-parameters",
            "-verbose",
            ">> many .java files >>");
    Bach.JdkTool.Javac javac = new Bach.JdkTool.Javac();
    javac.generateAllDebuggingInformation = true;
    javac.deprecation = true;
    javac.destinationPath = Paths.get("out");
    javac.encoding = StandardCharsets.US_ASCII;
    javac.failOnWarnings = true;
    javac.parameters = true;
    javac.verbose = true;
    javac.classPath = List.of(Paths.get("classes"));
    javac.classSourcePath = List.of(Paths.get("src/build"));
    javac.moduleSourcePath = List.of(Paths.get("src"));
    javac.modulePath = List.of(Paths.get("mods"));
    javac.patchModule = Map.of("foo", List.of(Paths.get("bar")));
    assertLinesMatch(expectedLines, dump(javac.toCommand()));
  }

  @Test
  void dumpJavaOptions() {
    List<String> expectedLines =
        List.of(
            "java",
            "--dry-run",
            "--patch-module",
            "  com.greetings=xxx",
            "--module-path",
            "  mods",
            "--module",
            "  com.greetings/com.greetings.Main");
    Bach.JdkTool.Java java = new Bach.JdkTool.Java();
    java.dryRun = true;
    java.patchModule = Map.of("com.greetings", List.of(Paths.get("xxx")));
    java.modulePath = List.of(Paths.get("mods"));
    java.module = "com.greetings/com.greetings.Main";
    assertLinesMatch(expectedLines, dump(java.toCommand()));
  }

  @Test
  void dumpJavadocOptions() {
    List<String> expectedLines = List.of("javadoc", "-quiet");
    Bach.JdkTool.Javadoc javadoc = new Bach.JdkTool.Javadoc();
    javadoc.quiet = true;
    assertLinesMatch(expectedLines, dump(javadoc.toCommand()));
  }

  @Test
  void dumpJarOptions() {
    List<String> expectedLines =
        List.of(
            "jar",
            "--list",
            "--file",
            "  fleet.jar",
            "--main-class",
            "  uss.Enterprise",
            "--module-version",
            "  1701",
            "--no-compress",
            "--verbose",
            "-C",
            "  classes",
            ".");
    Bach.JdkTool.Jar jar = new Bach.JdkTool.Jar();
    jar.mode = "--list";
    jar.file = Paths.get("fleet.jar");
    jar.mainClass = "uss.Enterprise";
    jar.moduleVersion = "1701";
    jar.noCompress = true;
    jar.verbose = true;
    jar.path = Paths.get("classes");
    Bach.Command command = jar.toCommand();
    command.mark(1);
    command.add(".");
    assertLinesMatch(expectedLines, dump(command));
  }

  @Test
  void dumpJdepsOptions() {
    Bach.JdkTool.Jdeps jdeps = new Bach.JdkTool.Jdeps();
    jdeps.classpath = List.of(Paths.get("classes"));
    jdeps.jdkInternals = true;
    jdeps.recursive = true;
    jdeps.profile = true;
    jdeps.apionly = true;
    jdeps.summary = true;
    jdeps.verbose = true;
    assertLinesMatch(
        List.of(
            "jdeps",
            "-classpath",
            "  classes",
            "-recursive",
            "--jdk-internals",
            "-profile",
            "-apionly",
            "-summary",
            "-verbose"),
        dump(jdeps.toCommand()));
  }

  @Test
  void dumpJlinkOptions() {
    Bach.JdkTool.Jlink jlink = new Bach.JdkTool.Jlink();
    jlink.modulePath = List.of(Paths.get("mods"));
    jlink.output = Paths.get("target", "image");
    assertLinesMatch(
        List.of(
            "jlink", "--module-path", "  mods", "--output", "  target" + File.separator + "image"),
        dump(jlink.toCommand()));
  }

  @Test
  void customTool() {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream(2000);
    PrintStream out = new PrintStream(bytes);
    new Bach.Command("custom tool")
        .addAll(List.of(1, 2, 3))
        .setStandardStreams(out, out)
        .setToolProvider(new CustomTool())
        .dump(out::println)
        .run();
    assertLinesMatch(
        List.of(">> dump >>", "CustomTool with [1, 2, 3]"),
        List.of(bytes.toString().split(System.lineSeparator())));
  }

  private class CustomTool implements ToolProvider {

    @Override
    public String name() {
      return "custom tool";
    }

    @Override
    public int run(PrintWriter out, PrintWriter err, String... args) {
      out.println("CustomTool with " + Arrays.toString(args));
      return 0;
    }
  }
}
