import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.project.DeclaredModule;
import com.github.sormuras.bach.project.ProjectSpace;

class build {
  public static void main(String... args) {
    Bach.build(build::project);
  }

  static Project project(Project project) {
    return project.withName("MultiReleaseMultiModule").withVersion("99").withMainSpace(build::main);
  }

  static ProjectSpace main(ProjectSpace main) {
    return main
        .withJavaRelease(8)
        .withModule("api/java-module/module-info.java", build::api)
        .withModule("engine/java-module/module-info.java", build::engine);
  }

  static DeclaredModule api(DeclaredModule module) {
    return module
        .withSources("api/java")
        .withSources("api/java-module")
        .withResources("api/java")
        .withResources("api/java-module");
  }

  static DeclaredModule engine(DeclaredModule module) {
    return module
        .withSources("engine/java")
        .withSources("engine/java-module")
        .withSources(11, "engine/java-11")
        .withResources("engine/java")
        .withResources("engine/java-module")
        .withResources(11, "engine/java-11");
  }
}
