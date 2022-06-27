package test.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Printer;
import com.github.sormuras.bach.project.DeclaredModule;
import com.github.sormuras.bach.project.ExternalModuleLocator;
import com.github.sormuras.bach.project.ExternalModuleLocator.SingleExternalModuleLocator;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MainTests {
  @Test
  void allCommandLineArgumentsAreParsedCorrectlyIntoTheirModels(@TempDir Path temp) {
    var bach =
        Bach.of(
            Printer.ofSilence(),
            args ->
                args
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
                    .with("--project-with-external-module", "org.example.foo@https//foo.jar")
                    .with("--project-with-external-module", "org.example.bar@https//bar.jar")
                    // initial tool call
                    .with("TOOL-NAME")
                    .with("--more-arguments")
                    .with(Stream.of(7, '8', 0x9)));

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
        Stream.of(
                new SingleExternalModuleLocator("org.example.foo", "https//foo.jar"),
                new SingleExternalModuleLocator("org.example.bar", "https//bar.jar"))
            .map(ExternalModuleLocator::caption)
            .toList(),
        project.externals().locators().list().stream()
            .map(ExternalModuleLocator::caption)
            .toList());
  }

  @Test
  void modules() {
    var project = Bach.of(Printer.ofSilence()).project();
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
        format@.+
        jreleaser@.+
        """.lines(),
        project.externals().tools().stream().sorted());
    assertLinesMatch(
        """
        java.base
        jdk.compiler
        jdk.jartool
        jdk.javadoc
        jdk.jdeps
        jdk.jfr
        jdk.jlink
        org.junit.jupiter
        test.base
        """
            .lines(),
        project
            .spaces()
            .test()
            .modules()
            .toModuleFinder()
            .find("com.github.sormuras.bach")
            .orElseThrow()
            .descriptor()
            .requires()
            .stream()
            .map(ModuleDescriptor.Requires::name)
            .sorted());

    var locators = project.externals().locators();
    assertNotNull(locators.locate("org.apiguardian.api"));
    assertNotNull(locators.locate("org.junit.jupiter"));
    assertNotNull(locators.locate("org.junit.platform.commons"));
    assertNotNull(locators.locate("org.opentest4j"));
  }

  @Test
  void info() {
    var bach = Bach.of(Printer.ofSilence());
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
}
