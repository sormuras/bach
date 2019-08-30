package it;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class DemoTests {
  @Test
  void build() {
    var bach = new Probe(Path.of("demo"));
    assertDoesNotThrow(bach::build, "bach::build failed: " + bach);
    assertLinesMatch(List.of(">> BUILD >>"), bach.lines());
  }

  @Test
  void validate() {
    var bach = new Probe(Path.of("demo"));
    assertDoesNotThrow(bach::validate, "bach::validate failed: " + bach);
    assertTrue(bach.out.toString().isBlank());
  }
}
