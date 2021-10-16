import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.project.DeclaredModule;
import com.github.sormuras.bach.project.ProjectSpace;

/**
 * Build this project.
 *
 * <pre>
 * foo
 * foo/main
 * foo/main/java
 * foo/main/java/foo
 * foo/main/java/foo/Foo.java
 * foo/main/java/module-info.java
 * foo/main/java-11
 * foo/main/java-11/foo
 * foo/main/java-11/foo/Foo.java
 * foo/main/java-15
 * foo/main/java-15/foo
 * foo/main/java-15/foo/Foo.java
 * foo/main/java-17
 * foo/main/java-17/foo
 * foo/main/java-17/foo/Foo.java
 * foo/main/java-17/module-info.java
 * foo/main/resources
 * foo/main/resources/foo
 * foo/main/resources/foo/Foo.txt
 * foo/main/resources-11
 * foo/main/resources-11/foo
 * foo/main/resources-11/foo/Foo.txt
 * foo/main/resources-13
 * foo/main/resources-13/foo
 * foo/main/resources-13/foo/Foo.txt
 * README.md
 * </pre>
 */
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
        .withSourcesFolder("foo/main/java-17", 17)
        .withResourcesFolder("foo/main/java")
        .withResourcesFolder("foo/main/java-11", 11)
        .withResourcesFolder("foo/main/java-15", 15)
        .withResourcesFolder("foo/main/resources")
        .withResourcesFolder("foo/main/resources-11", 11)
        .withResourcesFolder("foo/main/resources-13", 13);
  }
}
