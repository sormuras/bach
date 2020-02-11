// default package

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BachTests {
  @Test
  void useCanonicalConstructorWithCustomLogger() {
    var log = new Log();
    var bach = new Bach(log, log, true);
    assertDoesNotThrow(bach::hashCode);
    assertLinesMatch(List.of("L Initialized Bach.java .+"), log.lines());
  }

  @Nested
  class Build {

    @Test
    void empty(@TempDir Path temp) {
      var log = new Log();
      var project = Bach.newProject("empty").paths(temp).build();
      var summary = new Bach(log, log, true).build(project);
      assertLinesMatch(
          List.of(
              "L Initialized Bach.java .+",
              "L Build Project.+",
              "L Build project empty",
              "P Build project empty", // verbose
              "L Create directories .+",
              "P `Files.createDirectories.+",
              "L Print version of various foundation tools",
              "P Print version of various foundation tools", // verbose
              ">> FOUNDATION TOOL VERSIONS >>",
              "P Summary written to " + temp.resolve(".bach/summary.md").toUri()),
          log.lines());
      assertLinesMatch(
          List.of(
              "# Summary",
              "",
              "## Project",
              ">> PROJECT >>",
              "## Task Execution Overview",
              ">> TASK TABLE >>",
              "## Task Execution Details",
              ">> TASK DETAILS >>",
              "## System Properties",
              ">> SYSTEM PROPERTIES LISTING >>"),
          summary.toMarkdown());
    }
  }

  @Nested
  class Execute {

    @Test
    void singleTask() {
      var log = new Log();
      var bach = new Bach(log, log, true);
      var task = Bach.Build.newToolTask("javac", "--version");
      bach.execute(task, log);
      assertLinesMatch(
          List.of(
              "L Initialized Bach.java .+",
              "L Run `javac` with 1 argument(s)",
              "P `javac --version`", // verbose
              "E BEGIN `javac --version`",
              "P javac .+",
              "E END `javac --version`"),
          log.lines());
    }
  }
}
