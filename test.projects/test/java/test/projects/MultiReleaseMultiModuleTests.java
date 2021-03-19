package test.projects;

import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.Command;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MultiReleaseMultiModuleTests {

  @Test
  void build(@TempDir Path temp) throws Exception {
    var cli = new CLI("MultiReleaseMultiModule", temp);
    var out = cli.start(Command.of("bach").add("--verbose").add("--jar-with-sources").add("build"));
    assertLinesMatch(
        """
        >> BACH'S INITIALIZATION >>
        Perform main action: `build`
        Build MultiReleaseMultiModule 0
        >> INFO + BUILD >>
        Build took .+
        Logbook written to .+
        """
            .lines(),
        out.lines());
    var api = cli.workspace("modules", "api@0.jar");
    assertTrue(Files.exists(api));
    assertLinesMatch(
        """
        META-INF/
        META-INF/MANIFEST.MF
        module-info.class
        module-info.java
        api/
        api/Api.class
        api/Api.java
        """
            .lines()
            .sorted(),
        CLI.run(Command.jar().add("--list").add("--file", api)).lines().sorted());

    var engine = cli.workspace("modules", "engine@0.jar");
    assertTrue(Files.exists(engine));
    assertLinesMatch(
        """
        META-INF/
        META-INF/MANIFEST.MF
        module-info.class
        module-info.java
        engine/
        engine/Main.class
        engine/Main.java
        engine/Overlay.class
        engine/Overlay.java
        engine/OverlaySingleton.class
        engine/OverlaySingleton.java
        META-INF/versions/11/
        META-INF/versions/11/engine/
        META-INF/versions/11/engine/OverlaySingleton.class
        META-INF/versions/11/engine/OverlaySingleton.java
        """
            .lines()
            .sorted(),
        CLI.run(Command.jar().add("--list").add("--file", engine)).lines().sorted());
  }
}
