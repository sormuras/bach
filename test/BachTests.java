import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BachTests {
  @Test
  void test(@TempDir Path temp) {
    var out = new ArrayList<String>();
    var err = new ArrayList<String>();
    var bach = Bach.of(out::add, err::add, "--chroot", temp.toString());
    assertEquals(temp, bach.paths().root());
    assertTrue(out.isEmpty(), String.join("\n", out));
    assertTrue(err.isEmpty(), String.join("\n", err));
  }
}
