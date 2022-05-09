package test.integration.project;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.sormuras.bach.project.Project;
import org.junit.jupiter.api.Test;

class ProjectTests {

  @Test
  void defaultProject() {
    var project = Project.ofDefaults();
    assertEquals("unnamed", project.name().toString());
  }
}
