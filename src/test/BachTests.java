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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.lang.System.Logger.Level;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;

@ExtendWith(BachContext.class)
class BachTests {

  @Test
  void log(BachContext context) {
    var logger = context.bach.log.consumer;
    logger.accept(Level.ALL, "a");
    logger.accept(Level.TRACE, "t");
    context.bach.log.debug("{0}", "d"); // same as: logger.accept(Level.DEBUG, "d");
    context.bach.log.info("{0}", "i"); // same as: logger.accept(Level.INFO, "i");
    logger.accept(Level.WARNING, "w");
    logger.accept(Level.ERROR, "e");
    assertLinesMatch(List.of("a", "t", "d", "i", "w", "e"), context.recorder.all);
    assertLinesMatch(List.of("a", "t", "d", "i", "w", "e"), context.recorder.level(Level.ALL));
    assertLinesMatch(List.of("t", "d", "i", "w", "e"), context.recorder.level(Level.TRACE));
    assertLinesMatch(List.of("d", "i", "w", "e"), context.recorder.level(Level.DEBUG));
    assertLinesMatch(List.of("i", "w", "e"), context.recorder.level(Level.INFO));
    assertLinesMatch(List.of("w", "e"), context.recorder.level(Level.WARNING));
    assertLinesMatch(List.of("e"), context.recorder.level(Level.ERROR));
    assertLinesMatch(List.of(), context.recorder.level(Level.OFF));
  }

  @Test
  void debug(BachContext context) {
    assertTrue(context.bach.log.debug());
    context.bach.log.debug("{0}", "1");
    context.bach.vars.logLevel = Level.OFF;
    assertFalse(context.bach.log.debug());
    context.bach.log.debug("{0}", "2");
    context.bach.vars.logLevel = Level.INFO;
    assertFalse(context.bach.log.debug());
    context.bach.log.debug("{0}", "3");
    assertLinesMatch(List.of("1", "2", "3"), context.recorder.all);
    assertLinesMatch(List.of("1", "2", "3"), context.recorder.level(Level.DEBUG));
    assertLinesMatch(List.of(), context.recorder.level(Level.INFO));
  }

  @Test
  void capturingSystemOut() {
    var out = System.out;
    try {
      var bytes = new ByteArrayOutputStream(2000);
      System.setOut(new PrintStream(bytes));
      var bach = new Bach(); // pristine instance w/o print stream capturing context
      bach.log.debug("d");
      bach.log.info("i");
      var actual = List.of(bytes.toString().split("\\R"));
      assertLinesMatch(List.of("[DEBUG] d", "[INFO] i"), actual);
    } finally {
      System.setOut(out);
    }
  }

  @Test
  void creatingTemporaryDirectoriesFailsWhenFileAlreadyExist() throws Exception {
    var key = "bach.path.temporary";
    var file = Files.createTempFile("bach-", "-temporary.txt");
    try {
      assertNull(System.getProperty(key));
      System.setProperty(key, file.toString());
      assertEquals(file.toString(), System.getProperty(key));
      var exception = assertThrows(UncheckedIOException.class, Bach::new);
      assertTrue(exception.getCause() instanceof FileAlreadyExistsException);
    } finally {
      assertEquals(file.toString(), System.getProperty(key));
      System.getProperties().remove(key);
      assertNull(System.getProperty(key));
      Files.delete(file);
    }
  }

  @Test
  void runExecutable(BachContext context) {
    assertThrows(Error.class, () -> context.bach.run("executable", "a", "b", "3"));
    assertLinesMatch(
        List.of(
            "[run] executable [a, b, 3]",
            "running executable with 3 argument(s)",
            "executable\n" + "  a\n" + "  b\n" + "  3"),
        context.recorder.all);
  }

  @Test
  void runStreamSequentially(BachContext context) {
    var tasks = context.tasks(3);
    var result = context.bach.run("run stream sequentially", tasks);
    assertEquals(0, result);
    var expected =
        List.of(
            "[run] run stream sequentially...",
            "1 begin",
            "1 done. .+",
            "2 begin",
            "2 done. .+",
            "3 begin",
            "3 done. .+",
            "[run] run stream sequentially done.");
    assertLinesMatch(expected, context.recorder.all);
  }

  @Test
  @DisabledIfSystemProperty(named = "junit.jupiter.execution.parallel.enabled", matches = "true")
  void runStreamParallel(BachContext context) {
    var tasks = context.tasks(3).parallel();
    var result = context.bach.run("run stream in parallel", tasks);
    assertEquals(0, result);
    var expected =
        List.of(
            "[run] run stream in parallel...",
            ". begin",
            ". begin",
            ". begin",
            ". done. .+",
            ". done. .+",
            ". done. .+",
            "[run] run stream in parallel done.");
    assertLinesMatch(expected, context.recorder.all);
  }

  @Test
  @DisabledIfSystemProperty(named = "junit.jupiter.execution.parallel.enabled", matches = "true")
  void runVarArgs(BachContext context) {
    var result = context.bach.run("run varargs", () -> context.task("A"), () -> context.task("B"));
    assertEquals(0, result);
    var expected =
        List.of(
            "[run] run varargs...",
            ". begin",
            ". begin",
            ". done. .+",
            ". done. .+",
            "[run] run varargs done.");
    assertLinesMatch(expected, context.recorder.all);
  }

  @Test
  void runThrowsErrorOnNoneZeroResult(BachContext context) {
    Supplier<Integer> nine = () -> context.task("42", () -> 9);
    Executable executable = () -> context.bach.run("error", Stream.of(nine));
    var exception = assertThrows(Error.class, executable);
    assertEquals("error finished with exit code 9", exception.getMessage());
  }

  @Test
  void runApplyToCallHelp(BachContext context) {
    var bach = context.bach;
    var result = bach.apply("help");
    assertEquals(0, result);
    assertLinesMatch(
        List.of( //
            "java Bach.java <args>",
            "  help   - show this message and exit",
            "  build  - build project in current working directory",
            "  clean  - delete all generated directories and files",
            "  format - apply Google Java Format to all sources"),
        context.bytes.toString().lines().collect(Collectors.toList()));
  }

  @Test
  void runCommand(BachContext context) {
    var bach = context.bach;
    var command = bach.command("java").add("--version");
    var result = bach.run("java --version", command);
    assertEquals(0, result);
    assertTrue(context.bytes.toString().contains(Runtime.version().toString()));
    var expected =
        List.of(
            "[run] java --version...",
            "running java with 1 argument(s)",
            "java\n--version",
            "replaced executable `java` with program `" + bach.util.getJdkCommand("java") + "`",
            "[run] java --version done.");
    assertLinesMatch(expected, context.recorder.all);
  }

  @Test
  void runFunction(BachContext context) {
    var bach = context.bach;
    Function<Bach, Integer> function = bach.command("java", "--version");
    var result = bach.run(function);
    assertEquals(0, result);
    assertTrue(context.bytes.toString().contains(Runtime.version().toString()));
    var expected =
        List.of(
            "[run] " + function.getClass().getSimpleName() + "...",
            "running java with 1 argument(s)",
            "java\n--version",
            "replaced executable `java` with program `" + bach.util.getJdkCommand("java") + "`",
            "[run] " + function.getClass().getSimpleName() + " done.");
    assertLinesMatch(expected, context.recorder.all);
  }

  @Test
  void runFunctionWithCaption(BachContext context) {
    var bach = context.bach;
    Function<Bach, Integer> function = bach.command("java", "--version");
    var result = bach.run("Java Version Function", function);
    assertEquals(0, result);
    assertTrue(context.bytes.toString().contains(Runtime.version().toString()));
    var expected =
        List.of(
            "[run] Java Version Function...",
            "running java with 1 argument(s)",
            "java\n--version",
            "replaced executable `java` with program `" + bach.util.getJdkCommand("java") + "`",
            "[run] Java Version Function done.");
    assertLinesMatch(expected, context.recorder.all);
  }

  @Test
  void runFunctionOnDifferentBachInstanceFails(BachContext context) {
    var function = context.bach.command("java", "--version");
    var other = new Bach();
    other.vars.logLevel = Level.WARNING;
    assertThrows(Throwable.class, () -> other.run(function));
  }
}
