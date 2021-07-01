import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.Settings;

class build {
  public static void main(String... args) {
    try {
      bach("17-ea").build();
    } catch (Throwable cause) {
      if (cause instanceof Error) throw cause;
      throw new Error("Caught unhandled throwable", cause);
    }
  }

  static Bach bach(String projectVersion) {
    return new MyBach(project(projectVersion), settings());
  }

  static Project project(String projectVersion) {
    return Project.newProject("bach", projectVersion)
        .assertJDK(version -> version.feature() >= 16, "JDK 16+ is required")
        .assertJDK(Runtime.version().feature())
        .withName("bach")
        .withVersion(projectVersion)
        .withCompileMainModulesForJavaRelease(16)
        .withMainModule("com.github.sormuras.bach/main/java/module-info.java")
        .withTestModule("com.github.sormuras.bach/test/java-module/module-info.java")
        .withTestModule("test.base/test/java/module-info.java")
        .withTestModule("test.integration/test/java/module-info.java")
        .withTestModule("test.projects/test/java/module-info.java");
  }

  static Settings settings() {
    return Settings.newSettings().withBrowserConnectTimeout(9);
  }

  static class MyBach extends Bach {

    MyBach(Project project, Settings settings) {
      super(project, settings);
    }

    @Override
    public void build() {
      logbook.out().println("| BEGIN");
      project.toTextBlock().lines().map(line -> "| " + line).forEach(logbook.out()::println);
      super.build();
      logbook.out().println("| END.");
    }
  }
}
