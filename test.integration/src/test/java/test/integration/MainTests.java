package test.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Main;
import com.github.sormuras.bach.Printer;
import com.github.sormuras.bach.ToolCall;
import com.github.sormuras.bach.project.DeclaredModule;
import com.github.sormuras.bach.project.ExternalModuleLocator;
import com.github.sormuras.bach.project.ExternalModuleLocator.SormurasBachExternalModulesProperties;
import com.github.sormuras.bach.project.ExternalTool;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MainTests {
  @Test
  void allCommandLineArgumentsAreParsedCorrectlyIntoTheirModels(@TempDir Path temp) {
    var call =
        ToolCall.of("bach")
            // global configuration
            .with("--verbose")
            .with("--dry-run")
            // basic properties
            .with("--root-directory", temp)
            .with("--output-directory", temp.resolve("target"))
            // project model
            .with("--project-name", "NAME")
            .with("--project-version", "1.2.3")
            // ...
            .with("--project-with-external-modules", "junit@5.8.2")
            .with("--project-with-external-modules", "NAME@VERSION:OS:ARCH")
            // initial tool call
            .with("TOOL-NAME")
            .with("--more-arguments")
            .with(Stream.of(7, '8', 0x9));

    var bach = bach(call);
    var configuration = bach.configuration();
    assertTrue(configuration.isVerbose());
    assertTrue(configuration.isDryRun());

    var paths = configuration.paths();
    assertEquals(temp, paths.root());
    assertEquals(temp.resolve("target"), paths.out());
    assertEquals(temp.resolve(".bach/external-modules"), paths.externalModules());
    assertEquals(temp.resolve(".bach/external-tools"), paths.externalTools());

    var project = bach.project();
    assertEquals("NAME", project.name().value());
    assertEquals("1.2.3", project.version().value());
    assertEquals(
        List.of(
            SormurasBachExternalModulesProperties.of("junit", "5.8.2").caption(),
            SormurasBachExternalModulesProperties.of("NAME", "VERSION", "OS", "ARCH").caption()),
        project.externals().locators().list().stream()
            .map(ExternalModuleLocator::caption)
            .toList());
  }

  @Test
  void modules() {
    var project = bach().project();
    assertLinesMatch(
        """
        com.github.sormuras.bach
        com.github.sormuras.bach
        test.base
        test.integration
        test.workflow
        """
            .lines(),
        project.modules().stream().map(DeclaredModule::name).sorted());
    assertEquals(
        Set.of("com.github.sormuras.hello", "org.junit.platform.console"),
        project.externals().requires());
    assertLinesMatch(
        """
        format@.+""".lines(),
        project.externals().tools().stream().map(ExternalTool::name).sorted());
  }

  @Test
  void info() {
    var bach = bach();
    bach.run("info");
    assertLinesMatch(
        """
        info
        >>>>
        Tools
        >>>>
                    bach -> com.github.sormuras.bach/bach [Main]
        >> ... >>
        """
            .lines(),
        bach.configuration().printer().out().toString().lines(),
        bach.configuration().printer().toString());
  }

  static Bach bach(String... args) {
    return bach(ToolCall.of("bach").with(Stream.of(args)));
  }

  static Bach bach(ToolCall call) {
    return Main.bach(Printer.ofSilence(), call.arguments().toArray(String[]::new));
  }
}
