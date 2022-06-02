package test.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.sormuras.bach.Main;
import com.github.sormuras.bach.Printer;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.ToolCall;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProjectInitializationTests {

  @Test
  void initializeOneModulePerSpace(@TempDir Path temp) {
    Project.ofDefaults()
        .withName("initializeOneModulePerSpace")
        .withVersion("99")
        .withModule("init", "processor")
        .withModule("org.example.application")
        .withModule("test", "test.integration")
        .initializeInDirectory(temp);

    var bach =
        Main.bach(
            Printer.ofSilence(),
            ToolCall.of("bach")
                .with("--verbose")
                .with("--root-directory", temp)
                .arguments()
                .toArray(String[]::new));
    assertEquals("initializeOneModulePerSpace", bach.project().name().value());
    assertEquals("99", bach.project().version().value());
    bach.run("build");

    var paths = bach.configuration().paths();
    assertModuleExists("processor", paths.out("init", "modules"));
    assertModuleExists("org.example.application", paths.out("main", "modules"));
    assertModuleExists("test.integration", paths.out("test", "modules"));
  }

  static void assertModuleExists(String module, Path... paths) {
    assertEquals(module, ModuleFinder.of(paths).find(module).orElseThrow().descriptor().name());
  }
}
