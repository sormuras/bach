import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

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
        No init modules declared.
        Compile and package 3 main modules...
        >> JAVAC + JAR >>
        No test modules declared.
        """
            .lines(),
        printer.out().toString().lines());

    var checkedLines = printer.out().toString().lines().count();

    bach.run("launch", "first", '2', 0x3);
    assertLinesMatch(
        """
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
  @MethodSource
  @SwallowSystem
  void buildExampleProject(Path path, @TempDir Path temp) throws Exception {
    var bach = bach("--chroot", path.toString(), "--change-bach-out", temp.toString());
    assertDoesNotThrow(() -> bach.run("build"), bach.configuration().printer()::toString);
    var init = !bach.configuration().project().spaces().init().modules().isEmpty();
    if (OS.WINDOWS.isCurrentOs() && init) {
      System.gc(); // try to release file handles and...
      Thread.sleep(123); // hope JAR files are not locked...
    }
  }

  static List<Path> buildExampleProject() {
    return Bach.Core.PathSupport.list(Path.of("doc/example-projects"), Files::isDirectory);
  }
}
