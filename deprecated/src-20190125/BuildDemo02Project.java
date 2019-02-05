import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
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
    var compiler = new Task.CompilerTask(bach, project);
    bach.run("build", printer, compiler);
  }

  static class PrinterFunction implements BiFunction<Bach, Project, Integer> {
    @Override
    public Integer apply(Bach bach, Project project) {
      bach.log("%s %s", project.name(), project.version());
      return 0;
    }
  }
}
