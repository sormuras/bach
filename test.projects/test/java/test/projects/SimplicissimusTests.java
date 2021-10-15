package test.projects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Configuration;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.project.ProjectScanner;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import test.base.ToolProviders;

class SimplicissimusTests {

  @Test
  void build() throws Exception {
    var project = TestProject.of("Simplicissimus");
    assertEquals(0, project.build().waitFor());

    var jar = project.root().resolve(".bach/workspace/modules/simplicissimus@99.jar");

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

  @Test
  void scan() {
    var base = Path.of("test.projects", "Simplicissimus");

    var expected =
        Project.of("Simplicissimus", "0-ea")
            .withSpaces(spaces -> spaces.withSpace("main", main -> main.withModule(base)));

    var bach = new Bach(Configuration.ofErrorsOnly(base));
    var scanned = new ProjectScanner(bach).scanProject();

    assertEquals(expected, scanned);
  }
}
