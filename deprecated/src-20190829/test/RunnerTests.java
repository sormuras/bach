import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.util.List;
import org.junit.jupiter.api.Test;

class RunnerTests {
  @Test
  void runNamedTool() {
    assertEquals(0, new Probe().bach.runner.run("noop"));
    assertEquals(1, new Probe().bach.runner.run("fail"));
    assertEquals(42, new Probe().bach.runner.run("*?!"));
  }

  @Test
  void runWithExpectedErrorCodeThrowsAssertionError() {
    var probe = new Probe();
    assertDoesNotThrow(() -> probe.bach.run(0, "noop"));
    var e = assertThrows(Error.class, () -> probe.bach.run(43, ""));
    assertEquals("Tool (<empty>) returned 42, but expected 43", e.getMessage());
  }

  @Test
  void runJavaVersion() throws Exception {
    var probe = new Probe();
    assertEquals(0, probe.bach.runner.run("java", "--version"));
    assertLinesMatch(
        List.of(
            ">> java(\"--version\")",
            "Starting new process.+",
            "Process .+ successfully terminated."),
        probe.lines());
    assertLinesMatch(List.of(), probe.errors());
    assertTrue(
        Files.readString(probe.redirected).contains(Runtime.version().toString()),
        probe.toString());
  }

  @Test
  void runJavacVersion() throws Exception {
    var probe = new Probe();
    assertEquals(0, probe.bach.runner.run("javac", "--version"));
    assertLinesMatch(
        List.of(">> javac(\"--version\")", "Running provided tool.+", "javac .+"), probe.lines());
    assertLinesMatch(List.of(), probe.errors());
  }
}
