package test.project;

import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Options;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EmptyDirectoryTests {

  @Test
  void build(@TempDir Path temp) throws Exception {
    var out = new StringWriter();
    var err = new StringWriter();
    var options =
        Options.of(
            new PrintWriter(out),
            new PrintWriter(err),
            "--verbose",
            "--base-directory",
            temp.toString(),
            "--project-name",
            "empty");
    try (var bach = new Bach(options)) {
      bach.build();
    }
    assertLinesMatch(
        """
        Build empty 0
        >> INFO + BUILD >>
        Logbook written to {{LOGBOOK}}
        """
            .replace("{{LOGBOOK}}", temp.resolve(".bach/workspace/logbook.md").toUri().toString())
            .lines(),
        out.toString().lines());

    assertTrue(err.getBuffer().isEmpty());
  }
}
