import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class CommandTests {

  @Test
  void addSingleArguments() {
    var expectedLines =
        List.of(
            "executable",
            "--some-option",
            "  value",
            "-single-flag-without-values",
            "  0",
            ">> 1..8 >>",
            "  9");
    var command = new Bach.Command("executable");
    command.add("--some-option");
    command.add("value");
    command.add("-single-flag-without-values");
    command.addAll(List.of("0", "1", "2", "3")).add(4);
    command.addAll("5", "6", "7", "8").add(9);
    assertLinesMatch(expectedLines, lines(command));
  }

  @Test
  void addStreamOfArguments() {
    var expectedLines = List.of("executable", "--stream", "  1+2+{}");
    var command = new Bach.Command("executable");
    command.add("--stream");
    command.add(Stream.of("1", 2, new BitSet(4)), "+");
    assertLinesMatch(expectedLines, lines(command));
  }

  @Test
  void addPathsAsSingleOption() {
    var command = new Bach.Command("paths");
    assertSame(command, command.add("-p"));
    assertSame(command, command.add(List.of(Path.of("a"), Path.of("b"))));
    var expected = List.of("paths", "-p", "  a" + File.pathSeparator + "b");
    var actual = lines(command);
    assertLinesMatch(expected, actual);
  }

  @Test
  void addAllFiles() {
    var roots = List.of(Path.of("src"), Path.of("does", "not", "exist"));
    var command = new Bach.Command("files");
    assertSame(command, command.addAll(roots, __ -> true));
    var actual = String.join("\n", lines(command));
    assertAll(
        "All files are listed: " + actual,
        () -> assertTrue(actual.contains("Bach.java")),
        () -> assertTrue(actual.contains("CommandTests.java")),
        () -> assertTrue(actual.contains("scaffold.build.txt")),
        () -> assertTrue(actual.contains("scaffold.clean.txt")));
  }

  @Test
  void addAllJavaFiles() {
    var roots = List.of(Path.of("src"));
    var command = new Bach.Command("sources");
    assertSame(command, command.addAllJavaFiles(roots));
    var actual = String.join("\n", lines(command));
    assertTrue(actual.contains("Bach.java"));
    assertTrue(actual.contains("CommandTests.java"));
    assertFalse(actual.contains("java.lang.System$LoggerFinder"));
  }

  @Test
  void isJavaFile() {
    assertFalse(Bach.Command.isJavaFile(Path.of("")));
    assertFalse(Bach.Command.isJavaFile(Path.of("a/b")));
    assertTrue(Bach.Command.isJavaFile(Path.of("src/test/CommandTests.java")));
    assertFalse(Bach.Command.isJavaFile(Path.of("src/test-resources/Util.isJavaFile.java")));
  }

  @Test
  void addAllUsingNonExistentFileAsRootFails() {
    var command = new Bach.Command("error");
    var path = Path.of("error");
    var e = assertThrows(Error.class, () -> command.addAll(path, __ -> true).add(0));
    assertEquals("walking path `error` failed", e.getMessage());
  }

  private static List<String> lines(Bach.Command command) {
    var lines = new ArrayList<String>();
    assertSame(command, lines(command, lines::add));
    return lines;
  }

  private static Bach.Command lines(Bach.Command command, Consumer<String> consumer) {
    var iterator = command.arguments.listIterator();
    consumer.accept(command.name);
    while (iterator.hasNext()) {
      var argument = iterator.next();
      var indent = argument.startsWith("-") ? "" : "  ";
      consumer.accept(indent + argument);
    }
    return command;
  }
}
