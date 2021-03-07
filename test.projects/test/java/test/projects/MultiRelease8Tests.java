package test.projects;

import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.Command;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MultiRelease8Tests {

  @Test
  void build(@TempDir Path temp) throws Exception {
    var cli = new CLI("MultiRelease-8", temp);
    var out = cli.build("--verbose", "--project-targets-java", "8");
    var foo = cli.workspace("modules", "foo@0.jar");
    assertTrue(Files.exists(foo));
    assertLinesMatch(
        """
        >> BACH'S INITIALIZATION >>
        // Perform main action: `build`
        Build MultiRelease-8 0
        >> INFO + BUILD >>
        Build took .+
        Logbook written to .+
        """
            .lines(),
        out.lines());
    assertLinesMatch(
        """
        META-INF/
        META-INF/MANIFEST.MF
        module-info.class
        foo/
        foo/Foo.class
        META-INF/versions/9/
        META-INF/versions/9/foo/
        META-INF/versions/9/foo/Foo.class
        META-INF/versions/10/
        META-INF/versions/10/foo/
        META-INF/versions/10/foo/Foo.class
        """
            .lines(),
        CLI.run(Command.jar().add("--list").add("--file", foo)).lines());
  }
}
