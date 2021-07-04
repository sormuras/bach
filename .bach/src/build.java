import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.Settings;
import com.github.sormuras.bach.call.JarCall;
import com.github.sormuras.bach.call.JavacCall;
import com.github.sormuras.bach.workflow.CompileMainModulesWorkflow;
import com.github.sormuras.bach.workflow.CompileTestModulesWorkflow;
import java.lang.module.ModuleDescriptor;
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
        .withMainJavaRelease(16)
        .withMainModuleSourcePaths("./*/main/java")
        .withMainModule("com.github.sormuras.bach/main/java/module-info.java")
        .withTestModuleSourcePaths("./*/test/java", "./*/test/java-module")
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

    @Override
    public void compileMainModules() {
      new CompileMainModulesWorkflow(this) {
        @Override
        public JavacCall generateJavacCall(List<String> modules, Path classes) {
          return super.generateJavacCall(modules, classes)
              .with("-g")
              .with("-parameters")
              .with("-Werror")
              .with("-Xlint")
              .with("-encoding", "UTF-8");
        }
      }.execute();
    }

    @Override
    public void compileTestModules() {
      new CompileTestModulesWorkflow(this) {
        final Path patch = Path.of(".bach/workspace/classes-main-16/com.github.sormuras.bach");

        @Override
        public JavacCall generateJavacCall(List<String> modules, Path classes) {
          return super.generateJavacCall(modules, classes)
              .with("--patch-module", "com.github.sormuras.bach=" + patch)
              .with(
                  "--module-path",
                  List.of(Path.of(".bach/workspace/modules"), Path.of(".bach/external-modules")))
              .with("-encoding", "UTF-8");
        }

        @Override
        public JarCall generateJarCall(ModuleDescriptor module, Path classes, Path destination) {
          return super.generateJarCall(module, classes, destination).with("-C", patch, ".");
        }
      }.execute();
    }
  }
}
