import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DemoTests {
  @Disabled("Work in progress...")
  @Test
  void build(@TempDir Path work) {
    var probe = new Probe(Path.of("demo"), work);
    assertEquals(0, probe.bach.main(List.of("build")), probe.toString());
    assertLinesMatch(
        List.of(
            ">> INIT >>",
            ">> build(<empty>)",
            ">> SYNC >>",
            "Synchronized 10 module uri(s).",
            ">> BUILD >>",
            "Build successful."),
        probe.lines());
  }
}
