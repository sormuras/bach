// default package

import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BuildTests {

  @Nested
  class Execute {

    @Test
    void singleTask() {
      var log = new Log();
      var bach = new Bach(log, log, true);
      var task = Bach.Build.newToolTask("javac", "--version");
      Bach.Build.execute(bach, task, log);
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
