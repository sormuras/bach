// default package

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
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
      var bach = new Bach(log, verbose);
      var project = new Bach.Project.Builder("demo").build();
      var summary = new Bach.Build.Summary(project);
      Bach.Build.execute(bach, task, summary);

      var expected = new ArrayList<String>();
      if (verbose) expected.add("P Bach.java initialized");
      if (verbose) expected.add("P `javac --version`");
      if (verbose) expected.add("P javac .+");
      assertLinesMatch(expected, log.lines());
    }
  }

  @Nested
  @DisabledIfSystemProperty(named = "offline", matches = "true")
  class Resolver {

    Log log = new Log();
    Bach bach = new Bach(log, true);

    @Test
    void resolveJUnit4(@TempDir Path temp) {
      try {
        var summary = bach.build(project -> project.paths(temp).requires("junit", "4.13"));
        var lib = temp.resolve("lib");
        summary.assertSuccessful();
        assertEquals(lib, summary.project().paths().lib());
        assertTrue(Files.exists(lib.resolve("junit-4.13.jar")));
      } catch (Throwable t) {
        log.lines().forEach(System.err::println);
        throw t;
      }
    }

    @Test
    void resolveJUnitJupiter(@TempDir Path temp) {
      try {
        var summary =
            bach.build(project -> project.paths(temp).requires("org.junit.jupiter", "5.6.0"));
        var lib = temp.resolve("lib");
        summary.assertSuccessful();
        assertEquals(lib, summary.project().paths().lib());
        assertTrue(Files.exists(lib.resolve("org.apiguardian.api-1.1.0.jar")));
        assertTrue(Files.exists(lib.resolve("org.junit.jupiter-5.6.0.jar")));
        assertTrue(Files.exists(lib.resolve("org.junit.jupiter.api-5.6.0.jar")));
        assertTrue(Files.exists(lib.resolve("org.junit.jupiter.engine-5.6.0.jar")));
        assertTrue(Files.exists(lib.resolve("org.junit.jupiter.params-5.6.0.jar")));
        assertTrue(Files.exists(lib.resolve("org.junit.platform.commons-1.6.0.jar")));
        assertTrue(Files.exists(lib.resolve("org.junit.platform.console-1.6.0.jar")));
        assertTrue(Files.exists(lib.resolve("org.junit.platform.engine-1.6.0.jar")));
        assertTrue(Files.exists(lib.resolve("org.junit.platform.launcher-1.6.0.jar")));
        assertTrue(Files.exists(lib.resolve("org.junit.platform.reporting-1.6.0.jar")));
        assertTrue(Files.exists(lib.resolve("org.opentest4j-1.2.0.jar")));
      } catch (Throwable t) {
        log.lines().forEach(System.err::println);
        throw t;
      }
    }
  }
}
