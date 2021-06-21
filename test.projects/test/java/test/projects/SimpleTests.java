package test.projects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import test.base.ToolProviders;

@Disabled
class SimpleTests {

  @Test
  void build() throws Exception {
    var project = TestProject.of("Simple");
    assertEquals(0, project.build().waitFor());

    var jar =  project.root().resolve("workspace/modules/simple@1.0.1.jar");
    assertLinesMatch(
        """
        META-INF/
        META-INF/MANIFEST.MF
        module-info.class
        simple/
        simple/Main.class
        simple/internal/
        simple/internal/Interface.class
        """
            .lines()
            .sorted(),
        ToolProviders.run("jar", "--list", "--file", jar).lines().sorted());
  }
}
