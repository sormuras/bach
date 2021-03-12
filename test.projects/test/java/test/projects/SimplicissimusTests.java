package test.projects;

import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SimplicissimusTests {

  @Test
  void build(@TempDir Path temp) throws Exception {
    var cli = new CLI("Simplicissimus", temp);
    var out = cli.build("--strict", "--verbose");
    assertLinesMatch(
        """
        >> BACH'S INITIALIZATION >>
        Perform main action: `build`
        Build Simplicissimus 0
        >> INFO + BUILD >>
        Build took .+
        Logbook written to .+
        """
            .lines(),
        out.lines());
    assertTrue(Files.exists(cli.workspace("modules", "simplicissimus@0.jar")));
  }
}
