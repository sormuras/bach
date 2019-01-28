import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ProjectTests {

  private final Path dependencies = Path.of("dependencies");
  private final Path target = Path.of("target");
  private final Path mainDestination = target.resolve(Path.of("main", "mods"));
  private final Path testDestination = target.resolve(Path.of("test", "mods"));

  @Test
  void creatingGroupWithSameNameFails() {
    Exception e =
        assertThrows(
            IllegalArgumentException.class,
            () -> Bach.Project.builder().groupBuilder("name").buildGroup().groupBuilder("name"));
    assertEquals("name already defined", e.getMessage());
  }

  @Test
  void defaults() {
    var project = Bach.Project.builder().buildProject();
    assertEquals("bach", project.name());
    assertEquals("1.0.0-SNAPSHOT", project.version());
    assertTrue(project.target().endsWith(Path.of("target", "bach")));
    assertEquals(0, project.groups().size());
    assertThrows(NoSuchElementException.class, () -> project.group("main"));
    assertThrows(NoSuchElementException.class, () -> project.group("test"));
  }

  @Test
  void manual() {
    var project =
        Bach.Project.builder()
            .name("Manual")
            .version("II")
            .target(target)
            // main
            .groupBuilder("main")
            .destination(mainDestination)
            .moduleSourcePath(List.of(Path.of("src", "main", "java")))
            .mainClass(Map.of("foo", "foo.Main"))
            .buildGroup()
            // test
            .groupBuilder("test")
            .destination(testDestination)
            .moduleSourcePath(List.of(Path.of("src", "test", "java")))
            .modulePath(List.of(mainDestination, dependencies))
            .patchModule(Map.of("hello", List.of(Path.of("src/main/java/hello"))))
            .buildGroup()
            // done
            .buildProject();
    assertEquals("Manual", project.name());
    assertEquals("II", project.version());
    assertEquals(Path.of("target"), project.target());
    assertEquals("main", project.group("main").name());
    assertEquals("test", project.group("test").name());
    assertEquals(2, project.groups().size());

    var main = project.group("main");
    assertEquals("main", main.name());
    assertEquals(mainDestination, main.destination());
    assertEquals("foo.Main", main.mainClass().get("foo"));
    assertEquals(List.of(Path.of("src", "main", "java")), main.moduleSourcePath());
    assertTrue(main.modulePath().isEmpty());
    assertTrue(main.patchModule().isEmpty());

    var test = project.group("test");
    assertEquals("test", test.name());
    assertEquals(testDestination, test.destination());
    assertTrue(test.mainClass().isEmpty());
    assertEquals(List.of(Path.of("src", "test", "java")), test.moduleSourcePath());
    assertEquals(List.of(mainDestination, dependencies), test.modulePath());
    assertEquals(List.of(Path.of("src/main/java/hello")), test.patchModule().get("hello"));
  }

  @Test
  void demoJigsawQuickStartGreetings() {
    var root = Path.of("demo", "04-jigsaw-quick-start", "greetings");
    var expected =
        Bach.Project.builder()
            .name("greetings")
            .version("1.0.0-SNAPSHOT")
            .target(root.resolve(Path.of("target", "bach")))
            .groupBuilder("src")
            .destination(Path.of("target", "bach", "modules"))
            .moduleSourcePath(List.of(Path.of("src")))
            .buildGroup()
            .buildProject();

    var actual = Bach.Project.of(root, Path.of("src"));
    assertEquals(expected.name(), actual.name());
    assertEquals(expected.version(), actual.version());
    assertTrue(actual.target().endsWith(expected.target()));
    assertEquals(expected.groups().size(), actual.groups().size());
    var src = actual.group("src");
    assertEquals("src", src.name());
    assertTrue(src.destination().endsWith(Path.of("target", "bach", "modules")));
    assertEquals(List.of(Path.of("src")), src.moduleSourcePath());
    assertTrue(src.modulePath().isEmpty());
    assertTrue(src.patchModule().isEmpty());

    assertEquals(Set.of("com.greetings"), actual.modules());
  }

  @Test
  void demoJigsawQuickStartGreetingsWorld() {
    var expected =
        Bach.Project.builder()
            .name("greetings-world")
            .version("1.0.0-SNAPSHOT")
            .target(Path.of("target", "bach"))
            .groupBuilder("src")
            .destination(Path.of("target", "bach", "modules"))
            .moduleSourcePath(List.of(Path.of("src")))
            .buildGroup()
            .buildProject();

    var root = Path.of("demo", "04-jigsaw-quick-start", "greetings-world");
    var actual = Bach.Project.of(root, Path.of("src"));

    assertEquals(expected.name(), actual.name());
    assertEquals(expected.version(), actual.version());
    assertTrue(actual.target().endsWith(expected.target()));
    assertEquals(expected.groups().size(), actual.groups().size());

    var src = actual.group("src");
    assertEquals("src", src.name());
    assertTrue(src.destination().endsWith(Path.of("target", "bach", "modules")));
    assertEquals(List.of(Path.of("src")), src.moduleSourcePath());
    assertTrue(src.modulePath().isEmpty());
    assertTrue(src.patchModule().isEmpty());

    assertEquals(Set.of("com.greetings", "org.astro"), actual.modules());
  }

  @Test
  void demoJigsawQuickStartGreetingsWorldWithMainAndTest() {
    var expected =
        Bach.Project.builder()
            .name("greetings-world-with-main-and-test")
            .version("1.0.0-SNAPSHOT")
            .target(Path.of("target", "bach"))
            .groupBuilder("main")
            .destination(Path.of("target", "bach", "modules", "main"))
            .moduleSourcePath(List.of(Path.of("src", "main")))
            .buildGroup()
            .groupBuilder("test")
            .destination(Path.of("target", "bach", "modules", "test"))
            .moduleSourcePath(List.of(Path.of("src", "test")))
            .buildGroup()
            .buildProject();

    var root = Path.of("demo", "04-jigsaw-quick-start", "greetings-world-with-main-and-test");
    var actual = Bach.Project.of(root, Path.of("src", "main"), Path.of("src", "test"));

    assertEquals(expected.name(), actual.name());
    assertEquals(expected.version(), actual.version());
    assertTrue(actual.target().endsWith(expected.target()));
    assertEquals(expected.groups().size(), actual.groups().size());

    var main = actual.group("main");
    assertEquals("main", main.name());
    assertTrue(main.destination().endsWith(Path.of("target", "bach", "modules", "main")));
    assertEquals(List.of(Path.of("src", "main")), main.moduleSourcePath());
    assertTrue(main.modulePath().isEmpty());
    assertTrue(main.patchModule().isEmpty());

    assertEquals(Set.of("black.box", "com.greetings", "org.astro"), actual.modules());
  }
}
