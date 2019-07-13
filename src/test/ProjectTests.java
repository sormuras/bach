import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.File;
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
    assertEquals(
        List.of("de.sormuras.bach.demo", "de.sormuras.bach.demo.multi", "integration"),
        project.modules);
  }

  @Test
  void checkRealmProperties() {
    var probe = new Probe();
    var main = probe.bach.project.main;
    assertEquals("main", main.name);
    assertEquals(
        "demo/src/*/main/java" + File.pathSeparator + "demo/src/*/main/java-9",
        main.moduleSourcePath.replace('\\', '/'));
    assertEquals(
        Set.of("de.sormuras.bach.demo", "de.sormuras.bach.demo.multi"),
        main.declaredModules.keySet());

    var test = probe.bach.project.test;
    assertSame(main, test.main);
    assertEquals("test", test.name);
    assertEquals(Set.of("de.sormuras.bach.demo", "integration"), test.declaredModules.keySet());
    assertEquals(Set.of("org.junit.jupiter.api"), test.externalModules);
  }
}
