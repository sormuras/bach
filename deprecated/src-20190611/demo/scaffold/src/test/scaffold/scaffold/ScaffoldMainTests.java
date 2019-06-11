package scaffold;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ScaffoldMainTests {
  @Test
  void livingInModuleScaffold() {
    assertEquals("scaffold", ScaffoldMainTests.class.getModule().getName());
  }

  @Test
  void listFilesInTemporaryDirectory(@TempDir Path temp) throws Exception {
    assertEquals(0, ScaffoldMain.listFiles(temp).count());
    Files.writeString(temp.resolve("numbers.txt"), "123\n456\n789\n");
    assertEquals(1, ScaffoldMain.listFiles(temp).count());
  }
}
