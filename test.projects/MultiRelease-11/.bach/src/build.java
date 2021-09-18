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
        .withName("MultiRelease-11")
        .withVersion("11")
        .withSpaces(spaces -> spaces.withSpace("main", build::main));
  }

  static ProjectSpace main(ProjectSpace main) {
    return main.withRelease(11).withModule("foo/main/java/module-info.java", build::foo);
  }

  static DeclaredModule foo(DeclaredModule module) {
    return module
        .withSourcesFolder("foo/main/java")
        .withSourcesFolder("foo/main/java-12", 12)
        .withSourcesFolder("foo/main/java-13", 13)
        .withSourcesFolder("foo/main/java-14", 14)
        .withSourcesFolder("foo/main/java-15", 15)
        .withSourcesFolder("foo/main/java-16", 16)
        .withResourcesFolder("foo/main/java")
        .withResourcesFolder("foo/main/java-12", 12)
        .withResourcesFolder("foo/main/java-13", 13)
        .withResourcesFolder("foo/main/java-14", 14)
        .withResourcesFolder("foo/main/java-15", 15)
        .withResourcesFolder("foo/main/java-16", 16);
  }
}
