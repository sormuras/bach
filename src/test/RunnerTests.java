import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RunnerTests {
  @Test
  void runNamedTool() {
    assertEquals(42, new Probe().bach.runner.run(""));
  }

  @Test
  void runWithExpectedErrorCodeThrowsAssertionError() {
    var probe = new Probe();
    assertDoesNotThrow(() -> probe.bach.run(42, ""));
    var e = assertThrows(AssertionError.class, () -> probe.bach.run(43, ""));
    assertEquals("Tool (<empty>) returned 42, but expected 43", e.getMessage());
  }

  @Test
  void runJavacVersion() {
    assertEquals(0, new Probe().bach.runner.run("javac", "--version"));
  }
}
