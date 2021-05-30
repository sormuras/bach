package test.projects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Logbook;
import com.github.sormuras.bach.Options;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import test.base.SwallowSystem;

class ProcessingCodeTests {

  @Test
  @SwallowSystem
  void build(SwallowSystem.Streams streams) {
    var name = "ProcessingCode";
    var root = Path.of("test.projects", name);
    var bach =
        Bach.of(
            Logbook.ofErrorPrinter(),
            Options.of(
                call ->
                    call.with("--chroot", root)
                        .with("--verbose")
                        .with("--limit-tool", "javac")
                        .with("--limit-tool", "javadoc")
                        .with("--limit-tool", "jar")
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
                        .withAll("clean", "build")));

    assertEquals(0, bach.run(), bach.logbook().toString());

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
