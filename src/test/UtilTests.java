import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class UtilTests {

  @Test
  void newFails() {
    assertThrows(Error.class, Bach.Util::new);
  }

  @Test
  void join() {
    assertEquals("<empty>", Bach.Util.join());
    assertEquals("\"\"", Bach.Util.join(""));
    assertEquals("\"<null>\"", Bach.Util.join((Object) null));
    assertEquals("\"1\"", Bach.Util.join(1));
    assertEquals("\"1\", \"2\"", Bach.Util.join(1, 2));
    assertEquals("\"1\", \"2\", \"3\"", Bach.Util.join(1, 2, 3));
  }

  @Test
  void findJavaLauncher() {
    var javaHome = Path.of(System.getProperty("java.home"));
    assertTrue(Bach.Util.findExecutable(List.of(javaHome.resolve("bin")), "java").isPresent());
  }

  @Test
  void emptyPathsAreNotFiles() {
    assertFalse(Bach.Util.isJavaFile(Path.of("")));
    assertFalse(Bach.Util.isJarFile(Path.of("")));
    assertFalse(Bach.Util.isModuleInfo(Path.of("")));
  }
}
