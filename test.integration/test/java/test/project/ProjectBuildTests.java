package test.project;

import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.module.ModuleInfoFinder;
import java.nio.file.Path;
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
        """
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
        """
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
        """
            .lines(),
        output.lines());

    var finder = context.newModuleFinder();
    assertTrue(finder.find("com.greetings").isPresent());
    assertTrue(finder.find("org.astro").isPresent());
  }
}
