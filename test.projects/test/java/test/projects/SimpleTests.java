package test.projects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import com.github.sormuras.bach.api.CodeSpace;
import org.junit.jupiter.api.Test;
import test.base.ToolProviders;

class SimpleTests {

  @Test
  void build() throws Exception {
    var project = TestProject.of("Simple");
    assertEquals(0, project.build().waitFor());

    var jar = project.folders().modules(CodeSpace.MAIN, "simple@1.0.1.jar");
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
