package it;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.sormuras.bach.Bach;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BachApiTests {

  @Test
  void instantiateViaNoArgsFactory() {
    var bach = assertDoesNotThrow(Bach::of);
    assertTrue(bach.toString().contains("Bach"));
  }

  @Test
  void build(@TempDir Path temp) {
    var bach = new Probe(temp);
    assertDoesNotThrow(bach::build);
  }

  @Test
  void version(@TempDir Path temp) {
    var bach = new Probe(temp);
    assertDoesNotThrow(bach::version);
    assertTrue(String.join("\n", bach.lines()).contains(Bach.VERSION));
  }
}
