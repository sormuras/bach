package test.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.sormuras.bach.Libraries;
import com.github.sormuras.bach.Project;
import java.lang.module.ModuleDescriptor.Version;
import org.junit.jupiter.api.Test;

class ProjectTests {

  @Test
  void defaults() {
    var project = new Project();
    assertEquals("noname", project.name());
    assertEquals(Version.parse("0"), project.version());
    assertEquals(Libraries.of(), project.libraries());
  }
}
