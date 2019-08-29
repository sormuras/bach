package it;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.sormuras.bach.Bach;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ProjectTests {

  @ParameterizedTest
  @MethodSource("projects")
  void help(Path home) {
    var out = new StringWriter();
    var bach = new Bach(new PrintWriter(out), new PrintWriter(System.err), home, home);
    assertDoesNotThrow(bach::help, "Running bach.help() failed in: " + home);
    assertLinesMatch(
        List.of(
            "F1! F1! F1!",
            "Method API",
            ">> METHODS AND DECLARING CLASSES >>",
            "Provided tools",
            ">> NAMES OF TOOLS >>"),
        out.toString().lines().collect(Collectors.toList()));
  }

  @ParameterizedTest
  @MethodSource("projects")
  void info(Path home) {
    var out = new StringWriter();
    var bach = new Bach(new PrintWriter(out), new PrintWriter(System.err), home, home);
    assertDoesNotThrow(bach::info, "Running bach.info() failed in: " + home);
    assertLinesMatch(
        List.of("Bach (.+)", "  home='" + home + "' -> " + home.toUri(), "  work='" + home + "'"),
        out.toString().lines().collect(Collectors.toList()));
  }

  @ParameterizedTest
  @MethodSource("projects")
  void validate(Path home) {
    var out = new StringWriter();
    var bach = new Bach(new PrintWriter(out), new PrintWriter(System.err), home, home);
    assertDoesNotThrow(bach::validate, "Running bach.validate() failed in: " + home);
    assertTrue(out.toString().isBlank());
  }

  private static Stream<Path> projects() throws Exception {
    var root = Path.of("src/test-project");
    try (var entries = Files.list(root)) {
      return entries.filter(Files::isDirectory).sorted().collect(Collectors.toList()).stream();
    }
  }
}
