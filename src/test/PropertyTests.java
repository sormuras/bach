import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class PropertyTests {

  private final Bach bach = new Bach();

  @ParameterizedTest
  @EnumSource(Bach.Property.class)
  void assertProperty(Bach.Property property) {
    assertTrue(property.key.startsWith("bach."));
    assertFalse(property.defaultValue.isBlank());
  }

  @Test
  void pathCacheTools() {
    assertNotNull(Bach.Property.PATH_CACHE_TOOLS);
    assertEquals(".bach/tools", bach.get(Bach.Property.PATH_CACHE_TOOLS));
  }

  @Test
  void pathCacheModules() {
    assertNotNull(Bach.Property.PATH_CACHE_MODULES);
    assertEquals(".bach/modules", bach.get(Bach.Property.PATH_CACHE_MODULES));
  }

  @Test
  void executableJarToolUrisEndsWithJar() {
    assertTrue(bach.get(Bach.Property.TOOL_FORMAT_URI).endsWith(".jar"));
    assertTrue(bach.get(Bach.Property.TOOL_JUNIT_URI).endsWith(".jar"));
  }
}
