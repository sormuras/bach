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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class CommandTests {

  @Test
  void visitor() {
    var visitor = Command.visit(cmd -> cmd.addAllJavaFiles(List.of(Paths.get("."))));
    var command = new Command("test").add(visitor);
    assertEquals("test", command.executable);
    assertFalse(command.arguments.isEmpty());
    assertTrue(command.arguments.contains(Paths.get("./src/bach/Bach.java").toString()));
    assertTrue(command.arguments.contains(Paths.get("./src/bach/Command.java").toString()));
  }

  @Test
  void addAllOptionsUsingInstanceOfObject() {
    var command = new Command("executable");
    command.addAllOptions(new Object()).mark(0);
    assertEquals("executable", command.executable);
    assertTrue(command.arguments.isEmpty());
  }

  @Test
  void addSingleArguments() {
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
    var command = new Command("executable");
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
    var command = new Command("paths");
    assertSame(command, command.add("-p"));
    assertSame(command, command.add(List.of(Paths.get("a"), Paths.get("b"))));
    var expected = List.of("paths", "-p", "  a" + File.pathSeparator + "b");
    var actual = dump(command);
    assertLinesMatch(expected, actual);
  }

  @Test
  void addAllJavaFiles() {
    var roots = List.of(Paths.get("src/bach"), Paths.get("src/test"));
    var command = new Command("sources").mark(99);
    assertSame(command, command.addAllJavaFiles(roots));
    var actual = String.join("\n", dump(command));
    assertTrue(actual.contains("Bach.java"));
    assertTrue(actual.contains("Command.java"));
    assertTrue(actual.contains("CommandTests.java"));
    assertFalse(actual.contains("Bach.jsh"));
  }

  @Test
  @SuppressWarnings("unused")
  void addOptionsWithAnonymousClass() {
    Object options =
        new Object() {
          @Command.Option("--ZETA")
          boolean z = true;

          Boolean flag1 = Boolean.TRUE;
          byte hex = 13;
          int value = 42;
          Boolean flag2 = Boolean.FALSE;

          transient String unused = "hidden";
          private Byte hidden = Byte.valueOf("123");

          void hex(Command command) {
            command.add("--prime-as-hex");
            command.add("0x" + Integer.toHexString(hex));
          }
        };
    var command = new Command("executable");
    command.addAllOptions(options, fields -> fields.sorted(Comparator.comparing(Field::getName)));
    command.add(Stream.of(1, 2, 3), "+");
    command.add("final");
    var array = command.toArgumentsArray();
    assertEquals(11, array.length);
    assertAll(
        "Options are reflected, ordered by name and added to the command instance",
        () -> assertEquals("-flag1", array[0]),
        () -> assertEquals("true", array[1]),
        () -> assertEquals("-flag2", array[2]),
        () -> assertEquals("false", array[3]),
        () -> assertEquals("--prime-as-hex", array[4]),
        () -> assertEquals("0xd", array[5]),
        () -> assertEquals("-value", array[6]),
        () -> assertEquals("42", array[7]),
        () -> assertEquals("--ZETA", array[8]),
        () -> assertEquals("1+2+3", array[9]),
        () -> assertEquals("final", array[10]));
  }

  @Test
  void runJavaWithVersion() {
    assertTrue(run("java", "-version").contains(Runtime.version().toString()));
    assertTrue(run("java", "--version").contains(Runtime.version().toString()));
  }

  @Test
  void runJavaWithOptionThatDoesNotExist() {
    var error = assertThrows(AssertionError.class, () -> run("java", "--foo"));
    assertEquals("expected an exit code of zero, but got: 1", error.getMessage());
  }

  @Test
  void runToolThatDoesNotExist() {
    var error = assertThrows(Error.class, () -> run("tool, that doesn't exist", 1, 2, 3));
    assertEquals("executing `tool, that doesn't exist` failed", error.getMessage());
  }

  @Test
  void runJavaWithLongCommandLine() {
    var command = new JdkTool.Java().toCommand();
    command.add("--version");
    command.addAll(Paths.get("src/bach"), __ -> true);
    for (var i = 0; i <= 4000; i++) {
      command.add(String.format("arg-%04d", i));
    }
    var out = run(command);
    assertTrue(out.contains(Runtime.version().toString()));
  }

  @Test
  void toProcessBuilder() {
    var builder = new Command("builder").toProcessBuilder();
    assertEquals(1, builder.command().size());
    assertEquals("builder", builder.command().get(0));
  }

  @Test
  void markWithNegativeLimitFails() {
    var e = assertThrows(IllegalArgumentException.class, () -> new Command("nine").mark(-9));
    assertEquals("limit must be greater then zero: -9", e.getMessage());
  }

  @Test
  void helperMethodsCheckArguments() {
    var command = new Command("helper");
    assertEquals(List.of(), command.arguments);
    var helper = command.new Helper();
    assertEquals("Helper", helper.getClass().getSimpleName());
    helper.addModules(List.of());
    assertEquals(List.of(), command.arguments);
    assertThrows(AssertionError.class, () -> helper.patchModule(Map.of("abc", List.of())));
  }

  @Test
  void customTool() {
    var bytes = new ByteArrayOutputStream(2000);
    var out = new PrintStream(bytes);
    var custom = new Command("custom tool");
    var logger = new ArrayList<String>();
    custom
        .addAll(List.of(1, 2, 3))
        .setStandardStreams(out, out)
        .setExecutableSupportsArgumentFile(false)
        .setToolProvider(new CustomTool())
        .setLogger(logger::add)
        .dump(out::println)
        .run(UnaryOperator.identity(), custom::toProcessBuilder);
    assertLinesMatch(
        List.of(">> dump >>", "CustomTool with [1, 2, 3]"),
        List.of(bytes.toString().split(System.lineSeparator())));
    assertLinesMatch(
        List.of("running custom tool with 3 argument(s)", "custom tool\n  1\n  2\n  3"), logger);
    // now "overflow" command line
    for (var i = 0; i <= 4000; i++) {
      custom.add(String.format("arg-%04d", i));
    }
    bytes.reset();
    custom.toProcessBuilder();
    assertTrue(bytes.toString().startsWith("large command line (36026) detected"));
    assertTrue(bytes.toString().contains("but custom tool does not support @argument file"));
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

  private List<String> dump(Command command) {
    var lines = new ArrayList<String>();
    assertSame(command, command.dump(lines::add));
    return lines;
  }

  private String run(Command command) {
    return run(command, new ArrayList<String>()::add);
  }

  private String run(Command command, Consumer<String> logger) {
    var bytes = new ByteArrayOutputStream(2000);
    var out = new PrintStream(bytes);
    command.setStandardStreams(out, out).setLogger(logger).run();
    return bytes.toString();
  }

  private String run(String executable, Object... arguments) {
    return run(new Command(executable).addAll(List.of(arguments)));
  }
}
