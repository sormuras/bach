import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ModulesParseTextBlockDeclarationTest {

  @Test
  void parseDeclaration() {
    var descriptor = Bach.Modules.parseDeclaration("""
        module foo.bar {
          requires foo.bax;
          requires foo.bay/*1*/;
          requires foo.baz /* 2.3-ea */;
        }
        """);

    assertEquals("foo.bar", descriptor.name(), descriptor.toString());
    assertEquals(
        ModuleDescriptor.newModule("x")
            .requires("foo.bax")
            .requires(Set.of(), "foo.bay", Version.parse("1"))
            .requires(Set.of(), "foo.baz", Version.parse("2.3-ea"))
            .build()
            .requires(),
        descriptor.requires(),
        descriptor.toString());
  }
}
