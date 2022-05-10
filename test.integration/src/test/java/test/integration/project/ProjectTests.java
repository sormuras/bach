package test.integration.project;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.sormuras.bach.project.Project;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProjectTests {

  @Test
  void defaultProject() {
    var project = Project.ofDefaults();
    assertEquals("unnamed", project.name().toString());
    assertEquals(0, project.modules().size());
  }

  @Test
  void customProject() {
    var project =
        Project.ofDefaults()
            .withModule("init", "doc/example-projects/processing-code/processor/src/init/java")
            .withModule("doc/example-projects/processing-code/production/src/main/java");
    assertEquals(2, project.modules().size());
    assertEquals(List.of("processor"), project.spaces().init().modules().names());
    assertEquals(List.of("production"), project.spaces().main().modules().names());
  }
}
