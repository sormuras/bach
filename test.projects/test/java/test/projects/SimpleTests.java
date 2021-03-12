package test.projects;

import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SimpleTests {

  @Test
  void build(@TempDir Path temp) throws Exception {
    var cli = new CLI("Simple", temp);
    var out = cli.build("--strict", "--verbose");
    assertLinesMatch(
        """
        >> BACH'S INITIALIZATION >>
        Perform main action: `build`
        Build simple 1.0.1
        >> INFO + BUILD >>
        Build took .+
        Logbook written to .+
        """
            .lines(),
        out.lines());
    assertTrue(Files.exists(cli.workspace("modules", "simple@1.0.1.jar")));
  }
}
