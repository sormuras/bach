package test.integration.locator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.sormuras.bach.locator.Maven;
import org.junit.jupiter.api.Test;

class MavenTests {
  @Test
  void central() {
    var expected = "https://repo.maven.apache.org/maven2/g/a/v/a-v.jar";
    assertEquals(expected, Maven.central("g", "a", "v"));
  }

  @Test
  void centralWithClassifier() {
    var expected = "https://repo.maven.apache.org/maven2/g/a/v/a-v-c.jar";
    assertEquals(expected, Maven.central("g", "a", "v", "c"));
  }

  @Test
  void joiner() {
    var expected = "r/g/a/v/a-v-c.t";
    var joiner = Maven.Joiner.of("g", "a", "v").repository("r").classifier("c").type("t");
    assertEquals(expected, joiner.toString());
  }
}
