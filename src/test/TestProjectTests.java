import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TestProjectTests {

  private final Path projects = Path.of("src/test-project");

  @Nested
  class Empty {

    @Test
    void build(@TempDir Path work) {
      assertThrows(AssertionError.class, () -> new Probe(projects.resolve("empty"), work));
    }
  }

  @Nested
  class MissingModule {

    @Test
    void build(@TempDir Path work) {
      var probe = new Probe(projects.resolve("missing-module"), work);
      assertEquals(1, probe.bach.main(List.of("build")), probe.toString());
      assertLinesMatch(
          List.of(">> INIT >>", ">> build(<empty>)", ">> BUILD >>", "Bach::compile"),
          probe.lines());
      assertLinesMatch(
          List.of(
              "project.modules=[a]",
              "project.requires=[b]",
              "found in library=[]",
              "Missing module(s) are: b",
              "Compilation failed."),
          probe.errors());
    }
  }
}
