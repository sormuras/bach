import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class ProjectTests {

  @Test
  void checkProjectProperties() {
    var probe = new Probe();
    var project = probe.bach.project;
    assertEquals("demo", project.name, "expected 'demo' as name, but got: " + project.name);
    assertEquals("1", project.version.toString());
    assertEquals(List.of("de.sormuras.bach.demo"), project.modules);

    // main
    // assert "main".equals(project.main.name);
    // assert "src/modules/*/main/java".equals(project.main.moduleSourcePath.replace('\\', '/'));
    // assert "[de.sormuras.bach]".equals(project.main.declaredModules.keySet().toString());

    // test
    // assert "test".equals(project.test.name);

    // assert project.test.moduleSourcePath.replace('\\',
    // '/').startsWith("src/modules/*/test/java");

    // assert project.test.moduleSourcePath.replace('\\',
    // '/').endsWith("src/modules/*/test/module");

    // assert "[de.sormuras.bach,
    // integration]".equals(project.test.declaredModules.keySet().toString());
  }
}
