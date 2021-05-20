package test.integration.locator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.api.ExternalModuleLocator;
import com.github.sormuras.bach.locator.FXGL;
import com.github.sormuras.bach.locator.Maven;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FXGLTests {
  @Test
  void defaults() {
    var locator = FXGL.of("0");

    assertSame(ExternalModuleLocator.Stability.STABLE, locator.stability());
    assertEquals("com.almasb.fxgl[*] -> FXGL 0", locator.title());

    assertTrue(locator.locate("foo").isEmpty());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "com.almasb.fxgl.all",
        "com.almasb.fxgl.core",
        "com.almasb.fxgl.cutscene",
        "com.almasb.fxgl.entity",
        "com.almasb.fxgl.gameplay",
        "com.almasb.fxgl.input",
        "com.almasb.fxgl.io",
        "com.almasb.fxgl.localization",
        "com.almasb.fxgl.media",
        "com.almasb.fxgl.profiles",
        "com.almasb.fxgl.scene",
        "com.almasb.fxgl.ui"
      })
  void checkModuleIsLocatable(String module) {
    var locator = FXGL.of("99");
    var optional = locator.locate(module);
    assertTrue(optional.isPresent());
    var location = optional.orElseThrow();
    assertEquals(module, location.module());
    assertTrue(location.uri().startsWith(Maven.CENTRAL_REPOSITORY));
    assertTrue(location.uri().endsWith("-99.jar"));
  }
}
