package test.projects;

import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SimpleTests {

  @Test
  void build(@TempDir Path temp) throws Exception {
    var out = new Context("Simple", temp).build("--strict", "--verbose");
    assertLinesMatch(
        """
        Options.+
        >> BACH'S INITIALIZATION >>
        // Perform main action: `build`
        Build simple 1.0.1
        >> INFO + BUILD >>
        Build took .+
        Logbook written to .+
        """
            .lines(),
        out.lines());
  }
}
