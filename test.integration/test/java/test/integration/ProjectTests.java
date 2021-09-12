package test.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.external.JUnit;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProjectTests {

  @Test
  void withAll(@TempDir Path temp) throws Exception {
    var fooFile =
        Files.writeString(
            Files.createDirectories(temp.resolve("foo")).resolve("module-info.java"),
            """
            module foo {
              requires java.base;
            }
            """);
    var barFile =
        Files.writeString(
            Files.createDirectories(temp.resolve("bar")).resolve("module-info.java"),
            """
            open module bar {
              requires foo;
              requires org.junit.jupiter;
            }
            """);

    var project =
        Project.of("initial-name", "0-initial+version")
            .withName("custom-name")
            .withVersion("1-custom+version")
            .withSpaces(
                spaces ->
                    spaces
                        .withSpace(
                            "main",
                            main ->
                                main.withModule(
                                    fooFile,
                                    foo ->
                                        foo.withMainClass("foo.Main")
                                            .withResourcesFolder("resources")
                                            .withResourcesFolder("res-10", 10)
                                            .withResourcesFolder("res-11", 11)))
                        .withSpace(
                            "test",
                            Set.of("main"),
                            test ->
                                test.withModule(
                                    barFile,
                                    bar ->
                                        bar.withMainClass("bar.App")
                                            .withSourcesFolder("java-module")
                                            .withResourcesFolder("resources")
                                            .withResourcesFolder("res-12", 12)
                                            .withResourcesFolder("res-13", 13))))
            .withExternals(
                externals ->
                    externals
                        .withRequiresModule("org.junit.platform.console")
                        .withExternalModuleLocator(JUnit.version("5.7.2")))
            .with(Options.parse("--project-name", "Name", "--project-version", "99"));

    assertEquals("Name 99", project.toNameAndVersion());

    var mainSpace = project.space("main");
    var testSpace = project.space("test");

    assertSame(mainSpace, project.spaces().values().get(0));
    assertSame(mainSpace, testSpace.parents().get(0)); // test depends-on main

    var fooModule = mainSpace.module("foo");
    assertFalse(fooModule.descriptor().isOpen());
    assertEquals("foo", fooModule.name());
    assertTrue(fooModule.descriptor().requires().toString().contains("java.base"));
    assertEquals("foo.Main", fooModule.mainClass().orElseThrow());

    var barModule = testSpace.module("bar");
    assertTrue(barModule.descriptor().isOpen());
    assertEquals("bar", barModule.name());
    assertTrue(barModule.descriptor().requires().toString().contains("foo"));
    assertTrue(barModule.descriptor().requires().toString().contains("org.junit.jupiter"));
    assertEquals("bar.App", barModule.mainClass().orElseThrow());

    assertEquals(Set.of("org.junit.platform.console"), project.externals().requires());
    assertNotNull(project.externals().locators().values().get(0).locate("org.junit.jupiter"));

    assertEquals("bar.App", project.space("test").module("bar").mainClass().orElseThrow());
    var tweaked = project.withModuleTweak("test", "bar", bar -> bar.withMainClass("bar.Main"));
    assertEquals("bar.Main", tweaked.space("test").module("bar").mainClass().orElseThrow());
  }
}
