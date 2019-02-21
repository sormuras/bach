import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ActionTests {

  private final CollectingLogger logger = new CollectingLogger("*");
  private final Bach bach = new Bach(logger, Path.of("."), List.of());

  @Nested
  class Help {
    @Test
    void checkHelpOutput() {
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
              " banner    -> ",
              " build     -> ",
              " check     -> ",
              " clean     -> ",
              " erase     -> ",
              " help      -> Print this help screen on standard out ... F1, F1, F1!",
              ""),
          bytes.toString().lines().collect(Collectors.toList()));
    }
  }

  @Nested
  class Tool {

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
