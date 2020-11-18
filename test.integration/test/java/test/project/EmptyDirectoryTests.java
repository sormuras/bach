package test.project;

import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EmptyDirectoryTests {

  @Test
  void build(@TempDir Path temp) throws Exception {
    var output = new Context(temp, temp, 1).build();

    assertLinesMatch(
        """
        Build project %s 0-ea
        ERROR java.lang.RuntimeException: No module found!
        >> STACK TRACE >>
        """
            .formatted(temp.getFileName())
            .lines(),
        output.lines());
  }
}
