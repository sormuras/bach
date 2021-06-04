package test.projects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Logbook;
import com.github.sormuras.bach.Options;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class StreamingEventsTests {

  @Test
  void build() {
    var name = "StreamingEvents";
    var root = Path.of("test.projects", name);
    var bach =
        Bach.of(
            Logbook.ofErrorPrinter(),
            Options.of()
                .with("--chroot", root.toString())
                .with("--verbose", "true")
                .with("--limit-tool", "javac", "jar", "test", "junit")
                .with("--workflow", "build"));

    assertEquals(0, bach.run(), bach.logbook().toString());

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
