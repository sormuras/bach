package integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Test;

class IntegrationTests {
  @Test
  void moduleIsNamedIntegration() {
    assumeTrue(getClass().getModule().isNamed(), "test module is named");
    assertEquals("integration", getClass().getModule().getName());
  }

  @Test
  void mainMethodExists() throws ReflectiveOperationException {
    @SuppressWarnings("Java9ReflectionClassVisibility")
    var mainClass = Class.forName("de.sormuras.bach.demo.DemoMain");
    assertNotNull(mainClass.getMethod("main", String[].class));
  }
}
