import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class IntegrationTests {
  private static Bach bach(String... args) {
    var out = new Bach.Core.StringPrintWriter();
    var err = new Bach.Core.StringPrintWriter();
    return new Bach(Bach.Configuration.of(out, err, args));
  }

  @Test
  void buildDocTool(@TempDir Path temp) {
    var bach = bach("--verbose", "--chroot", "doc/tool", "--change-bach-out", temp.toString());
    var printer = bach.configuration().printer();
    var project = bach.configuration().project();
    var main = project.spaces().main();

    assertEquals("tool", project.name().value());
    assertEquals("123", project.version().value());
    assertEquals("2022-04-13T00:00Z", project.version().date().toString());
    assertEquals("org.example.app/org.example.app.Main", main.launcher().orElse("?"));

    bach.run("compile");
    assertLinesMatch(
        """
        compile
        Compile and package 3 main modules...
        >> JAVAC + JAR >>
        """
            .lines(),
        printer.out().toString().lines());

    var checkedLines = printer.out().toString().lines().count();

    bach.run("launch", "first", '2', 0x3);
    assertLinesMatch(
        """
        launch first 2 3
        Example application is running.
        file.+?/main/modules/org.example.app.jar
          module org.example.app
            package org.example.app
              class org.example.app.Main
         args -> (first 2 3)
        """
            .lines(),
        printer.out().toString().lines().skip(checkedLines));
  }

  @ParameterizedTest
  @ValueSource(strings = {"aggregator", "hello", "hello-world", "multi-release"})
  void buildExampleProject(String name, @TempDir Path temp) {
    var bach =
        bach("--chroot", "doc/example-projects/" + name, "--change-bach-out", temp.toString());
    bach.run("build");
  }
}
