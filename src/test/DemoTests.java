import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    void greetings(String name, @TempDir Path workspace) {
      var demo = Path.of("demo", "jigsaw-quick-start", name);
      var base = workspace.resolve(demo.getFileName());
      bach.run(new Bach.Action.TreeCopy(demo, base));

      var logger = new CollectingLogger(name);
      var bach = new Bach(logger, base, List.of("build"));
      assertEquals(base, bach.base);
      assertTrue(Files.isDirectory(bach.based("src")));
      assertEquals(name, bach.project.name);
      assertEquals("1.0.0-SNAPSHOT", bach.project.version);
      if (bach.var.offline) {
        // TODO Better check for unresolvable external modules.
        assumeFalse(name.equals("greetings-world-with-main-and-test"));
      }
      assertEquals(0, bach.run(), logger.toString());
      assertLinesMatch(
          List.of(
              "Running action Banner...",
              "Bach.java - " + Bach.VERSION,
              ">> BANNER >>",
              "Action Banner succeeded.",
              "Running action Check...",
              "Action Check succeeded.",
              "Running action Build...",
              ">> BUILD >>",
              "Action Build succeeded."),
          logger.getLines());
      // logger.getLines().forEach(System.out::println);
      // bach.run(new Bach.Action.TreeWalk(base, System.out::println));
    }
  }
}
