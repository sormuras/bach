import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.project.DeclaredModule;
import com.github.sormuras.bach.project.ProjectSpace;

class build {
  public static void main(String... args) {
    Bach.build(build::project);
  }

  static Project project(Project project) {
    return project.withName("MultiRelease-11").withVersion("11").withMainProjectSpace(build::main);
  }

  static ProjectSpace main(ProjectSpace main) {
    return main.withJavaRelease(11).withModule("foo/main/java/module-info.java", build::foo);
  }

  static DeclaredModule foo(DeclaredModule module) {
    return module
        .withSources("foo/main/java")
        .withSources(12, "foo/main/java-12")
        .withSources(13, "foo/main/java-13")
        .withSources(14, "foo/main/java-14")
        .withSources(15, "foo/main/java-15")
        .withSources(16, "foo/main/java-16")
        .withResources("foo/main/java")
        .withResources(12, "foo/main/java-12")
        .withResources(13, "foo/main/java-13")
        .withResources(14, "foo/main/java-14")
        .withResources(15, "foo/main/java-15")
        .withResources(16, "foo/main/java-16");
  }
}
