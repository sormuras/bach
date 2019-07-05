package de.sormuras.bach;

import java.lang.module.ModuleDescriptor;
import java.util.Set;

public class UtilParseModuleDeclarationTest {

  public static void main(String[] args) {
        var descriptor = Util.parseModuleDeclaration("""
        module foo.bar { // 3.3-ALPHA
          requires foo.bax; // @1.3
          requires foo.bay/*342*/;
          requires foo.baz; // 47.11
        }
        """);

    assert "foo.bar".equals(descriptor.name()) : descriptor;
    assert ModuleDescriptor.newModule("x")
            .requires("foo.bax")
            .requires(Set.of(), "foo.bay", ModuleDescriptor.Version.parse("342"))
            .requires("foo.baz")
            .build()
            .requires()
            .equals(descriptor.requires())
        : descriptor;
  }
}
