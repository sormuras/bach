import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BachTests {
  @RepeatedTest(2)
  void repeated() throws Exception {
    Thread.sleep(1000);
  }

  @Test
  void test(@TempDir Path temp) {
    var bach = Bach.of(Bach.Printer.ofSilent(), "--chroot", temp.toString());
    assertEquals(temp, bach.paths().root());
    assertTrue(bach.printer().lines().isEmpty());
  }

  @RepeatedTest(3)
  void sleeping() throws Exception {
    Thread.sleep(1000);
  }
}
