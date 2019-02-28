import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentest4j.TestAbortedException;

class DemoTests {
  private final CollectingLogger logger = new CollectingLogger("*");
  private final Bach bach = new Bach(logger, Path.of("."), List.of());

  @Nested
  @DisplayName("jigsaw-quick-start")
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
      assertEquals(
          "com.greetings/com.greetings.Main",
          Bach.ModuleInfo.findLaunch(base.resolve("src")).orElseThrow());
      var cleanTreeWalk = expected.resolve(demo.resolveSibling(name + ".clean.txt"));
      assertLinesMatch(Files.readAllLines(cleanTreeWalk), bach.utilities.treeWalk(base));
      if (bach.var.offline) {
        // TODO Better check for unresolvable external modules.
        assumeFalse(name.equals("greetings-world-with-main-and-test"));
      }
      assertEquals(0, bach.run(), logger.toString());
      var buildTreeWalk = expected.resolve(demo.resolveSibling(name + ".build.txt"));
      assertLinesMatch(Files.readAllLines(buildTreeWalk), bach.utilities.treeWalk(base));
      bach.run(Bach.Action.Default.ERASE);
      assertLinesMatch(Files.readAllLines(cleanTreeWalk), bach.utilities.treeWalk(base));
    }
  }

  @Test
  void scaffold(@TempDir Path workspace) throws Exception {
    var name = "scaffold";
    var demo = Path.of("demo", name);
    var base = workspace.resolve(demo.getFileName());
    bach.utilities.treeCopy(demo, base);

    var logger = new CollectingLogger(name);
    var bach = new Bach(logger, base, List.of("build"));
    var expected = Path.of("src", "test-resources");
    assertEquals(base, bach.base);
    assertEquals(name, bach.project.name);
    assertEquals("1.0.0-SNAPSHOT", bach.project.version);
    var cleanTreeWalk = expected.resolve(demo.resolveSibling(name + ".clean.txt"));
    // Files.write(cleanTreeWalk, bach.utilities.treeWalk(base));
    assertLinesMatch(Files.readAllLines(cleanTreeWalk), bach.utilities.treeWalk(base));
    if (bach.var.offline) {
      // TODO Better check for unresolvable external modules.
      throw new TestAbortedException("Online mode is required");
    }
    assertEquals(0, bach.run(), logger.toString());
    var buildTreeWalk = expected.resolve(demo.resolveSibling(name + ".build.txt"));
    // Files.write(buildTreeWalk, bach.utilities.treeWalk(base));
    assertLinesMatch(Files.readAllLines(buildTreeWalk), bach.utilities.treeWalk(base));
    bach.run(Bach.Action.Default.ERASE);
    assertLinesMatch(Files.readAllLines(cleanTreeWalk), bach.utilities.treeWalk(base));
  }
}
