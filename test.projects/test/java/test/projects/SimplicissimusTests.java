package test.projects;

import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import com.github.sormuras.bach.Command;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SimplicissimusTests {

  @Test
  void build(@TempDir Path temp) throws Exception {
    var cli = new CLI("Simplicissimus", temp);
    var out =
        cli.start(
            Command.of("bach")
                .add("--verbose")
                .add("--strict")
                .add("--limit-tools", "javac,jar")
                .add("--jar-with-sources") // has no effect, yet
                .add("build"));
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
    var jar = cli.workspace("modules", "simplicissimus@0.jar");
    assertLinesMatch(
        """
        META-INF/
        META-INF/MANIFEST.MF
        module-info.class
        """
            .lines()
            .sorted(),
        CLI.run(Command.jar().add("--list").add("--file", jar)).lines().sorted());
  }
}
