package de.sormuras.bach;

import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Set;

public class UtilTests {
  public static void main(String[] args) {
    checkAssigned();
    checkFindDirectoryEntries();
    minimalisticModuleDeclaration();
    moduleDeclarationWithSingleReadEdge();
    moduleDeclarationWithRequiresAndVersion();
  }

  private static void checkAssigned() {
    assert Util.assigned(Path.of("a")).getNameCount() == 1;
    assert Util.assigned(Path.of("b"), "path").getNameCount() == 1;
  }

  private static void checkFindDirectoryEntries() {
    var files = Util.findDirectoryEntries(Path.of(""), Files::isRegularFile);
    assert files.contains("bach.properties");
  }

  private static void minimalisticModuleDeclaration() {
    var descriptor = Util.parseModuleDeclaration("module a{}");
    assert "a".equals(descriptor.name());
    assert "[mandated java.base]".equals(descriptor.requires().toString()) : descriptor;
  }

  private static void moduleDeclarationWithSingleReadEdge() {
    var descriptor = Util.parseModuleDeclaration("module a{requires b;}");
    var requires = new HashMap<String, ModuleDescriptor.Requires>();
    for (var dependence : descriptor.requires()) {
      requires.put(dependence.name(), dependence);
    }
    assert "a".equals(descriptor.name()) : descriptor;
    assert Set.of("b", "java.base").equals(requires.keySet()) : descriptor;
  }

  private static void moduleDeclarationWithRequiresAndVersion() {
    var descriptor = Util.parseModuleDeclaration("module a{requires b/*1.2*/;}");
    var requires = new HashMap<String, ModuleDescriptor.Requires>();
    for (var dependence : descriptor.requires()) {
      requires.put(dependence.name(), dependence);
    }
    assert "a".equals(descriptor.name());
    assert Set.of("b", "java.base").equals(requires.keySet()) : descriptor;
    assert "1.2".equals(requires.get("b").compiledVersion().orElseThrow().toString()) : descriptor;
  }
}
