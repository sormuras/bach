// default package

import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.util.ArrayList;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BuildTests {

  @Nested
  class Execute {

    @ParameterizedTest(name = "verbose={0}")
    @ValueSource(booleans = {false, true})
    void singleTask(boolean verbose) {
      var task = Bach.Build.tool("javac", "--version");

      var log = new Log();
      var bach = new Bach(log, log, verbose);
      var project = new Bach.Project.Builder("demo").build();
      var summary = new Bach.Build.Summary(project);
      Bach.Build.execute(bach, task, summary);

      var expected = new ArrayList<String>();
      expected.add("L Initialized Bach.java .+");
      expected.add("L `javac --version`");
      if (verbose) expected.add("P `javac --version`");
      if (verbose) expected.add("P javac .+");
      assertLinesMatch(expected, log.lines());
    }
  }
}
