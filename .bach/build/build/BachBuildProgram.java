package build;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.BuildProgram;
import com.github.sormuras.bach.project.Project;
import com.github.sormuras.bach.project.ProjectInfo;
import java.nio.file.Files;
import java.nio.file.Path;

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

    new BachBuilder(bach, project).build();

    replaceAll(
        Path.of(".bach/workspace/deploy/maven/com.github.sormuras.bach.pom.xml"),
        "  <dependencies>",
        """
          <name>Bach</name>
          <description>🎼 Java Shell Builder</description>

          <url>https://github.com/sormuras/bach</url>
          <scm>
            <url>https://github.com/sormuras/bach.git</url>
          </scm>

          <developers>
            <developer>
              <name>Christian Stein</name>
              <id>sormuras</id>
            </developer>
          </developers>

          <licenses>
            <license>
              <name>MIT License</name>
              <url>https://opensource.org/licenses/mit-license</url>
              <distribution>repo</distribution>
            </license>
          </licenses>

          <dependencies>""");
  }

  @Override
  public String toString() {
    return "Bach's Build Program";
  }

  private static void replaceAll(Path path, String regex, String replacement) {
    try {
      Files.writeString(path, Files.readString(path).replaceAll(regex, replacement));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
