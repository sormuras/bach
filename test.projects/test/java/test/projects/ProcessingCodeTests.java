package test.projects;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Core;
import com.github.sormuras.bach.Factory;
import com.github.sormuras.bach.Logbook;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.ToolCall;
import com.github.sormuras.bach.api.Folders;
import org.junit.jupiter.api.Test;
import test.base.SwallowSystem;
import test.projects.builder.ProjectBuilder;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

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
                  """)
            .withAll("clean", "build");
    var options =
        Options.ofCommandLineArguments(call.arguments()).underlay(Options.ofDefaultValues());

    var core =
        new Core(Logbook.ofErrorPrinter(), ModuleLayer.empty(), options, new Factory(), folders);
    var project = new ProjectBuilder(core).build();
    var bach = new Bach(core, project);

    assertEquals(0, bach.run(), () -> bach.logbook().toString());

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
