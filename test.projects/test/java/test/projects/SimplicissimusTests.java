package test.projects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import org.junit.jupiter.api.Test;
import test.base.ToolProviders;

class SimplicissimusTests {

  @Test
  void build() throws Exception {
    var project = TestProject.of("Simplicissimus");
    assertEquals(0, project.build().waitFor());

    var jar =  project.root().resolve(".bach/workspace/modules/simplicissimus@99.jar");

    assertLinesMatch(
        """
        META-INF/
        META-INF/MANIFEST.MF
        module-info.class
        """
            .lines()
            .sorted(),
        ToolProviders.run("jar", "--list", "--file", jar).lines().sorted());
  }
}
