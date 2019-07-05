package de.sormuras.bach;

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Requires;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ModulesTests {
  public static void main(String[] args) {
    minimalisticModuleDeclaration();
    moduleDeclarationWithSingleReadEdge();
    moduleDeclarationWithRequiresAndVersion();
    findExternalModuleNames();
  }

  private static void minimalisticModuleDeclaration() {
    var descriptor = Modules.parseDeclaration("module a{}");
    assert "a".equals(descriptor.name());
    assert "[mandated java.base]".equals(descriptor.requires().toString()) : descriptor;
  }

  private static void moduleDeclarationWithSingleReadEdge() {
    var descriptor = Modules.parseDeclaration("module a{requires b;}");
    var requires = computeRequiresMap(descriptor);
    assert "a".equals(descriptor.name()) : descriptor;
    assert Set.of("b", "java.base").equals(requires.keySet()) : descriptor;
  }

  private static void moduleDeclarationWithRequiresAndVersion() {
    var descriptor = Modules.parseDeclaration("module a{requires b/*1.2*/;}");
    var requires = computeRequiresMap(descriptor);
    assert "a".equals(descriptor.name());
    assert Set.of("b", "java.base").equals(requires.keySet()) : descriptor;
    assert "1.2".equals(requires.get("b").compiledVersion().orElseThrow().toString()) : descriptor;
  }

  private static void findExternalModuleNames() {
    assert Modules.findExternalModuleNames(Set.of()).isEmpty();
    var a = Modules.parseDeclaration("module a { requires java.scripting; }");
    assert Modules.findExternalModuleNames(Set.of(a)).isEmpty() : a;
    var b = Modules.parseDeclaration("module b { requires a; }");
    assert Modules.findExternalModuleNames(Set.of(a, b)).isEmpty() : b;
    assert Modules.findExternalModuleNames(Set.of(b)).equals(Set.of("a")) : b;
    var c = Modules.parseDeclaration("module c { requires a; \n requires b; }");
    assert Modules.findExternalModuleNames(Set.of(a, b, c)).isEmpty() : c;
  }

  private static Map<String, Requires> computeRequiresMap(ModuleDescriptor descriptor) {
    var requires = new HashMap<String, Requires>();
    for (var dependence : descriptor.requires()) {
      requires.put(dependence.name(), dependence);
    }
    return requires;
  }
}
