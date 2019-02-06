import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LayoutTests {

  @Test
  void checkBootstrapProject() {
    var root = Path.of("demo", "00-bootstrap");
    assertEquals(Layout.BASIC, Layout.of(root.resolve("src/main/java")));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "greetings/src",
        "greetings-world/src",
        "greetings-world-with-main-and-test/src/main",
        "greetings-world-with-main-and-test/src/test"
      })
  void checkJigsawQuickStartContainsOnlyBasicLayout(Path path) {
    var root = Path.of("demo", "04-jigsaw-quick-start");
    assertEquals(Layout.BASIC, Layout.of(root.resolve(path)));
  }

  @Test
  void checkMavenProjects() {
    var root = Path.of("demo", "05-maven");
    assertEquals(Layout.MAVEN, Layout.of(root.resolve("maven-archetype-quickstart/src")));
  }
}
