package build;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.BuildProgram;
import com.github.sormuras.bach.Builder;
import com.github.sormuras.bach.project.Project;
import com.github.sormuras.bach.project.ProjectInfo;
import com.github.sormuras.bach.tool.ToolCall;

public class BachBuildProgram implements BuildProgram {

  public static void main(String... args) {
    new BachBuildProgram().build(Bach.ofSystem(), args);
  }

  public BachBuildProgram() {}

  @Override
  public void build(Bach bach, String... args) {
    var module = getClass().getModule();
    bach.info("Start of custom build program in %s", module);

    var info = module.getAnnotation(ProjectInfo.class);
    bach.debug("project-info -> %s", info);

    var project = Project.of(info);

    System.clearProperty("bach.project.name");
    System.clearProperty("bach.project.version");
    System.clearProperty("bach.project.main.release");
    System.clearProperty("bach.project.main.jarslug");

    new Builder(bach, project) {
      @Override
      public ToolCall computeMainDocumentationJavadocCall() {
        var title = "🎼 Bach " + project.version();
        return super.computeMainDocumentationJavadocCall().toCommand().toBuilder()
            .with("-windowtitle", title)
            .with("-header", title)
            .with("-doctitle", title)
            .build();
      }
    }.build();
  }

  @Override
  public String toString() {
    return "Bach's Build Program";
  }
}
