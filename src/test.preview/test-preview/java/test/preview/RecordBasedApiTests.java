package test.preview;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.System.Logger.Level;
import java.lang.module.ModuleDescriptor.Version;
import org.junit.jupiter.api.Test;

public class RecordBasedApiTests {

  @Test
  void check() {
    var bach = Bach.of().with(Level.DEBUG);

    var project = Project.of()
        .with(new MainSources(bach.defaultRelease()))
        .with(Version.parse("17.01"))
        .with(new MainSources(8));

    assertEquals("17.01", project.version().toString());
    assertEquals(8, project.main().release());

    bach.build(project);

    class CustomWorkflow extends Workflow {
      CustomWorkflow(Bach bach, Project project) {
        super(bach, project);
      }

      @Override
      public String computeJavac(MainSources main) {
        return super.computeJavac(main).toUpperCase();
      }
    }

    bach.with(CustomWorkflow::new).build(project);
  }

  record Bach(Level threshold, Workflow.Constructor workflowConstructor) {

    public static Bach of() {
      return new Bach(Level.INFO, Workflow::new);
    }

    Bach with(Level threshold) {
      return new Bach(threshold, workflowConstructor);
    }

    Bach with(Workflow.Constructor workflowConstructor) {
      return new Bach(threshold, workflowConstructor);
    }

    void build(Project project) {
      workflowConstructor.newWorkflow(this, project).build();
    }

    boolean log(Level level) {
      return threshold == Level.ALL || level.getSeverity() >= threshold.getSeverity();
    }

    int defaultRelease() {
      return Runtime.version().feature();
    }
  }

  record Project(Version version, MainSources main) {

    public static Project of() {
      return new Project(Version.parse("1-ea"), new MainSources(Runtime.version().feature()));
    }

    public Project with(Version version) {
      return new Project(version, main);
    }

    public Project with(MainSources main) {
      return new Project(version, main);
    }
  }

  record MainSources(int release) {}

  static class Workflow {

    @FunctionalInterface
    interface Constructor {
      Workflow newWorkflow(Bach bach, Project project);
    }

    private final Bach bach;
    private final Project project;

    Workflow(Bach bach, Project project) {
      this.bach = bach;
      this.project = project;
    }

    void build() {
      if (bach.log(Level.DEBUG)) System.out.println("\n" + getClass());
      System.out.println(computeJavac(project.main()));
    }

    public String computeJavac(MainSources main) {
      return "javac --module-version " + project.version() + " --release " + main.release();
    }
  }
}
