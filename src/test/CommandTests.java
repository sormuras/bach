/*
 * Bach - Java Shell Builder
 * Copyright (C) 2019 Christian Stein
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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BachContext.class)
class CommandTests {

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
  void addStreamOfArguments() {
    var expectedLines = List.of("executable", "--stream", "  1+2+{}");
    var command = new Command("executable");
    command.add("--stream");
    command.add(Stream.of("1", 2, new BitSet(4)), "+");
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
  void addAllFiles() {
    var roots = List.of(Paths.get("src"));
    var command = new Command("files").mark(99);
    assertSame(command, command.addAll(roots, __ -> true));
    var actual = String.join("\n", dump(command));
    assertTrue(actual.contains("Bach.java"));
    assertTrue(actual.contains("CommandTests.java"));
    assertTrue(actual.contains("Bach.jsh"));
    assertTrue(actual.contains("Property.load.properties"));
    assertTrue(actual.contains("Util.isJavaFile.java"));
    assertTrue(actual.contains("UtilTests.module-info.java"));
  }

  @Test
  void addAllJavaFiles() {
    var roots = List.of(Paths.get("src/bach"), Paths.get("src/test"));
    var command = new Command("sources").mark(99);
    assertSame(command, command.addAllJavaFiles(roots));
    var actual = String.join("\n", dump(command));
    assertTrue(actual.contains("Bach.java"));
    assertTrue(actual.contains("CommandTests.java"));
    assertFalse(actual.contains("Bach.jsh"));
  }

  @Test
  void runJavaWithVersionSingleDash(BachContext context) {
    var version = Runtime.version().toString();
    assertEquals(0, context.bach.run("java", "-version"));
    assertTrue(context.bytesOut.toString().contains(version));
  }

  @Test
  void runJavaWithVersionDoubleDash(BachContext context) {
    var version = Runtime.version().toString();
    assertEquals(0, context.bach.run("java", "--version"));
    assertTrue(context.bytesOut.toString().contains(version));
  }

  @Test
  void runJavaWithOptionThatDoesNotExist(BachContext context) {
    assertEquals(1, context.bach.run("java", "--foo"));
    assertTrue(context.bytesOut.toString().contains("Unrecognized option: --foo"));
  }

  @Test
  void runJavaWithOptionThatDoesNotExist(Bach bach) {
    var error =
        assertThrows(AssertionError.class, () -> new Command("java").add("--foo").run(bach));
    assertEquals("expected an exit code of zero, but got: 1", error.getMessage());
  }

  @Test
  void runToolThatDoesNotExist(Bach bach) {
    var error = assertThrows(Error.class, () -> bach.run("tool, that doesn't exist", 1, 2, 3));
    assertEquals("executing `tool, that doesn't exist` failed", error.getMessage());
  }

  @Test
  void runJavaWithLongCommandLine(BachContext context) {
    var command = new Command("java");
    assertEquals("java", command.executable);
    command.setExecutableSupportsArgumentFile(true);
    command.add("--dry-run");
    command.add("--version");
    assertSame(command, command.addAll(Paths.get("src/bach"), __ -> true)); // use return value
    for (var i = 0; i <= 4000; i++) {
      command.add(String.format("arg-%04d", i));
    }
    command.run(context.bach);
    assertLinesMatch(List.of("--dry-run", "--version", ">> files and args >>"), command.arguments);
    assertTrue(context.bytesOut.toString().contains(Runtime.version().toString()));
    // fails when temporary directory does not exist
    command.setTemporaryDirectory(Paths.get("does-not-exist"));
    var e = assertThrows(UncheckedIOException.class, command::toProcessBuilder);
    assertEquals("creating temporary arguments file failed", e.getMessage());
    assertEquals(NoSuchFileException.class, e.getCause().getClass());
    assertTrue(e.getCause().getMessage().contains("does-not-exist"));
  }

  @Test
  void runCommandWithCustomExecutableToProgramOperator(BachContext context) {
    var command = new Command("executable");
    command.add("--version");
    command.setExecutableToProgramOperator(__ -> "java");
    command.run(context.bach);
    assertTrue(context.bytesOut.toString().contains(Runtime.version().toString()));
    assertLinesMatch(
        List.of(
            "running executable with 1 argument(s)",
            "executable\n--version",
            "replaced executable `executable` with program `java`"),
        context.recorder.all);
  }

  @Test
  void addAllUsingNonExistentFileAsRootFails(Bach bach) {
    var command = new Command("error");
    var path = Paths.get("error");
    var e = assertThrows(UncheckedIOException.class, () -> command.addAll(path, __ -> true));
    assertEquals("walking path `error` failed", e.getMessage());
  }

  @Test
  void toArgumentsArray() {
    var empty = new Command("builder").toArgumentsArray();
    assertEquals(0, empty.length);
    var three = new Command("builder").addAll("1", 2, '3').toArgumentsArray();
    assertArrayEquals(new String[] {"1", "2", "3"}, three);
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
  void customTool(BachContext context) {
    var dumped = new ArrayList<String>();
    var custom = new Command("custom tool");
    custom
        .addAll(List.of(1, 2, 3))
        .setExecutableSupportsArgumentFile(false)
        .setExecutableToProgramOperator(exe -> "/usr/bin/env " + exe)
        .setToolProvider(new CustomTool())
        .setToolProvider(new CustomTool()) // twice to hit all branches
        .setTemporaryDirectory(Paths.get("any"))
        .dump(dumped::add)
        .run(context.bach, UnaryOperator.identity(), custom::toProcessBuilder);
    assertLinesMatch(List.of("custom tool", "  1", "  2", "  3"), dumped);
    assertLinesMatch(
        List.of("running custom tool with 3 argument(s)", "custom tool\n  1\n  2\n  3"),
        context.recorder.all);
    // now "overflow" command line
    for (var i = 0; i <= 4000; i++) {
      custom.add(String.format("arg-%04d", i));
    }
    var e = assertThrows(IllegalStateException.class, custom::toProcessBuilder);
    assertEquals(
        "large command line (36039) detected, but custom tool does not support @argument file",
        e.getMessage());
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
}
