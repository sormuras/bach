package test.integration;

import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.project.JavaStyle;
import com.github.sormuras.bach.project.Libraries;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProjectTests {

  @Test
  void defaults() {
    var project = new Project();
    assertEquals("noname", project.name());
    assertEquals("0", project.version());
    assertEquals(Libraries.of(), project.libraries());
    assertEquals(JavaStyle.FREE, project.spaces().style());
  }
}
