package test.projects;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Core;
import com.github.sormuras.bach.Factory;
import com.github.sormuras.bach.Logbook;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.Settings;
import com.github.sormuras.bach.api.Folders;
import org.junit.jupiter.api.Test;
import test.projects.builder.ProjectBuilder;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

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

    var core = new Core(Logbook.ofErrorPrinter(), options, new Factory(), folders);
    var project = new ProjectBuilder(core).build();
    var settings = Settings.of();
    var bach = new Bach(core, settings, project);

    assertEquals(0, bach.buildAndWriteLogbook(), () -> bach.logbook().toString());

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
