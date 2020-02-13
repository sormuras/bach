// default package

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ProjectTests {

  @Nested
  class CustomProject {

    final Bach.Project project =
        new Bach.Project.Builder("custom")
            .paths(Path.of("custom"))
            .version("1.2-C")
            // .requires("java.base", "11")
            .requires("foo", "4711")
            .requires("bar", "1701")
            .build();

    @Test
    void print() {
      var lines = new ArrayList<String>();
      project.print(lines::add);
      assertLinesMatch(
          List.of(
              "Project",
              "  descriptor = module { name: custom@1.2-C, requires: [synthetic bar (@1701), synthetic foo (@4711), mandated java.base] }",
              "  paths -> instance of Bach$Project$Paths",
              "  Paths",
              "    base = custom",
              "    lib = " + Path.of("custom/lib"),
              "    out = " + Path.of("custom/.bach")),
          lines);
    }

    @Test
    void toStringRepresentationIsLegit() {
      assertNotNull(project.toString());
      assertFalse(project.toString().isBlank());
    }

    @Test
    void base() {
      assertEquals(Path.of("custom"), project.paths().base());
    }

    @Test
    void out() {
      assertEquals(Path.of("custom", ".bach"), project.paths().out());
    }

    @Test
    void lib() {
      assertEquals(Path.of("custom", "lib"), project.paths().lib());
    }

    @Test
    void name() {
      assertEquals("custom", project.descriptor().name());
    }

    @Test
    void version() {
      assertEquals(Version.parse("1.2-C"), project.descriptor().version().orElseThrow());
    }

    @Test
    void requires() {
      assertEquals(
          List.of("mandated java.base", "synthetic bar (@1701)", "synthetic foo (@4711)"),
          project.descriptor().requires().stream()
              .map(Object::toString)
              .sorted()
              .collect(Collectors.toList()));
    }
  }

  @Nested
  class Sources {

    @Test
    void simple() {
      var source = Bach.Project.Source.of(Path.of("src"));
      assertEquals("src", source.path().toString());
      assertEquals(0, source.release());
      assertEquals(OptionalInt.empty(), source.target());
      assertFalse(source.isTargeted());
      assertFalse(source.isVersioned());
    }

    @Test
    void targeted() {
      var source = new Bach.Project.Source(Path.of("src"), 123, Set.of());
      assertEquals("src", source.path().toString());
      assertEquals(123, source.release());
      assertEquals(123, source.target().orElseThrow());
      assertTrue(source.isTargeted());
      assertFalse(source.isVersioned());
    }

    @Test
    void versioned() {
      var versioned = Set.of(Bach.Project.Source.Modifier.VERSIONED);
      var source = new Bach.Project.Source(Path.of("src-789"), 789, versioned);
      assertEquals("src-789", source.path().toString());
      assertEquals(789, source.release());
      assertEquals(789, source.target().orElseThrow());
      assertTrue(source.isTargeted());
      assertTrue(source.isVersioned());
    }
  }
}
