package test.projects;

import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import com.github.sormuras.bach.Command;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JigsawQuickStartGreetingsTests {

  @Test
  void build(@TempDir Path temp) throws Exception {
    var cli = new CLI("JigsawQuickStartGreetings", temp);
    var out =
        cli.start(
            Command.of("bach")
                .with("--verbose")
                .with("--strict")
                .with("--limit-tools", "javac,jar")
                .with("--jar-with-sources")
                .with("build"));
    assertLinesMatch(
        """
        >> BACH'S INITIALIZATION >>
        Perform main action: `build`
        Build JigsawQuickStartGreetings 0
        >> INFO + BUILD >>
        Build took .+
        Logbook written to .+
        """
            .lines(),
        out.lines());
    var greetings = cli.workspace("modules", "com.greetings@0.jar");
    assertLinesMatch(
        """
        META-INF/
        META-INF/MANIFEST.MF
        module-info.class
        module-info.java
        com/
        com/greetings/
        com/greetings/Main.class
        com/greetings/Main.java
        """
            .lines()
            .sorted(),
        CLI.run(Command.jar().with("--list").with("--file", greetings)).lines().sorted());
  }
}
