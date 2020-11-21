package test.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.module.ModuleInfoFinder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import test.base.Classes;

class ProjectBuildTests {

  @Test
  void buildSimplicissimus(@TempDir Path temp) throws Exception {
    var context = new Context("Simplicissimus", temp);
    var output = context.build();

    assertLinesMatch(
        """
        Build project Simplicissimus 0-ea
        Compile main modules
        >> TOOL CALLS >>
        Build took .+s
        Logbook written to %s
        """
            .formatted(context.base.resolve(Project.WORKSPACE.resolve("logbook.md")).toUri())
            .lines(),
        output.lines());

    assertTrue(context.newModuleFinder().find("simplicissimus").isPresent());
  }

  @Test
  void buildJigsawQuickStartGreetings(@TempDir Path temp) throws Exception {
    var context = new Context("JigsawQuickStartGreetings", temp);
    var infos = ModuleInfoFinder.of(context.base, ".");
    assertTrue(infos.find("com.greetings").isPresent());

    var output = context.build();

    assertLinesMatch(
        """
        Build project JigsawQuickStartGreetings 0-ea
        Compile main modules
        >> TOOL CALLS >>
        Build took .+s
        Logbook written to %s
        """
            .formatted(context.base.resolve(Project.WORKSPACE.resolve("logbook.md")).toUri())
            .lines(),
        output.lines());

    assertTrue(context.newModuleFinder().find("com.greetings").isPresent());
  }

  @Test
  void buildJigsawQuickStartWorld(@TempDir Path temp) throws Exception {
    var context = new Context("JigsawQuickStartWorld", temp);
    var output = context.build();

    assertLinesMatch(
        """
        Build project JigsawQuickStartWorld 0-ea
        Compile main modules
        >> TOOL CALLS >>
        Build took .+s
        Logbook written to %s
        """
            .formatted(context.base.resolve(Project.WORKSPACE.resolve("logbook.md")).toUri())
            .lines(),
        output.lines());

    var finder = context.newModuleFinder();
    assertTrue(finder.find("com.greetings").isPresent());
    assertTrue(finder.find("org.astro").isPresent());
  }

  @Test
  void buildSingleRelease7(@TempDir Path temp) throws Exception {
    var context = new Context("SingleRelease-7", temp);
    var output = context.build("-Dbach.project.main.release=7");

    assertLinesMatch(
        """
        Build project SingleRelease-7 0-ea
        Compile main modules
        >> TOOL CALLS >>
        Build took .+
        Logbook written to %s
        """
            .formatted(context.workspace("logbook.md").toUri())
            .lines(),
        output.lines());

    var foo = context.newModuleFinder().find("foo").orElseThrow();
    assertEquals("foo", foo.descriptor().name());
    var path = Path.of(foo.location().orElseThrow());
    try (var jar = new JarFile(path.toFile())) {
      assertFalse(jar.isMultiRelease(), "A multi-release JAR file?! -> " + path);
      var names = new ArrayList<String>();
      jar.entries().asIterator().forEachRemaining(e -> names.add(e.getName()));
      assertLinesMatch(
          """
          META-INF/
          META-INF/MANIFEST.MF
          module-info.class
          foo/
          foo/Foo.class
          """
              .lines(),
          names.stream());
    }

    assertEquals(9, Classes.feature(context.workspace("classes-main/7/foo","module-info.class")));
    assertEquals(7, Classes.feature(context.workspace("classes-main/7/foo", "foo/Foo.class")));
  }

  @Test
  void buildSingleRelease8(@TempDir Path temp) throws Exception {
    var context = new Context("SingleRelease-8", temp);
    var output = context.build("-Dbach.project.main.release=8");

    assertLinesMatch(
        """
        Build project SingleRelease-8 0-ea
        Compile main modules
        >> TOOL CALLS >>
        Build took .+
        Logbook written to %s
        """
            .formatted(context.workspace("logbook.md").toUri())
            .lines(),
        output.lines());

    var foo = context.newModuleFinder().find("foo").orElseThrow();
    assertEquals("foo", foo.descriptor().name());
    var path = Path.of(foo.location().orElseThrow());
    try (var jar = new JarFile(path.toFile())) {
      assertFalse(jar.isMultiRelease(), "A multi-release JAR file?! -> " + path);
      var names = new ArrayList<String>();
      jar.entries().asIterator().forEachRemaining(e -> names.add(e.getName()));
      assertLinesMatch(
          """
          META-INF/
          META-INF/MANIFEST.MF
          module-info.class
          foo/
          foo/Foo.class
          """
              .lines(),
          names.stream());
    }

    assertEquals(9, Classes.feature(context.workspace("classes-main/8/foo","module-info.class")));
    assertEquals(8, Classes.feature(context.workspace("classes-main/8/foo", "foo/Foo.class")));
  }

  @Test
  void buildSingleRelease9(@TempDir Path temp) throws Exception {
    var context = new Context("SingleRelease-9", temp);
    var output = context.build("-Dbach.project.main.release=9");

    assertLinesMatch(
        """
        Build project SingleRelease-9 0-ea
        Compile main modules
        >> TOOL CALLS >>
        Build took .+
        Logbook written to %s
        """
            .formatted(context.workspace("logbook.md").toUri())
            .lines(),
        output.lines());

    var foo = context.newModuleFinder().find("foo").orElseThrow();
    assertEquals("foo", foo.descriptor().name());
    var path = Path.of(foo.location().orElseThrow());
    try (var jar = new JarFile(path.toFile())) {
      assertFalse(jar.isMultiRelease(), "A multi-release JAR file?! -> " + path);
      var names = new ArrayList<String>();
      jar.entries().asIterator().forEachRemaining(e -> names.add(e.getName()));
      assertLinesMatch(
          """
          META-INF/
          META-INF/MANIFEST.MF
          module-info.class
          foo/
          foo/Foo.class
          """
              .lines(),
          names.stream());
    }

    assertEquals(9, Classes.feature(context.workspace("classes-main/9/foo","module-info.class")));
    assertEquals(9, Classes.feature(context.workspace("classes-main/9/foo", "foo/Foo.class")));
  }

  @Test
  void buildMultiRelease9(@TempDir Path temp) throws Exception {
    var context = new Context("MultiRelease-9", temp);
    var output = context.build("-Dbach.project.main.release=9");

    assertLinesMatch(
        """
        Build project MultiRelease-9 0-ea
        Compile main modules
        >> TOOL CALLS >>
        Build took .+s
        Logbook written to %s
        """
            .formatted(context.base.resolve(Project.WORKSPACE.resolve("logbook.md")).toUri())
            .lines(),
        output.lines());

    var foo = context.newModuleFinder().find("foo").orElseThrow();
    assertEquals("foo", foo.descriptor().name());
    var path = Path.of(foo.location().orElseThrow());
    try (var jar = new JarFile(path.toFile())) {
      assertTrue(jar.isMultiRelease(), "Not a multi-release JAR file: " + path);
      var names = new ArrayList<String>();
      jar.entries().asIterator().forEachRemaining(e -> names.add(e.getName()));
      assertLinesMatch(
          """
          META-INF/
          META-INF/MANIFEST.MF
          module-info.class
          foo/
          foo/Foo.class
          META-INF/versions/11/
          META-INF/versions/11/foo/
          META-INF/versions/11/foo/Foo.class
          META-INF/versions/15/
          META-INF/versions/15/foo/
          META-INF/versions/15/foo/Foo.class          
          """
              .lines(),
          names.stream());
    }

    assertEquals(9, Classes.feature(context.workspace("classes-main/9/foo","module-info.class")));
    assertEquals(9, Classes.feature(context.workspace("classes-main/9/foo", "foo/Foo.class")));
    assertEquals(11, Classes.feature(context.workspace("classes-mr/11/foo", "foo/Foo.class")));
    assertEquals(15, Classes.feature(context.workspace("classes-mr/15/foo", "foo/Foo.class")));
  }
}
