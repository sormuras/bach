package build;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.BuildProgram;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.ProjectBuilder;
import com.github.sormuras.bach.ProjectInfo;
import com.github.sormuras.bach.project.Base;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

public class BachBuildProgram implements BuildProgram {

  public static void main(String... args) {
    new BachBuildProgram().build(Bach.ofSystem(), args);
  }

  private static String version() {
    try {
      return Files.readString(Path.of("VERSION"));
    } catch (Exception exception) {
      throw new Error("Read VERSION failed: ", exception);
    }
  }

  public BachBuildProgram() {}

  @Override
  public void build(Bach bach, String... args) {
    var out = bach.printStream();
    var err = System.err;
    var version = args.length == 0 ? version() : args[0];
    var jarslug = args.length < 2 ? version : args[1];
    out.println("Build Bach " + version + " using Bach " + Bach.version());
    var info = getClass().getModule().getAnnotation(ProjectInfo.class);
    var project = Project.of(Base.ofCurrentDirectory(), info).with(Version.parse(version));
    var start = Instant.now();
    try {
      new BachBuilder(bach, project, jarslug).build();
    } catch (Exception exception) {
      err.println(exception);
    } finally {
      out.printf("Build took %d milliseconds%n", Duration.between(start, Instant.now()).toMillis());
    }
  }

  @Override
  public String toString() {
    return "Bach's Build Program";
  }

  static class BachBuilder extends ProjectBuilder {

    final String jarslug;

    BachBuilder(Bach bach, Project project, String jarslug) {
      super(bach, project);
      this.jarslug = jarslug;
    }

    @Override
    public String computeMainJarFileName(String module) {
      return module + "@" + jarslug + ".jar";
    }
  }
}
