import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class VariablesTests {

  private final CollectingLogger logger = new CollectingLogger("*");
  private final Bach bach = new Bach(logger, Path.of("."), List.of());

  @Test
  void defaults() {
    assertEquals(0, bach.var.properties.size());
    assertFalse(bach.var.offline);
  }

  @Test
  void loadPropertiesFromDirectoryFails() {
    assertThrows(Error.class, () -> bach.var.load(Path.of(".")));
  }

  @Test
  void loadPropertiesFromTestResources() {
    var path = Path.of("src", "test-resources", "Property.load.properties");
    var map = bach.var.load(path);
    assertEquals("true", map.get("bach.offline"));
    assertEquals("Test Project Name", map.get("project.name"));
    assertEquals("1.2.3-SNAPSHOT", map.get("project.version"));
    assertEquals(3, map.size());
  }

  @Test
  void systemPropertyOverridesManagedProperty() {
    var key = "bach.test.systemPropertyOverridesManagedProperty";
    assertNull(bach.var.get(key, null));
    bach.var.properties.setProperty(key, "123");
    assertEquals("123", bach.var.get(key, "456"));
    System.setProperty(key, "789");
    assertEquals("789", bach.var.get(key, "456"));
    System.clearProperty(key);
  }
}
