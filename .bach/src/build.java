import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Call;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.Settings;
import com.github.sormuras.bach.Tweak;
import com.github.sormuras.bach.call.CompileMainSpaceJavacCall;
import com.github.sormuras.bach.external.ExternalModuleLocation;
import com.github.sormuras.bach.external.JUnit;
import com.github.sormuras.bach.external.Maven;
import com.github.sormuras.bach.project.PatchMode;
import com.github.sormuras.bach.workflow.BuildWorkflow;
import com.github.sormuras.bach.Checkpoint;
import java.util.Optional;

class build {
  public static void main(String... args) {
    System.setProperty("java.util.logging.config.file", ".bach/src/logging.properties");
    Bach.build(bach(args));
  }

  static MyBach bach(String... args) {
    var options = Options.of(args);
    return new MyBach(project(options), settings(options));
  }

  static Project project(Options options) {
    return Project.of("bach", "17-ea")
        .assertJDK(version -> version.feature() >= 16, "JDK 16+ is required")
        .assertJDK(Runtime.version().feature())
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
                    .withModule(
                        "test.projects/test/java/module-info.java",
                        module -> module.withResources("test.projects/test/resources"))
                    .withModule(
                        "com.github.sormuras.bach/test/java-module/module-info.java",
                        module -> module.withSources("com.github.sormuras.bach/test/java"))
                    .with(PatchMode.SOURCES)
                    .withPatchModule(
                        "com.github.sormuras.bach", "com.github.sormuras.bach/main/java")
                    .withModulePaths(".bach/workspace/modules", ".bach/external-modules"))
        .withRequiresExternalModules("org.junit.platform.console", "org.junit.platform.jfr")
        .withExternalModuleLocators(JUnit.V_5_8_0_M1, build::locate)
        .with(options);
  }

  static Optional<ExternalModuleLocation> locate(String module) {
    var central =
        switch (module) {
          case "junit" -> Maven.central("junit", "junit", "4.13.2");
          case "org.hamcrest" -> Maven.central("org.hamcrest", "hamcrest", "2.2");
          default -> null;
        };
    return Optional.ofNullable(central).map(uri -> new ExternalModuleLocation(module, uri));
  }

  static Settings settings(Options options) {
    return Settings.of()
        .withBrowserConnectTimeout(9)
        .withWorkflowCheckpointHandler(build::smile)
        .withWorkflowTweakHandler(build::tweak)
        .with(options);
  }

  static void smile(Checkpoint checkpoint) {
    if (checkpoint instanceof BuildWorkflow.ErrorCheckpoint) {
      System.err.println(")-:");
      return;
    }
    System.out.printf("(-: %s%n", checkpoint.getClass());
  }

  static Call tweak(Tweak tweak) {
    if (tweak.call() instanceof CompileMainSpaceJavacCall javac) {
      return javac.with("-g").with("-parameters").with("-Werror").with("-Xlint");
    }
    return tweak.call();
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
  }
}
