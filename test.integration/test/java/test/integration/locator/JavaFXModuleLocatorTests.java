package test.integration.locator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.api.ExternalModuleLocator;
import com.github.sormuras.bach.locator.JavaFXModuleLocator;
import com.github.sormuras.bach.locator.Maven;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JavaFXModuleLocatorTests {
  @Test
  void defaults() {
    var locator = new JavaFXModuleLocator("0");

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
    var locator = new JavaFXModuleLocator("99", "Z");
    var optional = locator.locate(module);
    assertTrue(optional.isPresent());
    var location = optional.orElseThrow();
    assertEquals(module, location.module());
    assertTrue(location.uri().startsWith(Maven.CENTRAL_REPOSITORY));
    assertTrue(location.uri().endsWith("-99-Z.jar"));
  }
}
