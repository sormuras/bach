import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ToolTests {

  private final CollectingLogger logger = new CollectingLogger("*");
  private final Bach bach = new Bach(logger, Path.of("."), List.of());

  @Nested
  class JUnit {
    @Test
    void help() {
      // var junit = new Bach.Tool.JUnit(List.of("--help"));
      var junit = new Bach.Action.Tool("junit", "--help");
      logger.clear();
      assertEquals(0, junit.run(bach));
      assertLinesMatch(
          List.of(
              "Running tool: junit --help",
              ">> INSTALL JUNIT >>",
              "Usage: ConsoleLauncher [-h] [--disable-ansi-colors] [--disable-banner]",
              ">> JUNIT HELP TEXT >>"),
          logger.getLines());
    }
  }

  class NoopTool implements Bach.Tool {
    final int code;

    NoopTool(int code) {
      this.code = code;
    }

    @Override
    public Bach.Command toCommand(Bach bach) {
      return new Bach.Command("noop");
    }

    @Override
    public int run(Bach bach) {
      return code;
    }
  }

  @Test
  void runNoopToolWithNonZeroCodeFails() {
    var noop = new NoopTool(123);
    assertEquals(123, noop.run(bach));
  }
}
