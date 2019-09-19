package de.sormuras.bach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.module.ModuleDescriptor;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DomainTests {

  @Test
  void manual() {
    var foo =
        new Domain.ModuleSource(
            Path.of("src/foo/main/java/module-info.java"),
            Path.of("src/foo/main/java"),
            Path.of("src/foo/main/resources"),
            ModuleDescriptor.newModule("foo").version("1").build());

    var bar =
        new Domain.ModuleSource(
            Path.of("src/bar/test/java/module-info.java"),
            Path.of("src/bar/test/java"),
            Path.of("src/bar/test/resources"),
            ModuleDescriptor.newOpenModule("bar").requires("foo").build());

    var main = new Domain.MainRealm("src/*/main/java", Map.of("foo", foo));
    var test = new Domain.TestRealm(main, "src/*/test/java", Map.of("bar", bar));

    var library =
        new Domain.Library(
            List.of(Path.of("lib")), DomainTests::throwUnsupportedOperationException);

    var project =
        new Domain.Project(
            "manual",
            ModuleDescriptor.Version.parse("0"),
            library,
            List.of(main, test),
            Path.of("bin"));

    assertEquals("manual", project.name);
    assertEquals("0", project.version.toString());
    assertEquals("[lib]", project.library.modulePaths.toString());
    assertThrows(UnsupportedOperationException.class, () -> project.library.moduleMapper.apply(""));
    assertEquals(List.of(main, test), project.realms);
    assertEquals(Path.of("bin"), project.target);
    assertEquals("main", main.name);
    assertEquals("src/*/main/java", main.moduleSourcePath);
    assertEquals(foo, main.modules.get("foo"));
    assertNull(main.modules.get("?"));
    assertEquals("test", test.name);
    assertEquals("src/*/test/java", test.moduleSourcePath);
    assertEquals(bar, test.modules.get("bar"));
    assertNull(test.modules.get("?"));
  }

  private static URI throwUnsupportedOperationException(String module) {
    throw new UnsupportedOperationException("module " + module + " not mapped");
  }
}
