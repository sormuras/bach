// default package

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
