package test.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Logbook;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.Factory;
import com.github.sormuras.bach.Printer;
import com.github.sormuras.bach.api.CodeSpaceMain;
import com.github.sormuras.bach.api.CodeSpaceTest;
import com.github.sormuras.bach.api.Folders;
import com.github.sormuras.bach.api.Option;
import com.github.sormuras.bach.api.Project;
import com.github.sormuras.bach.api.Spaces;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BachTests {

  @Test
  void explicit() {
    var logbook = Logbook.ofErrorPrinter();
    var factory = new Factory();
    var options = Options.ofDefaultValues();
    var folders = Folders.of("");
    var spaces = new Spaces(new CodeSpaceMain(), new CodeSpaceTest());
    var project = new Project("explicit", folders, spaces);
    var bach = new Bach(logbook, options, factory, project);

    assertEquals("explicit", bach.project().name());
  }

  @Test
  void chrooted(@TempDir Path temp) {
    var bach = Bach.of(Option.CHROOT.cli(), temp.toString());
    assertEquals(temp, bach.project().folders().root());
  }

  @Test
  void help() {
    var out =
        """
        Usage: bach [OPTIONS] [ACTIONS...]
        >> MESSAGE >>
        """;
    bach(0, out, Option.HELP);
  }

  @Test
  void helpExtra() {
    var out =
        """
        Usage: bach [OPTIONS] [ACTIONS...]
        >> MESSAGE >>
          --chroot.*
        >> MESSAGE >>
        """;
    bach(0, out, Option.HELP_EXTRA);
  }

  @Test
  void showConfiguration(@TempDir Path temp) {
    var out =
        """
        Bach .+
        >> BANNER >>
        Configuration
        --show-configuration
        >> CONFIGURATION >>
        Bach run took .+
        Logbook written to .+
        """;
    bach(0, out, Option.CHROOT, temp, Option.SHOW_CONFIGURATION);
  }

  @Test
  void listTools(@TempDir Path temp) {
    var out =
        """
        >> TOOLS >>
        """;
    bach(0, out, Option.CHROOT, temp, Option.LIST_TOOLS);
  }

  @Test
  void tool() {
    var out =
        """
        javac .+
        """;
    bach(0, out, Option.TOOL, "javac", "--version");
  }

  @Test
  void toolBach() {
    var out =
        """
        .+
        """;
    bach(0, out, Option.TOOL, "bach", "--version");
  }

  @Test
  void version() {
    bach(0, ".+", Option.VERSION);
  }

  private static void bach(int expectedStatus, String expectedOutput, Object... objects) {
    var out = new StringWriter();
    var args =
        Stream.of(objects)
            .map(
                object -> {
                  if (object instanceof Option option) return option.cli();
                  return object.toString();
                })
            .toArray(String[]::new);
    var bach = Bach.of(Printer.of(new PrintWriter(out)), args);
    var status = bach.run();

    assertEquals(expectedStatus, status, () -> bach.logbook().toString());
    assertLinesMatch(expectedOutput.lines(), out.toString().lines());
  }
}
