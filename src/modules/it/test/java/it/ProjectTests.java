package it;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.sormuras.bach.Bach;
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
    var bach = new Probe(home);
    assertDoesNotThrow(bach::help, "bach::help failed: " + home);
    assertLinesMatch(
        List.of(
            "F1! F1! F1!",
            "Method API",
            ">> METHODS AND DECLARING CLASSES >>",
            "Provided tools",
            ">> NAMES OF TOOLS >>"),
        bach.lines());
  }

  @ParameterizedTest
  @MethodSource("projects")
  void info(Path home) {
    var bach = new Probe(home);
    assertDoesNotThrow(bach::info, "bach::info failed: " + home);
    assertLinesMatch(
        List.of(
            "Bach \\(" + Bach.VERSION + ".*\\)",
            "  home='" + home + "' -> " + home.toUri(),
            "  work='" + home + "'"),
        bach.lines());
  }

  @ParameterizedTest
  @MethodSource("projects")
  void validate(Path home) {
    var bach = new Probe(home);
    assertDoesNotThrow(bach::validate, "bach::validate failed: " + home);
    assertTrue(bach.out.toString().isBlank());
  }

  private static Stream<Path> projects() throws Exception {
    var root = Path.of("src/test-project");
    try (var entries = Files.list(root)) {
      return entries.filter(Files::isDirectory).sorted().collect(Collectors.toList()).stream();
    }
  }
}
