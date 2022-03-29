import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BachTests {
  @Test
  void test(@TempDir Path temp) {
    var bach = Bach.of(__ -> {}, "--chroot", temp.toString());
    assertEquals(temp, bach.paths().root());
    assertTrue(bach.printer().lines().isEmpty());
  }

  @Test
  void versions(@TempDir Path temp) {
    var lines = new ArrayList<String>();
    var bach = Bach.of(lines::add, "--chroot", temp.toString());

    Stream.of(
            Bach.ToolCall.of("jar").with("--version"),
            Bach.ToolCall.of("javac").with("--version"),
            Bach.ToolCall.of("javadoc").with("--version"))
        .parallel()
        .forEach(bach::run);
    bach.banner("---");
    Stream.of(
            Bach.ToolCall.of("jdeps").with("--version"),
            Bach.ToolCall.of("jlink").with("--version"),
            Bach.ToolCall.of("jmod").with("--version"),
            Bach.ToolCall.of("jpackage").with("--version"))
        .sequential()
        .forEach(bach::run);

    assertLinesMatch(
        """
        >> 6 >>
        ===
        ---
        ===
        jdeps --version
          %1$s
        jlink --version
          %1$s
        jmod --version
          %1$s
        jpackage --version
          %1$s
        """
            .formatted(System.getProperty("java.version", "?"))
            .lines(),
        lines.stream().flatMap(String::lines));
  }
}
