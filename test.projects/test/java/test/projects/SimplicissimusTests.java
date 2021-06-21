package test.projects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import test.base.ToolProviders;

@Disabled
class SimplicissimusTests {

  @Test
  void build() throws Exception {
    var project = TestProject.of("Simplicissimus");
    assertEquals(0, project.build().waitFor());

    var jar =  project.root().resolve("workspace/modules/simplicissimus@99.jar");

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
