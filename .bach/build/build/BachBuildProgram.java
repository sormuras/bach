package build;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.BuildProgram;
import com.github.sormuras.bach.Builder;
import com.github.sormuras.bach.ProjectInfo;
import com.github.sormuras.bach.project.Project;
import java.lang.System.Logger.Level;

public class BachBuildProgram implements BuildProgram {

  public static void main(String... args) {
    new BachBuildProgram().build(Bach.ofSystem(), args);
  }

  public BachBuildProgram() {}

  @Override
  public void build(Bach bach, String... args) {
    var book = bach.logbook();
    var info = getClass().getModule().getAnnotation(ProjectInfo.class);
    var project = Project.of(info);

    book.log(Level.INFO, "Custom build program started");

    System.clearProperty("bach.project.name");
    System.clearProperty("bach.project.version");
    System.clearProperty("bach.project.main.release");
    System.clearProperty("bach.project.main.jarslug");

    new Builder(bach, project).build();
  }

  @Override
  public String toString() {
    return "Bach's Build Program";
  }
}
