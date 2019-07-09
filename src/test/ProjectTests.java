import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ProjectTests {

  @Test
  void checkProjectProperties() {
    var probe = new Probe();
    var project = probe.bach.project;
    assertEquals("demo", project.name, "expected 'demo' as name, but got: " + project.name);
    assertEquals("1", project.version.toString());
    assertEquals(List.of("de.sormuras.bach.demo"), project.modules);
  }

  @Test
  void checkRealmProperties() {
    var probe = new Probe();
    var main = probe.bach.project.main;
    var test = probe.bach.project.test;
    // main
    assertEquals("main", main.name);
    assertEquals("demo/src/*/main/java", main.moduleSourcePath.replace('\\', '/'));
    assertEquals(Set.of("de.sormuras.bach.demo"), main.declaredModules.keySet());
    // test
    assertEquals("test", test.name);
    assertEquals(Set.of(), test.declaredModules.keySet());
  }
}
