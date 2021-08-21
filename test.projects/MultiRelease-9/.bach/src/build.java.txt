import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.project.DeclaredModule;
import com.github.sormuras.bach.project.ProjectSpace;

class build {
  public static void main(String... args) {
    Bach.build(build::project);
  }

  static Project project(Project project) {
    return project.withName("MultiRelease-9").withVersion("9").withMainSpace(build::main);
  }

  static ProjectSpace main(ProjectSpace main) {
    return main.withJavaRelease(9).withModule("foo/main/java/module-info.java", build::foo);
  }

  static DeclaredModule foo(DeclaredModule module) {
    return module
        .withSources("foo/main/java")
        .withSources(11, "foo/main/java-11")
        .withSources(15, "foo/main/java-15")
        .withResources("foo/main/java")
        .withResources(11, "foo/main/java-11")
        .withResources(15, "foo/main/java-15")
        .withResources("foo/main/resources")
        .withResources(11, "foo/main/resources-11")
        .withResources(13, "foo/main/resources-13");
  }
}
