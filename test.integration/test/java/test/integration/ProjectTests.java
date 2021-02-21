package test.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.project.JavaStyle;
import com.github.sormuras.bach.project.Libraries;
import java.lang.module.ModuleDescriptor.Version;
import org.junit.jupiter.api.Test;

class ProjectTests {

  @Test
  void defaults() {
    var project = new Project();
    assertEquals("noname", project.name().name());
    assertEquals(Version.parse("0"), project.version().version());
    assertEquals(Libraries.of(), project.libraries());
    assertEquals(JavaStyle.FREE, project.spaces().style());
  }
}
