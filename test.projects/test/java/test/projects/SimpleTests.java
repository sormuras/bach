package test.projects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import org.junit.jupiter.api.Test;
import test.base.ToolProviders;

class SimpleTests {

  @Test
  void build() throws Exception {
    var project = TestProject.of("Simple");
    assertEquals(0, project.build().waitFor());

    var jar =  project.root().resolve(".bach/workspace/modules/simple@1.0.1.jar");
    assertLinesMatch(
        """
        META-INF/
        META-INF/MANIFEST.MF
        module-info.class
        module-info.java
        simple/
        simple/Main.class
        simple/Main.java
        simple/Main.txt
        simple/internal/
        simple/internal/Interface.class
        simple/internal/Interface.java
        """
            .lines()
            .sorted(),
        ToolProviders.run("jar", "--list", "--file", jar).lines().sorted());
  }
}
