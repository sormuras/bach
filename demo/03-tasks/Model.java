import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Stream;

interface Model {

    class Project {

        String name = Paths.get(".").toAbsolutePath().normalize().getFileName().toString();
        boolean parallel = true;

        @SafeVarargs
        final int accept(Class<? extends Supplier<Integer>>... tasks) {
            return accept(Arrays.stream(tasks).map(this::instantiate));
        }

        private Supplier<Integer> instantiate(Class<? extends Supplier<Integer>> type) {
            try {
                return type.getDeclaredConstructor(Project.class).newInstance(this);
            } catch (ReflectiveOperationException e) {
                throw new Error(e);
            }
        }

        @SafeVarargs
        final int accept(Supplier<Integer>... tasks) {
            return accept(Arrays.stream(tasks));
        }

        int accept(Stream<Supplier<Integer>> stream) {
            if (parallel) {
                stream = stream.parallel();
            }
            var result = stream.map(CompletableFuture::supplyAsync).map(CompletableFuture::join).mapToInt(t -> t).sum();
            if (result != 0) {
                throw new IllegalStateException("0 expected, but got: " + result);
            }
            return result;
        }

        @Override
        public String toString() {
            return "Project [name="+name+"]";
        }
    }

    abstract class Task implements Supplier<Integer> {
        final Project project;
        Task(Project project) {
            this.project = project;
        }

        @Override
        public Integer get() {
            try {
                return run();
            }
            catch (Exception e) {
                return 1;
            }
        }

        abstract int run() throws Exception;
    }

    class Cleaner extends Task {

        Cleaner(Project project) {
            super(project);
        }

        @Override
        public int run() throws Exception {
            System.out.println("Cleaning " + project);
            Thread.sleep(900);
            System.out.println("Cleaned.");
            return 0;
        }
    }

    class Formatter extends Task {

        Formatter(Project project) {
            super(project);
        }

        @Override
        public int run() throws Exception {
            System.out.println("Formatting " + project);
            Thread.sleep(300);
            System.out.println("Formatted.");
            return 0;
        }
    }

    static int task(String name, Project project) {
        System.out.println("[" + name + "] begin " + project);
        var millis = (long)(Math.random() * 1000 + 500);
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new Error(e);
        }
        System.out.println("[" + name + "] done. " + millis);
        return Math.random() < 0.99 ? 0 : 1;
    }

    static void main(String[] args) {
        System.out.println("Model.main");
        var project = new Project();

        project.accept(Cleaner.class, Formatter.class);

        project.accept(() -> task("single", project));

        project.accept(
                () -> task("compile+test", project),
                () -> task("package", project),
                () -> task("document", project));
    }
}
