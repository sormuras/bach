package test.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Printer;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BachTests {

  @Test
  void empty() {
    assertEquals("empty", Auxiliary.newEmptyBach().project().name());
  }

  @Test
  void chrooted(@TempDir Path temp) {
    var bach = Bach.of("--chroot", temp.toString());
    assertEquals(temp, bach.project().folders().root());
  }

  @Test
  void help(@TempDir Path temp) {
    var out = """
        Usage: bach [OPTIONS] [ACTIONS...]
        >> MESSAGE >>
        """;
    bach(0, out, "--chroot", temp, "--help");
  }

  @Test
  void helpExtra(@TempDir Path temp) {
    var out =
        """
        Usage: bach [OPTIONS] [ACTIONS...]
        >> MESSAGE >>
          --chroot.*
        >> MESSAGE >>
        """;
    bach(0, out, "--chroot", temp, "--help-extra");
  }

  @Test
  void listTools(@TempDir Path temp) {
    var out = """
        >> TOOLS >>
        """;
    bach(0, out, "--chroot", temp, "--list-tools");
  }

  @Test
  void tool(@TempDir Path temp) {
    var out = """
        javac .+
        """;
    bach(0, out, "--chroot", temp, "--tool", "javac", "--version");
  }

  @Test
  void toolBach(@TempDir Path temp) {
    var out = """
        .+
        """;
    bach(0, out, "--chroot", temp, "--tool", "bach", "--chroot", temp, "--version");
  }

  @Test
  void version(@TempDir Path temp) {
    bach(0, ".+", "--chroot", temp, "--version");
  }

  private static void bach(int expectedStatus, String expectedOutput, Object... objects) {
    var out = new StringWriter();
    var args = Stream.of(objects).map(Object::toString).toArray(String[]::new);
    var bach = Bach.of(Printer.of(new PrintWriter(out)), args);
    var status = bach.run();

    assertEquals(expectedStatus, status, () -> bach.logbook().toString());
    assertLinesMatch(expectedOutput.lines(), out.toString().lines());
  }
}
