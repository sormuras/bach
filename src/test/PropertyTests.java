import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class PropertyTests {

  private final Bach bach = new Bach();

  @ParameterizedTest
  @EnumSource(Bach.Property.class)
  void assertProperty(Bach.Property property) {
    assertTrue(property.key.startsWith("bach."));
    assertNotNull(property.defaultValue);
  }

  @Test
  void base() {
    assertEquals(".", Bach.Property.BASE.get());
    assertEquals(".", bach.get(Bach.Property.BASE));
  }

  @Test
  void loadPropertiesFromDirectoryFails() {
    assertThrows(Error.class, () -> Bach.Property.loadProperties(Path.of(".")));
  }

  //  @Test
  //  void loadPropertiesFromTestResources() {
  //    var path = Path.of("src", "test-resources", "Property.load.properties");
  //    var map = bach.var.load(path);
  //    assertEquals("true", map.get("bach.offline"));
  //    assertEquals("Test Project Name", map.get("project.name"));
  //    assertEquals("1.2.3-SNAPSHOT", map.get("project.version"));
  //    assertEquals(3, map.size());
  //  }

  @Test
  void systemPropertyOverridesManagedProperty() {
    var key = "bach.test.systemPropertyOverridesManagedProperty";
    assertNull(bach.get(key, null));
    bach.properties.setProperty(key, "123");
    assertEquals("123", bach.get(key, "456"));
    System.setProperty(key, "789");
    assertEquals("789", bach.get(key, "456"));
    System.clearProperty(key);
  }

  @Test
  void getMultipleValuesForSingleKey() {
    var property = Bach.Property.PROJECT_LAUNCH_OPTIONS;
    bach.properties.setProperty(property.key, "a: b:  c  ");
    var actual = bach.get(property, ":").collect(Collectors.toList());
    assertEquals(List.of("a", "b", "c"), actual);
    bach.properties.setProperty(property.key, " \t\r\n ");
    assertEquals(0, bach.get(property, ":").count());
    bach.properties.setProperty(property.key, property.defaultValue);
  }

  //  @Test
  //  void pathCacheTools() {
  //    assertNotNull(Bach.Property.PATH_CACHE_TOOLS);
  //    assertEquals(".bach/tools", bach.get(Bach.Property.PATH_CACHE_TOOLS));
  //  }

  //  @Test
  //  void pathCacheModules() {
  //    assertNotNull(Bach.Property.PATH_CACHE_MODULES);
  //    assertEquals(".bach/modules", bach.get(Bach.Property.PATH_CACHE_MODULES));
  //  }

  //  @Test
  //  void executableJarToolUrisEndsWithJar() {
  //    assertTrue(bach.get(Bach.Property.TOOL_FORMAT_URI).endsWith(".jar"));
  //    assertTrue(bach.get(Bach.Property.TOOL_JUNIT_URI).endsWith(".jar"));
  //  }
}
