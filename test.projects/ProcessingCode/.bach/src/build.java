import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.Settings;
import com.github.sormuras.bach.call.AnyCall;
import com.github.sormuras.bach.call.DeleteDirectoriesCall;
import com.github.sormuras.bach.call.JavacCall;
import com.github.sormuras.bach.project.ProjectSpace;
import com.github.sormuras.bach.workflow.CompileWorkflow;
import java.nio.file.Path;
import java.util.List;

class build {
  public static void main(String... args) {
    Bach.build(new MyBach(project()));
  }

  static Project project() {
    return Project.of("ProcessingCode", "99")
        .withMainProjectSpace(build::main)
        .withTestProjectSpace(build::test);
  }

  static ProjectSpace main(ProjectSpace main) {
    return main.withModule("showcode/main/java/module-info.java");
  }

  static ProjectSpace test(ProjectSpace test) {
    return test.withModule("tests/test/java/module-info.java");
  }
}

class MyBach extends Bach {
  public MyBach(Project project) {
    super(project, Settings.of());
  }

  @Override
  public void compileMainSpace() {
    execute(new DeleteDirectoriesCall(folders.workspace()));
    super.compileMainSpace();
  }

  @Override
  public void compileTestSpace() {
    new CompileWorkflow(this, project.spaces().test()) {
      @Override
      public JavacCall generateJavacCall(List<String> modules, Path classes) {
        return super.generateJavacCall(modules, classes)
            .with("-Xplugin:showPlugin")
            .with("--processor-module-path", folders.workspace("modules"));
      }
    }.execute();
    execute(
        new MyCall("javadoc", List.of())
            .with("--module", "showcode")
            .with("--module-source-path", "./*/main/java")
            .with("-docletpath", folders.workspace("modules", "showcode@99.jar"))
            .with("-doclet", "showcode.ShowDoclet"));
  }
}

record MyCall(String name, List<String> arguments) implements AnyCall<MyCall> {
  @Override
  public MyCall arguments(List<String> arguments) {
    if (this.arguments == arguments) return this;
    return new MyCall(name, arguments);
  }
}
