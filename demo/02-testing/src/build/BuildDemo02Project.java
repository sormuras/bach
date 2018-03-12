import java.nio.file.Paths;
import java.util.function.Supplier;

class BuildDemo02Project {

    public static void main(String... args) {
        var project = Project.builder();
        project.name = "Demo2";
        project.version = "II";
        project.libs = Paths.get(".bach/resolved");

        var bach = new Bach();
        bach.run("project", new CompilerTask(bach, project.build()));
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
