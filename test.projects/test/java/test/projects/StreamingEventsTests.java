package test.projects;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Logbook;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.Settings;
import com.github.sormuras.bach.api.Folders;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import test.projects.builder.ProjectBuilder;

class StreamingEventsTests {

  @Test
  void build() {
    var name = "StreamingEvents";
    var root = Path.of("test.projects", name);
    var folders = Folders.of(root);
    var options =
        Options.of()
            .with("--chroot", root.toString())
            .with("--verbose", "true")
            .with("--project-requires", "org.junit.platform.console")
            .with("--project-requires", "org.junit.platform.jfr")
            .with("--external-library-version", "JUnit=5.7.2")
            .with("--limit-tool", "javac", "jar", "test", "junit")
            .underlay(Options.ofDefaultValues());

    var logbook = Logbook.ofErrorPrinter();
    var settings = Settings.of(options, logbook).with(folders);
    var bach = new Bach(settings, new ProjectBuilder(settings).build());

    assertDoesNotThrow(bach::buildAndWriteLogbook, () -> bach.logbook().toString());

    assertLinesMatch(
        """
        >> BACH'S INITIALIZATION >>
        Work on project StreamingEvents 0
        >> INFO + BUILD >>
        Ran 2 tests in .+
        Build end.
        Bach run took .+
        Logbook written to .+
        """
            .lines(),
        bach.logbook().lines());
  }
}
