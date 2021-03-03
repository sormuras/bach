package test.integration.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import com.github.sormuras.bach.project.ModuleDeclaration;
import com.github.sormuras.bach.project.ModuleDeclarations;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ModuleDeclarationsTests {

  @Nested
  class BachProject {
    @Test
    void checkModulePatches() {
      var module = "com.github.sormuras.bach";
      var main =
          new ModuleDeclarations(
              Map.of(module, ModuleDeclaration.of(Path.of(module, "main", "java"), false)));
      var test =
          new ModuleDeclarations(
              Map.of(module, ModuleDeclaration.of(Path.of(module, "test", "java-module"), false)));

      var expected = Map.of(module, Path.of(module, "main", "java").toString());
      assertEquals(expected, test.toModulePatches(main));
    }
  }

  @Nested
  class TestProjectJigsawQuickStartWorld {

    static Path root = Path.of("test.projects", "JigsawQuickStartWorld");
    static ModuleDeclarations declarations =
        new ModuleDeclarations(
            Map.of(
                "com.greetings",
                ModuleDeclaration.of(root, root.resolve("com.greetings"), false),
                "org.astro",
                ModuleDeclaration.of(root, root.resolve("org.astro"), false)));

    @Test
    void checkNames() {
      assertEquals("com.greetings,org.astro", declarations.toNames(","));
    }

    @Test
    void checkModuleSourcePathsInModuleSpecificForm() {
      var expected =
          List.of(
              "com.greetings=" + root.resolve("com.greetings"),
              "org.astro=" + root.resolve("org.astro"));
      assertLinesMatch(expected, declarations.toModuleSourcePaths(true));
    }

    @Test
    void checkModuleSourcePathsInModulePatternForm() {
      assertLinesMatch(List.of(root.toString()), declarations.toModuleSourcePaths(false));
    }
  }
}
