package test.integration.workflow;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Configuration;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class WorkflowTests {
  private static Bach bach(Object... args) {
    return new Bach(Configuration.of(Stream.of(args).map(Object::toString).toArray(String[]::new)));
  }

  @Test
  void buildDocTool(@TempDir Path temp) {
    var bach = bach("--verbose", "--chroot", "doc/tool", "--change-bach-out", temp);
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
        >> INTRO >>
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
        >> INTRO >>
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
  void buildExampleProject(Path path, @TempDir Path temp) throws Exception {
    var bach = bach("--chroot", path, "--change-bach-out", temp);
    assertDoesNotThrow(() -> bach.run("build"), bach.configuration().printer()::toString);
    var init = !bach.configuration().project().spaces().init().modules().isEmpty();
    if (OS.WINDOWS.isCurrentOs() && init) {
      System.gc(); // try to release file handles and...
      Thread.sleep(123); // hope JAR files are not locked...
    }
  }

  static List<Path> buildExampleProject() {
    try (var stream =
        Files.newDirectoryStream(Path.of("doc/example-projects"), Files::isDirectory)) {
      return StreamSupport.stream(stream.spliterator(), false).toList();
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }
}
