package test.preview;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.module.ModuleDescriptor.Version;
import org.junit.jupiter.api.Test;

public class RecordBasedApiTests {

  @Test
  void check() {
    var project = Project.of();
    assertEquals("1-ea", project.version().toString());
    assertEquals(Runtime.version().feature(), project.main().release());

    project = project
        .with(Version.parse("17.01"))
        .with(new MainSources(8));

    assertEquals("17.01", project.version().toString());

    var builder = new Builder(project) {
      @Override
      public String computeJavac(MainSources main) {
        // generate and return custom javac or...
        return super.computeJavac(main).toUpperCase(); // tweak super
      }
    };

    builder.build();
  }

  record Project(Version version, MainSources main) {

    public static Project of() {
      return new Project(Version.parse("1-ea"), new MainSources(Runtime.version().feature()));
    }

    public Project with(Version version) {
      return new Project(version, main);
    }

    public Project with(MainSources main) {
      return new Project(version, main);
    }

    public Builder toBuilder() {
      return new Builder(this);
    }
  }

  record MainSources(int release) {}

  static class Builder {
    private final Project project;

    Builder(Project project) {
      this.project = project;
    }

    void build() {
      System.out.println(computeJavac(project.main()));
    }

    public String computeJavac(MainSources main) {
      return "javac --module-version " + project.version() + " --release " + main.release();
    }
  }
}
