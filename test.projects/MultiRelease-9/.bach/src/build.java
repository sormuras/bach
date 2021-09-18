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
        .withName("MultiRelease-9")
        .withVersion("9")
        .withSpaces(spaces -> spaces.withSpace("main", build::main));
  }

  static ProjectSpace main(ProjectSpace main) {
    return main.withRelease(9).withModule("foo/main/java/module-info.java", build::foo);
  }

  static DeclaredModule foo(DeclaredModule module) {
    return module
        .withSourcesFolder("foo/main/java")
        .withSourcesFolder("foo/main/java-11", 11)
        .withSourcesFolder("foo/main/java-15", 15)
        .withResourcesFolder("foo/main/java")
        .withResourcesFolder("foo/main/java-11", 11)
        .withResourcesFolder("foo/main/java-15", 15)
        .withResourcesFolder("foo/main/resources")
        .withResourcesFolder("foo/main/resources-11", 11)
        .withResourcesFolder("foo/main/resources-13", 13);
  }
}
