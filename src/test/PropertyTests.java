import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class PropertyTests {

  private final CollectingLogger logger = new CollectingLogger("*");
  private final Bach bach = new Bach(logger, Path.of("."), List.of());

  @ParameterizedTest
  @EnumSource(Bach.Property.class)
  void assertProperty(Bach.Property property) {
    assertTrue(property.key.startsWith("bach."));
    assertFalse(property.defaultValue.isBlank());
  }

  @Test
  void pathCacheTools() {
    assertNotNull(Bach.Property.PATH_CACHE_TOOLS);
    assertEquals(".bach/tools", bach.var.get(Bach.Property.PATH_CACHE_TOOLS));
    assertEquals(Path.of(".bach", "tools"), bach.based(Bach.Property.PATH_CACHE_TOOLS));
  }

  @Test
  void pathCacheModules() {
    assertNotNull(Bach.Property.PATH_CACHE_MODULES);
    assertEquals(".bach/modules", bach.var.get(Bach.Property.PATH_CACHE_MODULES));
    assertEquals(Path.of(".bach", "modules"), bach.based(Bach.Property.PATH_CACHE_MODULES));
  }

  @Test
  void toolJUnitUriEndsWithJar() {
    assertTrue(bach.var.get(Bach.Property.TOOL_JUNIT_URI).endsWith(".jar"));
  }
}
