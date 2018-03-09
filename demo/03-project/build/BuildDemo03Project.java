import java.nio.file.Paths;
import java.util.function.Supplier;

class BuildDemo03Project {

    static class DefaultWorker implements Supplier<Integer> {
        final Project project;
        DefaultWorker(Project project) {
            this.project = project;
        }

        @Override
        public Integer get() {
            System.out.println(project);
            return 0;
        }
    }

    public static void main(String... args) {
        var builder = Project.builder();
        builder.libs = Paths.get("libraries");

        var project = builder.build();
        var bach = new Bach();
        bach.run("project", new DefaultWorker(project));
    }
}
