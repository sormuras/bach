import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.Settings;
import com.github.sormuras.bach.call.JavacCall;
import com.github.sormuras.bach.external.JUnit;
import com.github.sormuras.bach.project.PatchMode;
import com.github.sormuras.bach.workflow.CompileWorkflow;
import java.nio.file.Path;
import java.util.List;

class build {
  public static void main(String... args) {
    Bach.build(bach("17-ea"));
  }

  static MyBach bach(String projectVersion) {
    return new MyBach(project(projectVersion), settings());
  }

  static Project project(String projectVersion) {
    return Project.of("bach", projectVersion)
        .assertJDK(version -> version.feature() >= 16, "JDK 16+ is required")
        .assertJDK(Runtime.version().feature())
        .withName("bach")
        .withVersion(projectVersion)
        .withDefaultSourceFileEncoding("UTF-8")
        .withMainSpace(
            main ->
                main.withJavaRelease(16)
                    .withModuleSourcePaths("./*/main/java")
                    .withModule(
                        "com.github.sormuras.bach/main/java/module-info.java",
                        module -> module.withResources("com.github.sormuras.bach/main/java")))
        .withTestSpace(
            test ->
                test.withModule("test.base/test/java/module-info.java")
                    .withModule("test.integration/test/java/module-info.java")
                    .withModule("test.projects/test/java/module-info.java")
                    .withModule(
                        "com.github.sormuras.bach/test/java-module/module-info.java",
                        module -> module.withSources("com.github.sormuras.bach/test/java"))
                    .with(PatchMode.SOURCES)
                    .withPatchModule(
                        "com.github.sormuras.bach", "com.github.sormuras.bach/main/java")
                    .withModulePaths(".bach/workspace/modules", ".bach/external-modules"))
        .withRequiresExternalModules("org.junit.platform.console")
        .withExternalModuleLocators(JUnit.V_5_8_0_M1);
  }

  static Settings settings() {
    return Settings.of().withBrowserConnectTimeout(9);
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
