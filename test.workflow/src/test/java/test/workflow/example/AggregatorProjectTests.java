package test.workflow.example;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Paths;
import com.github.sormuras.bach.Printer;
import com.github.sormuras.bach.project.ProjectSpaces;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import test.base.resource.ResourceManager;
import test.base.resource.TempDir;

@ExtendWith(ResourceManager.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AggregatorProjectTests {

  private final Bach bach;

  AggregatorProjectTests(@TempDir Path temp) {
    var root = Path.of("test.workflow", "example-projects", "aggregator");
    this.bach =
        Bach.of(
            Printer.ofSilence(),
            args ->
                args.with("--verbose")
                    .with("--root-directory", root)
                    .with("--output-directory", temp));
  }

  @Nested
  class SpacesTests {
    final ProjectSpaces spaces = bach.project().spaces();

    @Test
    void initSpaceIsEmpty() {
      assertTrue(spaces.init().modules().list().isEmpty());
    }

    @Test
    void mainSpaceContainsAggregatorModule() {
      assertEquals("aggregator", spaces.main().modules().names(","));
    }

    @Test
    void testSpaceIsEmpty() {
      assertTrue(spaces.test().modules().list().isEmpty());
    }
  }

  @Nested
  class Build {
    @Test
    void build() {
      assertDoesNotThrow(() -> bach.run("build"), bach.configuration().printer()::toString);
    }

    @Nested
    class Verify {

      final Paths paths = bach.configuration().paths();

      @Test
      void externalAssetsWereNotCached() {
        assertTrue(Files.notExists(paths.externalModules()));
        assertTrue(Files.notExists(paths.externalTools()));
      }

      @Test
      void modules() {
        var finder = ModuleFinder.of(paths.out("main", "modules"));
        var module = finder.find("aggregator").orElseThrow().descriptor();
        assertEquals("aggregator", module.name());
      }
    }
  }
}
