import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DemoTests {
  private final CollectingLogger logger = new CollectingLogger("*");
  private final Bach bach = new Bach(logger, Path.of("."), List.of());

  @Nested
  class JigsawQuickStart {

    @Test
    void greetings(@TempDir Path workspace) {
      var demo = Path.of("demo", "jigsaw-quick-start", "greetings");
      var base = workspace.resolve(demo.getFileName());
      bach.run(new Bach.Action.TreeCopy(demo, base));

      var logger = new CollectingLogger("*");
      var bach = new Bach(logger, base, List.of());
      bach.project.dormant = false;
      assertEquals(base, bach.base);
      assertTrue(Files.isDirectory(bach.based("src")));
      assertEquals("greetings", bach.project.name);
      assertEquals("1.0.0-SNAPSHOT", bach.project.version);
      assertEquals(0, bach.run());
      assertLinesMatch(
          List.of(
              "Running action Banner...",
              "Bach.java - " + Bach.VERSION,
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
