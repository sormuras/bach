package test.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.module.ModuleInfoFinder;
import java.nio.file.Path;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
  void buildMultiRelease8(@TempDir Path temp) throws Exception {
    var context = new Context("MultiRelease8", temp);
    var output = context.build("-Dbach.project.base.release=8");

    assertLinesMatch(
        """
        Build project MultiRelease8 0-ea
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
    }
  }

  @Test
  void buildMultiRelease9(@TempDir Path temp) throws Exception {
    var context = new Context("MultiRelease9", temp);
    var output = context.build("-Dbach.project.base.release=9");

    assertLinesMatch(
        """
        Build project MultiRelease9 0-ea
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
    }
  }
}
