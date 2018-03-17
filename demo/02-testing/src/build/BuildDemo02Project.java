import java.nio.file.Paths;
import java.util.function.Supplier;

class BuildDemo02Project {

  public static void main(String... args) {
    var project = Project.builder();
    project.name = "Demo2";
    project.version = "II";
    project.libs = Paths.get(".bach/resolved");

    build(new Bach(), project.build());
  }

  static void build(Bach bach, Project project) {
    Supplier<Integer> printer = () -> new PrinterFunction().apply(bach, project);
    Supplier<Integer> compiler = new CompilerTask(bach, project);
    bach.run("build", printer, compiler);
  }

  static class CompilerTask implements Supplier<Integer> {
    final Bach bach;
    final Project project;

    CompilerTask(Bach bach, Project project) {
      this.bach = bach;
      this.project = project;
    }

    int compile(Project.ModuleGroup source) {
      bach.log("[compile] %s", source.name);
      var javac = new JdkTool.Javac();
      javac.destination = source.destination;
      javac.moduleSourcePath = source.moduleSourcePath;
      javac.modulePath = source.modulePath;
      return javac.toCommand().get();
    }

    @Override
    public Integer get() {
      bach.log("[compiler] %s", project);
      return compile(project.moduleGroupMap.get("main"));
    }
  }
}
