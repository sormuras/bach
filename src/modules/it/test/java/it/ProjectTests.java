package it;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import de.sormuras.bach.Bach;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ProjectTests {

  @ParameterizedTest
  @MethodSource("projects")
  void help(Path project) {
    var out = new StringWriter();
    var bach = new Bach(new PrintWriter(out), new PrintWriter(System.err));
    assertDoesNotThrow(bach::help, "Running bach.help() failed in: " + project);
  }

  private static Stream<Path> projects() throws Exception {
    var root = Path.of("src/test-project");
    try (var entries = Files.list(root)) {
      return entries.filter(Files::isDirectory).sorted().collect(Collectors.toList()).stream();
    }
  }
}
