import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.module.ModuleDescriptor;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ModulesParseTextBlockDeclarationTest {

  @Test
  void parseDeclaration() {
    var descriptor = Bach.Modules.parseDeclaration("""
        module foo.bar { // 3.3-ALPHA
          requires foo.bax; // @1.3
          requires foo.bay/*342*/;
          requires foo.baz; // 47.11
        }
        """);

    assertEquals("foo.bar", descriptor.name(), descriptor.toString());
    assertEquals(
        ModuleDescriptor.newModule("x")
            .requires("foo.bax")
            .requires(Set.of(), "foo.bay", ModuleDescriptor.Version.parse("342"))
            .requires("foo.baz")
            .build()
            .requires(),
        descriptor.requires(),
        descriptor.toString());
  }
}
