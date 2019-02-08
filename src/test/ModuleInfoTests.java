import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ModuleInfoTests {

  @Test
  void moduleInfoEmpty() {
    var info = ModuleInfo.of(List.of("module foo {}"));
    assertEquals("foo", info.name);
    assertTrue(info.requires.isEmpty());
  }

  @Test
  void moduleInfoFromModuleWithoutNameFails() {
    var source = "module { no name }";
    Exception e = assertThrows(IllegalArgumentException.class, () -> ModuleInfo.of(source));
    assertEquals("expected java module descriptor unit, but got: " + source, e.getMessage());
  }

  @Test
  void moduleInfoFromNonExistingFileFails() {
    var source = Path.of(".", "module-info.java");
    var exception = assertThrows(Exception.class, () -> ModuleInfo.of(source));
    assertEquals("reading '" + source + "' failed", exception.getMessage());
  }

  @Test
  void moduleInfoRequiresBarAndBaz() {
    var source = "module   foo{requires a; requires static b; requires any modifier c;}";
    var info = ModuleInfo.of(source);
    assertEquals("foo", info.name);
    assertEquals(3, info.requires.size());
    assertTrue(info.requires.contains("a"));
    assertTrue(info.requires.contains("b"));
    assertTrue(info.requires.contains("c"));
  }

  @Test
  void moduleInfoFromFile() {
    var source = Path.of("demo/02-testing/src/test/java/application");
    var info = ModuleInfo.of(source);
    assertEquals("application", info.name);
    assertEquals(2, info.requires.size());
    assertTrue(info.requires.contains("application.api"));
    assertTrue(info.requires.contains("org.junit.jupiter.api"));
  }

  @Test
  void moduleInfoFromM1() throws Exception {
    var loader = getClass().getClassLoader();
    var resource = loader.getResource("UtilTests.module-info.java");
    if (resource == null) {
      fail("resource not found!");
    }
    var info = ModuleInfo.of(Path.of(resource.toURI()));
    assertEquals("com.google.m", info.name);
    assertEquals(3, info.requires.size());
    assertTrue(info.requires.contains("com.google.r1"));
    assertTrue(info.requires.contains("com.google.r2"));
    assertTrue(info.requires.contains("com.google.r3"));
  }

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
    assertEquals("expected java module descriptor unit, but got: " + source, e.getMessage());
  }

  private void assertModuleNameIs(String expected, String source) {
    assertEquals(expected, ModuleInfo.of(source).name);
  }

  @Test
  void findSystemModuleNames() {
    var names = ModuleInfo.findSystemModuleNames();
    assertTrue(names.contains("java.base"));
    assertTrue(names.contains("java.compiler"));
    assertTrue(names.contains("java.desktop"));
    assertTrue(names.contains("java.scripting"));
    assertTrue(names.contains("java.sql"));
    assertTrue(names.contains("java.xml"));
    assertTrue(names.contains("jdk.accessibility"));
    assertTrue(names.contains("jdk.jartool"));
    assertTrue(names.contains("jdk.javadoc"));
    assertTrue(names.contains("jdk.zipfs"));
    assertFalse(names.contains("hello"));
    assertFalse(names.contains("world"));
  }

  @Test
  void findExternalModuleNamesInDemoProjects() {
    var names = ModuleInfo.findExternalModuleNames(Set.of(Path.of("demo")));
    assertTrue(names.contains("org.junit.jupiter.api"));
    assertFalse(names.contains("hello"));
    assertFalse(names.contains("world"));
  }

  @Test
  void findExternalModuleNamesForNonExistingPathFails() {
    var paths = Set.of(Path.of("does not exist"));
    var e = assertThrows(Exception.class, () -> ModuleInfo.findExternalModuleNames(paths));
    assertEquals("walking path failed for: does not exist", e.getMessage());
  }
}
