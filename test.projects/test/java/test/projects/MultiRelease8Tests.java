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
    var out =
        cli.start(
            Command.of("bach")
                .add("--verbose")
                .add("--project-targets-java", "8")
                .add("--limit-tools", "javac,jar")
                .add("--jar-with-sources")
                .add("build"));
    assertLinesMatch(
        """
        >> BACH'S INITIALIZATION >>
        Perform main action: `build`
        Build MultiRelease-8 0
        >> INFO + BUILD >>
        Build took .+
        Logbook written to .+
        """
            .lines(),
        out.lines());
    var foo = cli.workspace("modules", "foo@0.jar");
    assertTrue(Files.exists(foo));
    assertLinesMatch(
        """
        META-INF/
        META-INF/MANIFEST.MF
        module-info.class
        foo/
        foo/Foo.class
        foo/Foo.java
        module-info.java
        META-INF/versions/9/
        META-INF/versions/9/foo/
        META-INF/versions/9/foo/Foo.class
        META-INF/versions/9/foo/Foo.java
        META-INF/versions/10/
        META-INF/versions/10/foo/
        META-INF/versions/10/foo/Foo.class
        META-INF/versions/10/foo/Foo.java
        """
            .lines()
            .sorted(),
        CLI.run(Command.jar().add("--list").add("--file", foo)).lines().sorted());
  }
}
