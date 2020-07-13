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

      @Override
      public String computeMainJavac(Project project) {
        return super.computeMainJavac(project).toUpperCase();
      }
    }

    bach.with(new CustomWorkflow()).build(project);
  }

  record Bach(Level threshold, Workflow workflow) {

    public static Bach of() {
      return new Bach(Level.INFO, new Workflow());
    }

    Bach with(Level threshold) {
      return new Bach(threshold, workflow);
    }

    Bach with(Workflow workflow) {
      return new Bach(threshold, workflow);
    }

    void build(Project project) {
      workflow.build(this, project);
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

    void build(Bach bach, Project project) {
      // if (bach.log(Level.DEBUG)) System.out.println("\n" + getClass());
      // System.out.println(computeMainJavac(project));
    }

    public String computeMainJavac(Project project) {
      return "javac --module-version " + project.version() + " --release " + project.main().release();
    }
  }
}
