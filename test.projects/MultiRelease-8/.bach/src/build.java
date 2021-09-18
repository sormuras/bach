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
        .withName("MultiRelease-8")
        .withVersion("8")
        .withSpaces(spaces -> spaces.withSpace("main", build::main));
  }

  static ProjectSpace main(ProjectSpace main) {
    return main.withRelease(8).withModule("foo/main/java-module/module-info.java", build::foo);
  }

  static DeclaredModule foo(DeclaredModule module) {
    return module
        .withSourcesFolder("foo/main/java")
        .withSourcesFolder("foo/main/java-module")
        .withSourcesFolder("foo/main/java-9", 9)
        .withSourcesFolder("foo/main/java-10", 10)
        .withResourcesFolder("foo/main/java")
        .withResourcesFolder("foo/main/java-module")
        .withResourcesFolder("foo/main/java-9", 9)
        .withResourcesFolder("foo/main/java-10", 10);
  }
}
