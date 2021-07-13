import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Call;
import com.github.sormuras.bach.Checkpoint;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.Settings;
import com.github.sormuras.bach.Tweak;
import com.github.sormuras.bach.call.CompileTestSpaceJavacCall;
import com.github.sormuras.bach.call.DeleteDirectoriesCall;
import com.github.sormuras.bach.project.ProjectSpace;
import com.github.sormuras.bach.workflow.BuildWorkflow;

class build {
  public static void main(String... args) {
    Bach.build(new Bach(project(), settings()));
  }

  static Project project() {
    return Project.of("ProcessingCode", "99").withMainSpace(build::main).withTestSpace(build::test);
  }

  static ProjectSpace main(ProjectSpace main) {
    return main.withModule("showcode/main/java/module-info.java");
  }

  static ProjectSpace test(ProjectSpace test) {
    return test.withModule("tests/test/java/module-info.java");
  }

  static Settings settings() {
    return Settings.of()
        .withWorkflowTweakHandler(build::handle)
        .withWorkflowCheckpointHandler(build::handle);
  }

  static Call handle(Tweak tweak) {
    if (tweak.call() instanceof CompileTestSpaceJavacCall javac) {
      return javac
          .with("-Xplugin:showPlugin")
          .with("--processor-module-path", tweak.bach().folders().workspace("modules"));
    }
    return tweak.call();
  }

  static void handle(Checkpoint checkpoint) {
    var bach = checkpoint.workflow().bach();
    var folders = bach.folders();
    if (checkpoint instanceof BuildWorkflow.StartCheckpoint) {
      bach.execute(new DeleteDirectoriesCall(folders.workspace()));
    }
    if (checkpoint instanceof BuildWorkflow.SuccessCheckpoint) {
      bach.execute(
          Call.tool("javadoc")
              .with("--module", "showcode")
              .with("--module-source-path", "./*/main/java")
              .with("-docletpath", folders.workspace("modules", "showcode@99.jar"))
              .with("-doclet", "showcode.ShowDoclet"));
    }
  }
}
