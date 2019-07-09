import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DemoTests {
  @Test
  void build(@TempDir Path work) {
    var probe = new Probe(Path.of("demo"), work);
    assertEquals(0, probe.bach.main(List.of("sync")));
    assertLinesMatch(
        List.of(">> INIT >>", ">> sync(<empty>)", ">> SYNC >>", "Synchronized 10 module uri(s)."),
        probe.lines());
  }
}
