import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.opentest4j.TestAbortedException;

class ActionTests {

  private final CollectingLogger logger = new CollectingLogger("*");
  private final Bach bach = new Bach(logger, Path.of("."), List.of());

  @Test
  void help() {
    var out = System.out;
    var bytes = new ByteArrayOutputStream();
    try {
      System.setOut(new PrintStream(bytes));
      Bach.Action.Default.HELP.run(bach);
    } finally {
      System.setOut(out);
    }
    assertLinesMatch(
        List.of(
            "",
            " build     -> Build project in base directory.",
            " clean     -> Delete all generated assets - but keep caches intact.",
            " erase     -> Delete all generated assets - and also delete caches.",
            " help      -> Print this help screen on standard out... F1, F1, F1!",
            " launch    -> Start main program.",
            " scaffold  -> Create modular Java sample project in base directory.",
            " tool      -> Execute named tool consuming all remaining arguments.",
            ""),
        bytes.toString().lines().collect(Collectors.toList()));
  }

  @Test
  void scaffold(@TempDir Path temp) {
    assumeFalse(bach.var.offline);

    var logger = new CollectingLogger("*");
    var bach = new Bach(logger, temp, List.of());
    var code = Bach.Action.Default.SCAFFOLD.run(bach);
    assertEquals(0, code, logger.toString());
    var uri = "https://github.com/sormuras/bach/raw/" + Bach.VERSION + "/demo/scaffold.zip";
    assertLinesMatch(
        List.of(
            "Downloading " + uri + "...",
            "Transferring " + uri + "...",
            "Downloaded scaffold.zip successfully.",
            ">> 2 >>"),
        logger.getLines());
    var expected = bach.utilities.treeWalk(Path.of("demo", "scaffold"));
    var actual = bach.utilities.treeWalk(bach.base);
    actual.removeAll(expected);
    assertTrue(actual.size() <= 2, actual.toString());
  }

  @Test
  void tool() {
    assertThrows(UnsupportedOperationException.class, () -> Bach.Action.Default.TOOL.run(bach));
  }

  @ParameterizedTest
  @EnumSource(Bach.Action.Default.class)
  void applyToEmptyDirectory(Bach.Action action, @TempDir Path empty) {
    var logger = new CollectingLogger("empty-" + action);
    var bach = new Bach(logger, empty, List.of());
    if (action == Bach.Action.Default.TOOL) {
      assertThrows(UnsupportedOperationException.class, () -> bach.run(action));
      return;
    }
    if (bach.var.offline) {
      if (action == Bach.Action.Default.SCAFFOLD) {
        throw new TestAbortedException("Online mode required");
      }
    }
    var code = bach.run(action);
    assertEquals(0, code, logger.toString());
  }

  @Nested
  class ToolRunner {

    @Test
    void failsOnNonExistentTool() {
      var tool = new Bach.Action.ToolRunner("does not exist", "really");
      var code = bach.run(tool);
      var log = logger.toString();
      assertEquals(1, code, log);
      assertTrue(log.contains("does not exist"), log);
      assertTrue(log.contains("Running tool failed:"), log);
    }

    @Test
    void standardIO() {
      var out = new StringBuilder();
      var tool = new Bach.Action.ToolRunner("java", "--version");
      bach.var.out = out::append;
      var code = bach.run(tool);
      var log = logger.toString();
      assertEquals(0, code, log);
      assertTrue(out.toString().contains(Runtime.version().toString()), out.toString());
    }

    @Test
    void java() {
      var tool = new Bach.Action.ToolRunner("java", "--version");
      var code = bach.run(tool);
      var log = logger.toString();
      assertEquals(0, code, log);
      assertTrue(log.contains(Runtime.version().toString()), log);
    }

    @Test
    void javac() {
      var tool = new Bach.Action.ToolRunner("javac", "--version");
      var code = bach.run(tool);
      var log = logger.toString();
      assertEquals(0, code, log);
      assertTrue(log.contains("javac " + Runtime.version().feature()), log);
    }

    @Test
    void javadoc() {
      var tool = new Bach.Action.ToolRunner(new Bach.Command("javadoc").add("--version"));
      var code = bach.run(tool);
      var log = logger.toString();
      assertEquals(0, code, log);
      assertTrue(log.contains("javadoc " + Runtime.version().feature()), log);
    }
  }
}
