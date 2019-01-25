/*
 * Bach - Java Shell Builder
 * Copyright (C) 2018 Christian Stein
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

import java.io.File;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BachContext.class)
class CommandTests {

  @Test
  void addAllOptionsUsingInstanceOfObject(Bach bach) {
    var command = bach.command("executable");
    command.addAllOptions(new Object()).mark(0);
    assertEquals("executable", command.executable);
    assertTrue(command.arguments.isEmpty());
  }

  @Test
  void addSingleArguments(Bach bach) {
    var expectedLines =
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
    var command = bach.command("executable");
    command.add("--some-option");
    command.add("value");
    command.add("-single-flag-without-values");
    command.mark(5);
    List.of("0", "1", "2", "3", "4").forEach(command::add);
    List.of("5", "6", "7", "8", "9").forEach(command::add);
    assertLinesMatch(expectedLines, dump(command));
  }

  @Test
  void addPathsAsSingleOption(Bach bach) {
    var command = bach.command("paths");
    assertSame(command, command.add("-p"));
    assertSame(command, command.add(List.of(Paths.get("a"), Paths.get("b"))));
    var expected = List.of("paths", "-p", "  a" + File.pathSeparator + "b");
    var actual = dump(command);
    assertLinesMatch(expected, actual);
  }

  @Test
  void addAllJavaFiles(Bach bach) {
    var roots = List.of(Paths.get("src/bach"), Paths.get("src/test"));
    var command = bach.command("sources").mark(99);
    assertSame(command, command.addAllJavaFiles(roots));
    var actual = String.join("\n", dump(command));
    assertTrue(actual.contains("Bach.java"));
    assertTrue(actual.contains("CommandTests.java"));
    assertFalse(actual.contains("Bach.jsh"));
  }

  @Test
  @SuppressWarnings("unused")
  void addOptionsWithAnonymousClass(Bach bach) {
    Object options =
        new Object() {
          @Bach.Option("--ZETA")
          boolean z = true;

          Boolean flag1 = Boolean.TRUE;
          byte hex = 13;
          int value = 42;
          Boolean flag2 = Boolean.FALSE;

          transient String unused = "hidden";
          private Byte hidden = Byte.valueOf("123");
          // static Number ignored = Short.valueOf("456");

          List<String> collection = List.of("a", "b", "c");

          void hex(Bach.Command command) {
            command.add("--prime-as-hex");
            command.add("0x" + Integer.toHexString(hex));
          }
        };
    var command = bach.command("executable");
    command.addAllOptions(options, fields -> fields.sorted(Comparator.comparing(Field::getName)));
    command.add(Stream.of(1, 2, 3), "+");
    command.add("final");
    var array = command.toArgumentsArray();
    assertEquals(13, array.length);
    assertAll(
        "Options are reflected, ordered by name and added to the command instance",
        () -> assertEquals("-collection", array[0]),
        () -> assertEquals("[a, b, c]", array[1]),
        () -> assertEquals("-flag1", array[2]),
        () -> assertEquals("true", array[3]),
        () -> assertEquals("-flag2", array[4]),
        () -> assertEquals("false", array[5]),
        () -> assertEquals("--prime-as-hex", array[6]),
        () -> assertEquals("0xd", array[7]),
        () -> assertEquals("-value", array[8]),
        () -> assertEquals("42", array[9]),
        () -> assertEquals("--ZETA", array[10]),
        () -> assertEquals("1+2+3", array[11]),
        () -> assertEquals("final", array[12]));
  }

  @Test
  void runJavaWithVersionSingleDash(BachContext context) {
    var version = Runtime.version().toString();
    assertEquals(0, context.bach.run("java", "-version"));
    assertTrue(context.bytes.toString().contains(version));
  }

  @Test
  void runJavaWithVersionDoubleDash(BachContext context) {
    var version = Runtime.version().toString();
    assertEquals(0, context.bach.run("java", "--version"));
    assertTrue(context.bytes.toString().contains(version));
  }

  @Test
  void runJavaWithOptionThatDoesNotExist(BachContext context) {
    assertEquals(1, context.bach.run("java", "--foo"));
    assertTrue(context.bytes.toString().contains("Unrecognized option: --foo"));
  }

  @Test
  void runJavaWithOptionThatDoesNotExist(Bach bach) {
    var error = assertThrows(AssertionError.class, () -> bach.command("java", "--foo").run());
    assertEquals("expected an exit code of zero, but got: 1", error.getMessage());
  }

  @Test
  void runToolThatDoesNotExist(Bach bach) {
    var error = assertThrows(Error.class, () -> bach.run("tool, that doesn't exist", 1, 2, 3));
    assertEquals("executing `tool, that doesn't exist` failed", error.getMessage());
  }

  @Test
  void runJavaWithLongCommandLine(BachContext context) {
    var command = new JdkTool.Java().toCommand(context.bach, "--dry-run");
    command.add("--version");
    command.addAll(Paths.get("src/bach"), __ -> true);
    for (var i = 0; i <= 4000; i++) {
      command.add(String.format("arg-%04d", i));
    }
    command.run();
    assertLinesMatch(List.of("--dry-run", "--version", ">> files and args >>"), command.arguments);
    assertTrue(context.bytes.toString().contains(Runtime.version().toString()));
    // fails when temporary directory does not exist
    command.setTemporaryDirectory(Paths.get("does-not-exist"));
    var e = assertThrows(UncheckedIOException.class, command::toProcessBuilder);
    assertEquals("creating temporary arguments file failed", e.getMessage());
    assertEquals(NoSuchFileException.class, e.getCause().getClass());
    assertTrue(e.getCause().getMessage().contains("does-not-exist"));
  }

  @Test
  void runCommandWithCustomExecutableToProgramOperator(BachContext context) {
    var command = context.bach.command("executable");
    command.add("--version");
    command.setExecutableToProgramOperator(__ -> "java");
    command.run();
    assertTrue(context.bytes.toString().contains(Runtime.version().toString()));
    assertLinesMatch(
        List.of(
            "running executable with 1 argument(s)",
            "executable\n--version",
            "replaced executable `executable` with program `java`"),
        context.recorder.all);
  }

  @Test
  void addAllUsingNonExistentFileAsRootFails(Bach bach) {
    var command = bach.command("error");
    var path = Paths.get("error");
    var e = assertThrows(UncheckedIOException.class, () -> command.addAll(path, __ -> true));
    assertEquals("walking path `error` failed", e.getMessage());
  }

  @Test
  void addAllOptionsWithIllegalProperties(Bach bach) {
    var command = bach.command("error");
    var options = new ClassWithPrivateField();
    var fields = Arrays.stream(ClassWithPrivateField.class.getDeclaredFields());
    var error = assertThrows(Error.class, () -> command.addAllOptions(options, __ -> fields));
    assertTrue(error.getMessage().startsWith("reflecting option from field 'private int"));
    var cause = error.getCause();
    assertTrue(cause.getMessage().startsWith("class Bach$Command cannot access a member of class"));
    assertTrue(cause.getMessage().endsWith("with modifiers \"private\""));
  }

  @Test
  void addAllOptionsIgnoresStaticFields(Bach bach) {
    var command = bach.command("ignore-b");
    var options = new ClassWithStaticField();
    assertLinesMatch(List.of("ignore-b", "-a", "  0"), dump(command.addAllOptions(options)));
  }

  @Test
  void toProcessBuilder(Bach bach) {
    var builder = bach.command("builder").toProcessBuilder();
    assertEquals(1, builder.command().size());
    assertEquals("builder", builder.command().get(0));
  }

  @Test
  void markWithNegativeLimitFails(Bach bach) {
    var e = assertThrows(IllegalArgumentException.class, () -> bach.command("nine").mark(-9));
    assertEquals("limit must be greater then zero: -9", e.getMessage());
  }

  @Test
  void helperMethodsCheckArguments(Bach bach) {
    var command = bach.command("helper");
    assertEquals(List.of(), command.arguments);
    var helper = command.new Helper();
    assertEquals("Helper", helper.getClass().getSimpleName());
    helper.addModules(List.of());
    assertEquals(List.of(), command.arguments);
    assertThrows(AssertionError.class, () -> helper.patchModule(Map.of("abc", List.of())));
  }

  @Test
  void customTool(BachContext context) {
    var dumped = new ArrayList<String>();
    var custom = context.bach.command("custom tool");
    custom
        .addAll(List.of(1, 2, 3))
        .setExecutableSupportsArgumentFile(false)
        .setExecutableToProgramOperator(exe -> "/usr/bin/env " + exe)
        .setToolProvider(new CustomTool())
        .setToolProvider(new CustomTool()) // twice to hit all branches
        .setTemporaryDirectory(Paths.get("any"))
        .dump(dumped::add)
        .run(UnaryOperator.identity(), custom::toProcessBuilder);
    assertLinesMatch(List.of("custom tool", "  1", "  2", "  3"), dumped);
    assertLinesMatch(
        List.of("running custom tool with 3 argument(s)", "custom tool\n  1\n  2\n  3"),
        context.recorder.all);
    // now "overflow" command line
    for (var i = 0; i <= 4000; i++) {
      custom.add(String.format("arg-%04d", i));
    }
    custom.toProcessBuilder();
    var actual = String.join("\n", context.recorder.all);
    assertTrue(actual.contains("large command line (36039) detected"), actual);
    assertTrue(actual.contains("but custom tool does not support @argument file"));
  }

  private class CustomTool implements ToolProvider {

    @Override
    public String name() {
      return "custom tool";
    }

    @Override
    public int run(PrintWriter out, PrintWriter err, String... args) {
      out.println("CustomTool with " + List.of(args));
      return 0;
    }
  }

  private static class ClassWithPrivateField {

    private int x = 3;

    @Override
    public String toString() {
      return "ClassWithPrivateField [x=" + x + "]";
    }
  }

  private static class ClassWithStaticField {

    int a = 0;
    static int b = 0;

    @Override
    public String toString() {
      return "ClassWithStaticField [a=" + a + ", b=" + b + "]";
    }
  }

  private List<String> dump(Bach.Command command) {
    var lines = new ArrayList<String>();
    assertSame(command, command.dump(lines::add));
    return lines;
  }
}
