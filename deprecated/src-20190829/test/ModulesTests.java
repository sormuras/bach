import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Requires;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ModulesTests {

  @Test
  void minimalisticModuleDeclaration() {
    var descriptor = Bach.Modules.parseDeclaration("module a{}");
    assertEquals("a", descriptor.name());
    assertEquals("[mandated java.base]", descriptor.requires().toString());
  }

  @Test
  void moduleDeclarationWithSingleReadEdge() {
    var descriptor = Bach.Modules.parseDeclaration("module a{requires b;}");
    var requires = computeRequiresMap(descriptor);
    assertEquals("a", descriptor.name());
    assertEquals(Set.of("b", "java.base"), requires.keySet());
  }

  @Test
  void moduleDeclarationWithMainClass() {
    var descriptor = Bach.Modules.parseDeclaration("/* --main-class a.A */ module a{}");
    assertEquals("a", descriptor.name());
    assertEquals("a.A", descriptor.mainClass().orElseThrow());
  }

  @Test
  void findSystemModuleNames() {
    var names = Bach.Modules.findSystemModuleNames();
    assertTrue(names.contains("java.base"));
    assertTrue(names.contains("java.se"));
    assertTrue(names.contains("java.xml"));
  }

  @Test
  void moduleDeclarationWithRequiresAndVersion() {
    var descriptor = Bach.Modules.parseDeclaration("module a{requires b/*1.2*/;}");
    var requires = computeRequiresMap(descriptor);
    assertEquals("a", descriptor.name());
    assertEquals(Set.of("b", "java.base"), requires.keySet());
    assertEquals("1.2", requires.get("b").compiledVersion().orElseThrow().toString());
  }

  @Test
  void findExternalModuleNames() {
    assert Bach.Modules.findExternalModuleNames(Set.of()).isEmpty();
    var a = Bach.Modules.parseDeclaration("module a { requires java.scripting; }");
    assertTrue(Bach.Modules.findExternalModuleNames(Set.of(a)).isEmpty());
    var b = Bach.Modules.parseDeclaration("module b { requires a; }");
    assertTrue(Bach.Modules.findExternalModuleNames(Set.of(a, b)).isEmpty());
    assertEquals(Bach.Modules.findExternalModuleNames(Set.of(b)), Set.of("a"));
    var c = Bach.Modules.parseDeclaration("module c { requires a; \n requires b; }");
    assertTrue(Bach.Modules.findExternalModuleNames(Set.of(a, b, c)).isEmpty());
  }

  private static Map<String, Requires> computeRequiresMap(ModuleDescriptor descriptor) {
    var requires = new HashMap<String, Requires>();
    for (var dependence : descriptor.requires()) {
      requires.put(dependence.name(), dependence);
    }
    return requires;
  }
}
