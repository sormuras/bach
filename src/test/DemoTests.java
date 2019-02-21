import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DemoTests {
  private final CollectingLogger logger = new CollectingLogger("*");
  private final Bach bach = new Bach(logger, Path.of("."), List.of());

  @Nested
  class JigsawQuickStart {

    @ParameterizedTest
    @ValueSource(strings = {"greetings", "greetings-world", "greetings-world-with-main-and-test"})
    void greetings(String name, @TempDir Path workspace) throws Exception {
      var demo = Path.of("demo", "jigsaw-quick-start", name);
      var base = workspace.resolve(demo.getFileName());
      bach.utilities.treeCopy(demo, base);

      var logger = new CollectingLogger(name);
      var bach = new Bach(logger, base, List.of("build"));
      var expected = Path.of("src", "test-resources");
      assertEquals(base, bach.base);
      assertEquals(name, bach.project.name);
      assertEquals("1.0.0-SNAPSHOT", bach.project.version);
      var cleanTreeWalk = expected.resolve("DemoTests.JigsawQuickStart." + name + ".clean.txt");
      assertLinesMatch(Files.readAllLines(cleanTreeWalk), bach.utilities.treeWalk(base));
      if (bach.var.offline) {
        // TODO Better check for unresolvable external modules.
        assumeFalse(name.equals("greetings-world-with-main-and-test"));
      }
      assertEquals(0, bach.run(), logger.toString());
      assertLinesMatch(
          List.of(
              "Running action BANNER...",
              ">> BANNER >>",
              "Action BANNER succeeded.",
              "Running action CHECK...",
              ">> CHECK >>",
              "Action CHECK succeeded.",
              "Running action BUILD...",
              ">> BUILD >>",
              "Action BUILD succeeded."),
          logger.getLines());
      var buildTreeWalk = expected.resolve("DemoTests.JigsawQuickStart." + name + ".build.txt");
      assertLinesMatch(Files.readAllLines(buildTreeWalk), bach.utilities.treeWalk(base));
      bach.run(Bach.Action.Default.ERASE);
      assertLinesMatch(Files.readAllLines(cleanTreeWalk), bach.utilities.treeWalk(base));
    }
  }
}
