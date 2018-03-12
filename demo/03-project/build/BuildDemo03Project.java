import java.nio.file.Paths;
import java.util.List;
import java.util.function.Supplier;

class BuildDemo03Project {

    public static void main(String... args) {
        var builder = Project.builder();
        builder.name = "Demo3";
        builder.version = "III";
        builder.libs = Paths.get(".bach/libs");

        var world = Paths.get(".bach/junit5-samples-master/junit5-modular-world");
        builder.moduleSource("main").moduleSourcePath = List.of(world.resolve("src/main"));
        builder.moduleSource("test").moduleSourcePath = List.of(world.resolve("src/test"));
        builder.moduleSourceMap.values().forEach(it -> it.modulePath = List.of(builder.libs));

        var project = builder.build();
        var bach = new Bach();
        bach.run("project", new CompilerTask(bach, project));
    }

    static class CompilerTask implements Supplier<Integer> {
        final Bach bach;
        final Project project;

        CompilerTask(Bach bach, Project project) {
            this.bach = bach;
            this.project = project;
        }

        int compile(Project.ModuleSource source) {
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
            return compile(project.moduleSourceMap.get("main"));
        }
    }

}
