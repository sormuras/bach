package test.preview;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.module.ModuleDescriptor.Version;
import org.junit.jupiter.api.Test;

public class RecordBasedApiTests {

  @Test
  void check() {
    assertEquals("1-ea",
        Project.of().version().toString());

    assertEquals("javac --module-version 1-ea",
        Project.of().getJavacForMainSources());

    assertEquals("javac --module-version 17.01",
        Project.of().with(Version.parse("17.01")).getJavacForMainSources());

    assertEquals("javac --module-version 47.11",
        Project.of().with(new MainSources("javac --module-version 47.11")).getJavacForMainSources());
  }

  record Project(Version version, MainSources main) {

    public static Project of() {
      return new Project(Version.parse("1-ea"), new MainSources(""));
    }

    public Project with(Version version) {
      return new Project(version, main);
    }

    public Project with(MainSources main) {
      return new Project(version, main);
    }

    public String getJavacForMainSources() {
      var javac = main().customJavac();
      return javac.isEmpty() ? "javac --module-version " + version() : javac;
    }
  }

  record MainSources(String customJavac) {}

}
