import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.Settings;
import com.github.sormuras.bach.call.JavacCall;
import com.github.sormuras.bach.project.PatchMode;
import com.github.sormuras.bach.workflow.CompileWorkflow;
import java.nio.file.Path;
import java.util.List;

class build {
  public static void main(String... args) {
    try {
      bach("17-ea").build();
    } catch (Throwable cause) {
      if (cause instanceof Error) throw cause;
      throw new Error("Caught unhandled throwable", cause);
    }
  }

  static MyBach bach(String projectVersion) {
    return new MyBach(project(projectVersion), settings());
  }

  static Project project(String projectVersion) {
    return Project.newProject("bach", projectVersion)
        .assertJDK(version -> version.feature() >= 16, "JDK 16+ is required")
        .assertJDK(Runtime.version().feature())
        .withName("bach")
        .withVersion(projectVersion)
        .withDefaultSourceFileEncoding("UTF-8")
        .withMainProjectSpace(
            main ->
                main.withJavaRelease(16)
                    .withModuleSourcePaths("./*/main/java")
                    .withModule("com.github.sormuras.bach/main/java/module-info.java"))
        .withTestProjectSpace(
            test ->
                test.withModule(
                        "com.github.sormuras.bach/test/java-module/module-info.java",
                        "com.github.sormuras.bach/test/java")
                    .withModule("test.base/test/java/module-info.java")
                    .withModule("test.integration/test/java/module-info.java")
                    .withModule("test.projects/test/java/module-info.java")
                    .withModulePaths(".bach/workspace/modules", ".bach/external-modules")
                    .with(PatchMode.SOURCES)
                    .withPatchModule(
                        "com.github.sormuras.bach", "com.github.sormuras.bach/main/java"));
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
      super.build();
      logbook.out().println("| END.");
    }

    @Override
    public void compileMainSpace() {
      new CompileWorkflow(this, project.spaces().main()) {
        @Override
        public JavacCall generateJavacCall(List<String> modules, Path classes) {
          return super.generateJavacCall(modules, classes)
              .with("-g")
              .with("-parameters")
              .with("-Werror")
              .with("-Xlint");
        }
      }.execute();
    }
  }
}
