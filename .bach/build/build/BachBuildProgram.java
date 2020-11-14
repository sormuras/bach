package build;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.BuildProgram;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.ProjectBuilder;
import com.github.sormuras.bach.ProjectInfo;
import java.time.Duration;
import java.time.Instant;

public class BachBuildProgram implements BuildProgram {

  public static void main(String... args) {
    new BachBuildProgram().build(Bach.ofSystem(), args);
  }

  public BachBuildProgram() {}

  @Override
  public void build(Bach bach, String... args) {
    var out = bach.printStream();
    var err = System.err;

    var info = getClass().getModule().getAnnotation(ProjectInfo.class);
    var project = Project.of(info);
    out.println("Build Bach " + project.version() + " // @" + project.main().jarslug() + ".jar");

    System.clearProperty("bach.project.name");
    System.clearProperty("bach.project.version");
    System.clearProperty("bach.project.main.release");
    System.clearProperty("bach.project.main.jarslug");

    var start = Instant.now();
    try {
      new ProjectBuilder(bach, project).build();
    } catch (RuntimeException exception) {
      exception.printStackTrace(err);
    } finally {
      out.printf("Build took %d milliseconds%n", Duration.between(start, Instant.now()).toMillis());
    }
  }

  @Override
  public String toString() {
    return "Bach's Build Program";
  }
}
