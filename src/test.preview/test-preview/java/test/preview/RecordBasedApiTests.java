package test.preview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.module.ModuleDescriptor.Version;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

public class RecordBasedApiTests {

  @Test
  void check() {
    assertTitle("<untitled>", Project.newProject());
    assertTitle("Title", Project.newProject().title("Title"));
    assertVersion("47.11", Project.newProject().version("47.11"));
    assertFalse(Project
        .newProject()
        .requires("foo", "bar")
        .library()
        .requires()
        .isEmpty());
  }

  static void assertTitle(String expectedTitle, Project project) {
    assertEquals(expectedTitle, project.title());
  }

  static void assertVersion(String expectedVersion, Project project) {
    assertEquals(Version.parse(expectedVersion), project.version());
  }

  record Project(String title, Version version, Library library)  {

    static Project newProject() {
      return new Project("<untitled>", Version.parse("1-ea"), Library.newLibrary());
    }

    public Project title(String title) {
      return new Project(title, version, library);
    }
    public Project version(String version) {
      return new Project(title, Version.parse(version), library);
    }
    public Project requires(String module, String... more) {
      return new Project(title, version, library.requires(module, more));
    }
  }

  record Library(Set<String> requires)  {

    public static Library newLibrary() {
      return new Library(Set.of());
    }

    public Library requires(String module, String... more) {
      var set = new TreeSet<>(requires);
      set.add(module);
      if (more.length > 0) set.addAll(Set.of(more));
      return new Library(set);
    }
  }
}
