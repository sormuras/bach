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

  @Nested
  class GoogleJavaFormat {
    @Test
    void help() {
      var format = new Bach.Tool.GoogleJavaFormat(List.of("--help"));
      logger.clear();
      assertEquals(0, format.run(bach));
      assertLinesMatch(
          List.of(
              ">> INSTALL >>",
              "Running action Tool...",
              ">> INTRO >>",
              "Usage: google-java-format [options] file(s)",
              ">> HELP TEXT >>",
              "Action Tool succeeded."),
          logger.getLines());
    }
  }

  @Nested
  class Maven {

    @Test
    void version() {
      var mvn = new Bach.Tool.Maven(List.of("--version"));
      logger.clear();
      assertEquals(0, mvn.run(bach));
      assertLinesMatch(
          List.of(
              ">> INSTALL >>",
              "Running action Tool...",
              ">> INTRO >>",
              "\\QApache Maven 3.6.0 (97c98ec64a1fdfee7767ce5ffb20918da4f719f3; 2018-10-24T\\E.+",
              ">> HELP TEXT >>",
              "Action Tool succeeded."),
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
      assertEquals(1, Bach.Tool.super.run(bach));
      return code;
    }
  }

  @Test
  void runNoopToolWithNonZeroCodeFails() {
    var noop = new NoopTool(123);
    assertEquals(123, noop.run(bach));
  }
}
