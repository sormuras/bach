package test.projects;

import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import com.github.sormuras.bach.Command;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProcessingCodeTests {

  @Test
  void build(@TempDir Path temp) throws Exception {
    var cli = new CLI("ProcessingCode", temp);
    var out =
        cli.start(
            Command.of("bach")
                .with("--verbose")
                .with("--strict")
                .with("--limit-tools", "javac,jar,javadoc")
                .with("clean") // force re-compilation
                .with("build"));
    assertLinesMatch(
        """
        >> BACH'S INITIALIZATION >>
        Perform main action: `build`
        Build ProcessingCode 0
        >> INFO + BUILD 1 >>
        # ShowProcessor.process
        >> INFO + BUILD 2 >>
        # ShowPlugin.finished
        >> INFO + BUILD 3 >>
        Build took .+
        Logbook written to .+
        """
            .lines(),
        out.lines());
  }
}
