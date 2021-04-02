package test.projects;

import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import com.github.sormuras.bach.Command;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SimpleTests {

  @Test
  void build(@TempDir Path temp) throws Exception {
    var cli = new CLI("Simple", temp);
    var out =
        cli.start(
            Command.of("bach")
                .with("--verbose")
                .with("--strict")
                .with("--limit-tools", "javac,jar")
                .with("--jar-with-sources") // has no effect, yet
                .with("build"));
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
    var jar = cli.workspace("modules", "simple@1.0.1.jar");
    assertLinesMatch(
        """
        META-INF/
        META-INF/MANIFEST.MF
        module-info.class
        simple/
        simple/Main.class
        simple/internal/
        simple/internal/Interface.class
        """
            .lines()
            .sorted(),
        CLI.run(Command.jar().with("--list").with("--file", jar)).lines().sorted());
  }
}
