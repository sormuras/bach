import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.project.DeclaredModule;
import com.github.sormuras.bach.project.ProjectSpace;

class build {
  public static void main(String... args) {
    Bach.build(build::project);
  }

  static Project project(Project project) {
    return project.withName("MultiRelease-8").withVersion("8").withMainSpace(build::main);
  }

  static ProjectSpace main(ProjectSpace main) {
    return main.withJavaRelease(8).withModule("foo/main/java-module/module-info.java", build::foo);
  }

  static DeclaredModule foo(DeclaredModule module) {
    return module
        .withSources("foo/main/java")
        .withSources("foo/main/java-module")
        .withSources(9, "foo/main/java-9")
        .withSources(10, "foo/main/java-10")
        .withResources("foo/main/java")
        .withResources("foo/main/java-module")
        .withResources(9, "foo/main/java-9")
        .withResources(10, "foo/main/java-10");
  }
}
