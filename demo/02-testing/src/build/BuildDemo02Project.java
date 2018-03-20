import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

class BuildDemo02Project {

  public static void main(String... args) {
    var dependencies = Paths.get(".bach", "resolved");
    var target = Paths.get("target", "bach", "project");
    var mainDestination = target.resolve(Paths.get("main", "mods"));
    var testDestination = target.resolve(Paths.get("test", "mods"));
    var project =
        Project.builder()
            .name("Demo2")
            .version("II")
            .target(target)
            // main
            .newModuleGroup("main")
            .destination(mainDestination)
            .moduleSourcePath(List.of(Paths.get("src", "main", "java")))
            .end()
            // test
            .newModuleGroup("test")
            .destination(testDestination)
            .moduleSourcePath(List.of(Paths.get("src", "test", "java")))
            .modulePath(List.of(mainDestination, dependencies))
            .patchModule(
                Map.of(
                    "application",
                    List.of(Paths.get("src/main/java/application")),
                    "application.api",
                    List.of(Paths.get("src/main/java/application.api"))))
            .end()
            // done
            .build();

    build(new Bach(), project);
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

    int compile(Project.ModuleGroup group) {
      bach.log("[compile] %s", group.name());
      var javac = new JdkTool.Javac();
      javac.destination = group.destination();
      javac.moduleSourcePath = group.moduleSourcePath();
      javac.modulePath = group.modulePath();
      javac.patchModule = group.patchModule();
      return javac.toCommand().get();
    }

    @Override
    public Integer get() {
      bach.log("[compiler] %s", project);
      compile(project.moduleGroup("main"));
      compile(project.moduleGroup("test"));
      return 0;
    }
  }
}
