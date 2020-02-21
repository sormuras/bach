// default package

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DocTests {

  private final Log log = new Log();
  private final Bach bach = new Bach(log, true);

  @Test
  void buildProjectJigsawQuickStart(@TempDir Path temp) {
    var name = "jigsaw.quick.start";
    var base = Path.of("doc/project", name);
    var summary = build(base, temp);
    var project = summary.project();
    assertEquals(name, project.descriptor().name());
    assertEquals("com.greetings", project.mainModule().orElseThrow());
    var units = project.units();
    assertEquals(1, units.size());
    var layout = Bach.Project.Layout.FLAT;
    assertEquals(layout, Bach.Project.Layout.find(units).orElseThrow());
    var greetings = units.get(0);
    try {
      assertEquals("com.greetings", greetings.name());
      assertEquals(base.resolve("com.greetings/module-info.java"), greetings.path());
      assertEquals(base.toString(), greetings.moduleSourcePath());
      assertEquals("", layout.realmOf(greetings).orElseThrow());
      assertEquals(1, greetings.sources().size());
      assertEquals(0, greetings.resources().size());
      assertTrue(greetings.isMainClassPresent());
      assertEquals("com.greetings.Main", greetings.descriptor().mainClass().orElseThrow());
      assertFalse(greetings.isMultiRelease());
    } catch (AssertionError error) {
      greetings.print(System.err::println);
      throw error;
    }
  }

  @Test
  void buildProjectJigsawQuickStartAndTesting(@TempDir Path temp) {
    var name = "jigsaw.quick.start.and.testing";
    var base = Path.of("doc/project", name);
    var summary = build(base, temp);
    var project = summary.project();
    assertEquals(name, project.descriptor().name());
    assertEquals("com.greetings", project.mainModule().orElseThrow());
    var units = project.units();
    assertEquals(3, units.size());
    var layout = Bach.Project.Layout.MAIN_TEST;
    assertEquals(layout, Bach.Project.Layout.find(units).orElseThrow());
    for (var unit : units) {
      if (unit.path().equals(base.resolve("com.greetings/src/main/java/module-info.java"))) {
        assertEquals("main", layout.realmOf(unit).orElseThrow());
        assertTrue(unit.isMainClassPresent());
      }
      if (unit.path().equals(base.resolve("org.astro/src/main/java/module-info.java"))) {
        assertEquals("main", layout.realmOf(unit).orElseThrow());
        assertFalse(unit.isMainClassPresent());
      }
      if (unit.path().equals(base.resolve("test.modules/src/test/java/module-info.java"))) {
        assertEquals("test", layout.realmOf(unit).orElseThrow());
        assertFalse(unit.isMainClassPresent());
      }
    }
  }

  @Test
  void buildProjectJigsawQuickStartWithJUnit(@TempDir Path temp) {
    var name = "jigsaw.quick.start.with.junit";
    var base = Path.of("doc/project", name);
    var summary = build(base, temp);
    var project = summary.project();
    assertEquals(name, project.descriptor().name());
    assertEquals("com.greetings", project.mainModule().orElseThrow());
    var units = project.units();
    assertEquals(5, units.size());
    var layout = Bach.Project.Layout.MAIN_TEST;
    assertEquals(layout, Bach.Project.Layout.find(units).orElseThrow());
    for (var unit : units) {
      if (unit.path().equals(base.resolve("src/com.greetings/main/java/module-info.java"))) {
        assertEquals("main", layout.realmOf(unit).orElseThrow());
        assertTrue(unit.isMainClassPresent());
      }
      if (unit.path().equals(base.resolve("src/org.astro/main/java/module-info.java"))) {
        assertEquals("main", layout.realmOf(unit).orElseThrow());
        assertFalse(unit.isMainClassPresent());
      }
      if (unit.path().equals(base.resolve("src/test.base/test/java/module-info.java"))) {
        assertEquals("test", layout.realmOf(unit).orElseThrow());
        assertFalse(unit.isMainClassPresent());
      }
      if (unit.path().equals(base.resolve("src/test.modules/test/java/module-info.java"))) {
        assertEquals("test", layout.realmOf(unit).orElseThrow());
        assertFalse(unit.isMainClassPresent());
      }
      if (unit.path().equals(base.resolve("src/org.astro/test/java/module-info.java"))) {
        assertEquals("test", layout.realmOf(unit).orElseThrow());
        assertFalse(unit.isMainClassPresent());
      }
    }
  }

  private Bach.Build.Summary build(Path base, Path temp) {
    var paths = new Bach.Project.Paths(base, temp.resolve("out"), temp.resolve("lib"));
    var summary = bach.build(paths, project -> project.version("0-ea"));
    try {
      assertDoesNotThrow(summary::assertSuccessful);
    } catch (AssertionError e) {
      log.lines().forEach(System.out::println);
      summary.toMarkdown().forEach(System.out::println);
      throw e;
    }
    return summary;
  }
}
