// default package

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.nio.file.Path;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DocTests {

  @ParameterizedTest
  @ValueSource(
      strings = {"doc/project/jigsaw.quick.start", "doc/project/jigsaw.quick.start.with.tests"})
  void project(Path base, @TempDir Path temp) {
    var log = new Log();
    var bach = new Bach(log, log, true);
    var paths = new Bach.Project.Paths(base, temp.resolve("out"), temp.resolve("lib"));
    var summary = bach.build(paths, project -> project.version("0-ea"));
    assertDoesNotThrow(summary::assertSuccessful);
    // log.lines().forEach(System.out::println);
    // summary.toMarkdown().forEach(System.out::println);
  }
}
