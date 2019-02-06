import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LayoutTests {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "module a {",
        "open module a{",
        "what ever \r\n prefix module    a   \n{",
        "/**\n * Comment with module literal.\n */ module a {"
      })
  void readModuleNameFromStringYieldsA(String source) {
    assertModuleNameIs("a", source);
  }

  @Test
  void readModuleNameReturnsWrongNameWithContrivedComment() {
    var src = "/**\n * Some module literal {@code followed} by a curly bracket.\n */ module a {";
    assertModuleNameIs("literal", src);
  }

  @Test
  void readModuleNameFailsForNonModuleDescriptorSourceUnit() {
    String source = "enum E {}";
    Exception e = assertThrows(Exception.class, () -> assertModuleNameIs("b", source));
    assertEquals(IllegalArgumentException.class, e.getClass());
    assertEquals("expected java module descriptor unit, but got: \n" + source, e.getMessage());
  }

  private void assertModuleNameIs(String expected, String source) {
    assertEquals(expected, Layout.readModuleName(source));
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
