import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Builder;
import com.github.sormuras.bach.Folders;
import com.github.sormuras.bach.Logbook;
import com.github.sormuras.bach.Project;

class build {
  public static void main(String... args) {
    try {
      project("17-ea").build(bach());
    } catch (Exception exception) {
      System.exit(-1);
    }
  }

  static Bach bach() {
    return Bach.configureBach()
        .with(Logbook.ofSystem())
        .with(Folders.of(""))
        .with(MyBuilder::new);
  }

  static Project project(String version) {
    return Project.configureProject("bach", version)
        .withMainModule("com.github.sormuras.bach/main/java/module-info.java")
        .withTestModule("com.github.sormuras.bach/test/java-module/module-info.java")
        .withTestModule("test.base/test/java/module-info.java")
        .withTestModule("test.integration/test/java/module-info.java")
        .withTestModule("test.projects/test/java/module-info.java");
  }

  static class MyBuilder extends Builder {

    MyBuilder(Bach bach, Project project) {
      super(bach, project);
    }

    @Override
    public void build() {
      System.out.println("BEGIN");
      System.out.printf("%s%n", Project.toString(project));
      super.build();
      System.out.println("END.");
    }
  }
}
