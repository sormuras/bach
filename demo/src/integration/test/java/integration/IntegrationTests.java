package integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import de.sormuras.bach.demo.multi.Multi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;

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

  @Test
  void multi() {
    var release = Runtime.version().feature() >= 11 ? 11 : 8;
    assertEquals("Multi (" + release + ")", new Multi().toString());
  }

  @Test
  @EnabledOnJre({JRE.JAVA_8, JRE.JAVA_9, JRE.JAVA_10})
  void multi8() {
    assertEquals("Multi (8)", new Multi().toString());
  }

  @Test
  @EnabledOnJre({JRE.JAVA_11, JRE.JAVA_12, JRE.JAVA_13, JRE.JAVA_14})
  void multi11() {
    assertEquals("Multi (11)", new Multi().toString());
  }
}
