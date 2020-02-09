// default package

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.module.ModuleDescriptor.Version;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ProjectTests {

  @Nested
  class CustomProject {

    final Bach.Project project =
        Bach.newProject("custom")
            .version("1.2-C")
            // .requires("java.base", "11")
            .requires("foo", "4711")
            .requires("bar", "1701")
            .build();

    @Test
    void toStringRepresentationIsLegit() {
      assertNotNull(project.toString());
      assertFalse(project.toString().isBlank());
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
}
