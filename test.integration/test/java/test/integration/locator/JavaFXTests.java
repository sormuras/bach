package test.integration.locator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.api.ExternalModuleLocator;
import com.github.sormuras.bach.locator.JavaFX;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JavaFXTests {
  @Test
  void defaults() {
    var locator = JavaFX.of("0");

    assertSame(ExternalModuleLocator.Stability.STABLE, locator.stability());
    assertTrue(locator.title().startsWith("javafx.[*] -> JavaFX 0-")); // "classifier"

    assertTrue(locator.locate("foo").isEmpty());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "javafx.base",
        "javafx.controls",
        "javafx.fxml",
        "javafx.graphics",
        "javafx.media",
        "javafx.swing",
        "javafx.web"
      })
  void checkModuleIsLocatable(String module) {
    var locator = JavaFX.of("99", "Z");
    var optional = locator.locate(module);
    assertTrue(optional.isPresent());
    var location = optional.orElseThrow();
    assertEquals(module, location.module());
    assertTrue(location.uri().startsWith("https://repo.maven.apache.org/maven2"));
    assertTrue(location.uri().endsWith("-99-Z.jar"));
  }
}
