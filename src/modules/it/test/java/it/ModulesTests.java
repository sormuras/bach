package it;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.sormuras.bach.Modules;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ModulesTests {
  @Test
  void minimalisticModuleDeclaration() {
    var actual = Modules.describe("module a{}");
    assertEquals(ModuleDescriptor.newModule("a").build(), actual);
  }

  @Test
  void moduleDeclarationWithRequires() {
    var actual = Modules.describe("module a{requires b;}");
    assertEquals(ModuleDescriptor.newModule("a").requires("b").build(), actual);
  }

  @Test
  void moduleDeclarationWithRequiresAndVersion() {
    var actual = Modules.describe("module a{requires b/*1.2*/;}");
    assertEquals(
        ModuleDescriptor.newModule("a").requires(Set.of(), "b", Version.parse("1.2")).build(),
        actual);
  }

  @Test
  void moduleDeclarationWithProvides() {
    var actual = Modules.describe("module a{provides a.B with a.C;}");
    assertEquals(ModuleDescriptor.newModule("a").provides("a.B", List.of("a.C")).build(), actual);
  }

  @Test
  void moduleDeclarationWithProvidesTwoImplementations() {
    var actual = Modules.describe("module a{provides a.B with a.C,a.D;}");
    assertEquals(
        ModuleDescriptor.newModule("a").provides("a.B", List.of("a.C", "a.D")).build(), actual);
  }

  @Test
  void moduleDeclarationWithMainClass() {
    var actual = Modules.describe("// --main-class a.Main\nmodule a{}");
    assertEquals(ModuleDescriptor.newModule("a").mainClass("a.Main").build(), actual);
  }

  @Test
  void moduleDeclarationWithComments() {
    var actual = Modules.describe("open /*test*/ module a /*extends a*/ {}");
    assertEquals(ModuleDescriptor.newModule("a").build(), actual);
  }
}
