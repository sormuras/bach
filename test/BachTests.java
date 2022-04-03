import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BachTests {
  @Test
  void test(@TempDir Path temp) {
    var bach = Bach.ofSilent("--chroot", temp.toString());
    assertEquals(temp, bach.paths().root());
    assertTrue(bach.getOut().toString().isEmpty());
    assertTrue(bach.getErr().toString().isEmpty());
  }

  @Test
  void versions(@TempDir Path temp) {
    var bach = Bach.ofSilent("--chroot", temp.toString());

    Stream.of(
            Bach.Tool.Call.of("jar").with("--version"),
            Bach.Tool.Call.of("javac").with("--version"),
            Bach.Tool.Call.of("javadoc").with("--version"))
        .parallel()
        .forEach(bach::run);
    bach.run(Bach.Tool.Call.of("banner", "---"));
    Stream.of(
            Bach.Tool.Call.of("jdeps").with("--version"),
            Bach.Tool.Call.of("jlink").with("--version"),
            Bach.Tool.Call.of("jmod").with("--version"),
            Bach.Tool.Call.of("jpackage").with("--version"))
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
        bach.getOut().toString().lines());
  }
}
