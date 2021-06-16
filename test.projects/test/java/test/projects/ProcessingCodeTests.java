package test.projects;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Logbook;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.Settings;
import com.github.sormuras.bach.ToolCall;
import com.github.sormuras.bach.api.Folders;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import test.base.SwallowSystem;
import test.projects.builder.ProjectBuilder;

class ProcessingCodeTests {

  @Test
  @SwallowSystem
  void build(SwallowSystem.Streams streams) {
    var name = "ProcessingCode";
    var root = Path.of("test.projects", name);
    var folders = Folders.of(root);
    var call =
        ToolCall.of("bach")
            .with("--chroot", root)
            .with("--verbose")
            .with("--limit-tool", "javac,javadoc,jar")
            .with(
                "--tweak",
                """
                    test
                    javac
                    --processor-module-path
                    """
                    + root.resolve(".bach/workspace/modules"))
            .with(
                "--tweak",
                """
                  test
                  javac
                  -Xplugin:showPlugin
                  """);
    var options =
        Options.ofCommandLineArguments(call.arguments()).underlay(Options.ofDefaultValues());

    var logbook = Logbook.ofErrorPrinter();
    var settings = Settings.of(options, logbook).with(folders);
    var bach = new Bach(settings, new ProjectBuilder(settings).build());

    assertDoesNotThrow(bach.settings().workflows().newCleanWorkflow().with(bach)::clean);
    assertDoesNotThrow(bach::buildAndWriteLogbook, () -> bach.logbook().toString());

    assertLinesMatch(
        """
        >> BACH'S INITIALIZATION >>
        Work on project ProcessingCode 0
        >> INFO + BUILD >>
        Bach run took .+
        Logbook written to .+
        """
            .lines(),
        bach.logbook().lines());

    assertLinesMatch(
        """
        #
        # ShowProcessor.process
        #
        | CLASS tests.Tests
          | CONSTRUCTOR Tests()
        | MODULE tests
          # DOC_COMMENT Defines the API of the tests module.
            # TEXT Defines the API of the tests module.
          | PACKAGE tests
            | CLASS tests.Tests
              | CONSTRUCTOR Tests()
        #
        # ShowProcessor.process
        #
        #
        # ShowPlugin.finished
        #
        | CLASS <anonymous >
        #
        # ShowPlugin.finished
        #
        | CLASS tests.Tests
          | CONSTRUCTOR Tests()
        """
            .lines(),
        streams.lines());
  }
}
