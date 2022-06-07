package test.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.sormuras.bach.project.DeclaredFolders;
import com.github.sormuras.bach.project.DeclaredModule;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DeclaredModuleTests {

  static final Path PROJECTS = Path.of("test.workflow/example-projects");

  @Test
  void checkAggregator() {
    var project = PROJECTS.resolve("aggregator");

    assertEquals(
        new DeclaredModule(
            project,
            project.resolve("module-info.java"),
            ModuleDescriptor.newModule("aggregator")
                .requires("java.se")
                .requires("jdk.management")
                .build(),
            DeclaredFolders.of(project),
            Map.of()),
        DeclaredModule.of(project, project.resolve("module-info.java")));
  }

  @Test
  void checkHello() {
    var project = PROJECTS.resolve("hello");

    assertEquals(
        new DeclaredModule(
            project,
            project.resolve("module-info.java"),
            ModuleDescriptor.newModule("hello").build(),
            DeclaredFolders.of(project),
            Map.of()),
        DeclaredModule.of(project, project.resolve("module-info.java")));
  }

  @Test
  void checkHelloWorld() {
    var project = PROJECTS.resolve("hello-world");

    assertEquals(
        new DeclaredModule(
            project.resolve("hello"),
            project.resolve("hello/module-info.java"),
            ModuleDescriptor.newModule("hello").requires("world").build(),
            DeclaredFolders.of(project.resolve("hello")),
            Map.of()),
        DeclaredModule.of(project.resolve("hello"), project.resolve("hello/module-info.java")));

    assertEquals(
        new DeclaredModule(
            project.resolve("world"),
            project.resolve("world/module-info.java"),
            ModuleDescriptor.newModule("world").build(),
            DeclaredFolders.of(project.resolve("world")),
            Map.of()),
        DeclaredModule.of(project.resolve("world"), project.resolve("world/module-info.java")));
  }

  @Test
  void checkMultiRelease() {
    var project = PROJECTS.resolve("multi-release");
    var conent = project.resolve("foo");

    assertEquals(
        new DeclaredModule(
            conent,
            conent.resolve("java/module-info.java"),
            ModuleDescriptor.newModule("foo").build(),
            DeclaredFolders.of(conent.resolve("java")),
            Map.of(
                11, DeclaredFolders.of(conent.resolve("java-11")),
                17, DeclaredFolders.of(conent.resolve("java-17")))),
        DeclaredModule.of(project, conent.resolve("java/module-info.java")));
  }

  @Test
  void checkMultiReleaseWithResources() {
    var project = PROJECTS.resolve("multi-release-with-resources");
    var content = project.resolve("foo");
    var main = content.resolve("main");

    assertEquals(
        new DeclaredModule(
            content,
            main.resolve("java/module-info.java"),
            ModuleDescriptor.newModule("foo").build(),
            DeclaredFolders.of(main.resolve("java")).withResourcePath(main.resolve("resources")),
            Map.of(
                11,
                DeclaredFolders.of()
                    .withSourcePath(main.resolve("java-11"))
                    .withResourcePath(main.resolve("resources-11")),
                13,
                DeclaredFolders.of().withResourcePath(main.resolve("resources-13")),
                15,
                DeclaredFolders.of().withSourcePath(main.resolve("java-15")),
                17,
                DeclaredFolders.of().withSourcePath(main.resolve("java-17")))),
        DeclaredModule.of(project, main.resolve("java/module-info.java")));
  }
}
