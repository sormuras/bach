import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.project.DeclaredModule;
import com.github.sormuras.bach.project.ProjectSpace;

class build {
  public static void main(String... args) {
    Bach.build(build::project);
  }

  static Project project(Project project) {
    return project
        .withName("MultiReleaseMultiModule")
        .withVersion("99")
        .withSpaces(spaces -> spaces.withSpace("main", build::main));
  }

  static ProjectSpace main(ProjectSpace main) {
    return main.withRelease(8)
        .withModule("api/java-module/module-info.java", build::api)
        .withModule("engine/java-module/module-info.java", build::engine);
  }

  static DeclaredModule api(DeclaredModule module) {
    return module
        .withSourcesFolder("api/java")
        .withSourcesFolder("api/java-module")
        .withResourcesFolder("api/java")
        .withResourcesFolder("api/java-module");
  }

  static DeclaredModule engine(DeclaredModule module) {
    return module
        .withSourcesFolder("engine/java")
        .withSourcesFolder("engine/java-module")
        .withSourcesFolder("engine/java-11", 11)
        .withResourcesFolder("engine/java")
        .withResourcesFolder("engine/java-module")
        .withResourcesFolder("engine/java-11", 11);
  }
}
