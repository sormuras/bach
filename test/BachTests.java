import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BachTests {
  @Test
  void test(@TempDir Path temp) {
    var bach = Bach.of(Bach.Component.Printer.ofSilent(), "--chroot", temp.toString());
    assertEquals(temp, bach.paths().root());
    assertTrue(bach.printer().lines().isEmpty());
  }
}
