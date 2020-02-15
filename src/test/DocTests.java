// default package

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DocTests {

  private final Log log = new Log();
  private final Bach bach = new Bach(log, log, true);

  @Test
  void buildProjectJigsawQuickStart(@TempDir Path temp) {
    var name = "jigsaw.quick.start";
    var base = Path.of("doc/project", name);
    var summary = build(base, temp);
    var project = summary.project();
    assertEquals(name, project.descriptor().name());
    var units = project.units();
    assertEquals(1, units.size());
    var greetings = units.get(0);
    try {
      assertEquals("com.greetings", greetings.name());
      assertEquals(base.resolve("com.greetings/module-info.java"), greetings.path());
      assertEquals(1, greetings.sources().size());
      assertEquals(0, greetings.resources().size());
      assertTrue(greetings.isMainClassPresent());
      assertEquals("com.greetings.Main", greetings.descriptor().mainClass().orElseThrow());
      assertFalse(greetings.isMultiRelease());
    } catch (AssertionError error) {
      greetings.print(System.err::println);
      throw error;
    }
  }

  @Test
  void buildProjectJigsawQuickStartWithTests(@TempDir Path temp) {
    var name = "jigsaw.quick.start.with.tests";
    var base = Path.of("doc/project", name);
    var summary = build(base, temp);
    var project = summary.project();
    assertEquals(name, project.descriptor().name());
    var units = project.units();
    assertEquals(3, units.size());
  }

  private Bach.Build.Summary build(Path base, Path temp) {
    var paths = new Bach.Project.Paths(base, temp.resolve("out"), temp.resolve("lib"));
    var summary = bach.build(paths, project -> project.version("0-ea"));
    assertDoesNotThrow(summary::assertSuccessful);
    // log.lines().forEach(System.out::println);
    // summary.toMarkdown().forEach(System.out::println);
    return summary;
  }
}
