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
    var bach = new Bach(log, true);
    assertDoesNotThrow(bach::hashCode);
    assertLinesMatch(List.of(), log.lines());
  }

  @Nested
  class Build {

    @Test
    void empty(@TempDir Path temp) {
      var log = new Log();
      var project = new Bach.Project.Builder("empty").paths(temp).build();
      var summary = new Bach(log, true).build(project);
      assertLinesMatch(
          List.of(
              "P Build empty", // verbose
              "P Project", // verbose
              ">> PROJECT COMPONENTS >>", // verbose
              "P Build project empty", // verbose
              "P `Files.createDirectories.+",
              "P Print version of various foundation tools", // verbose
              ">> FOUNDATION TOOL VERSIONS >>"),
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
}
