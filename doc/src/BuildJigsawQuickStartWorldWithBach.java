import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.spi.ToolProvider;

class BuildJigsawQuickStartWorldWithBach {

  public static void main(String... arguments) {
    var base = new Base(Path.of("doc", "project", "JigsawQuickStartWorld"));

    var main = new Realm();
    main.name = "main";
    main.declares = Set.of("com.greetings", "org.astro");
    main.sourcePathPatterns.add(base.path + File.separator + '*' + File.separator + "main");

    var test = new Realm();
    test.name = "test";
    test.declares = Set.of("test.modules", "org.astro");
    test.requires = List.of(main);
    test.sourcePathPatterns.add(base.path + File.separator + '*' + File.separator + "test");
    test.modulePaths.add(base.modules(main.name));
    test.patches.put("org.astro", List.of(base.path("org.astro", "main")));

    var project = new Project();
    project.realms.add(main);
    project.realms.add(test);

    var bach = new Bach();
    bach.base = base;
    bach.project = project;
    bach.build();
  }

  static class Base {
    private final Path path;
    private final Path workspace;

    Base(Path path) {
      this(path, path.resolve(".bach/workspace"));
    }

    Base(Path path, Path workspace) {
      this.path = path;
      this.workspace = workspace;
    }

    Path path(String first, String... more) {
      return path.resolve(Path.of(first, more));
    }

    Path workspace(String first, String... more) {
      return workspace.resolve(Path.of(first, more));
    }

    Path modules(String realm) {
      return workspace("modules", realm);
    }
  }

  static class Realm {
    String name;
    Set<String> declares = new TreeSet<>(); // --module
    List<Realm> requires = new ArrayList<>();
    List<String> sourcePathPatterns = new ArrayList<>(); // --module-source-path
    List<Path> modulePaths = new ArrayList<>(); // --module-path
    Map<String, List<Path>> patches = new TreeMap<>(); // --patch-module

    @Override
    public String toString() {
      return name;
    }
  }

  static class Project {
    List<Realm> realms = new ArrayList<>();
  }

  static class Task {

    private final String label;
    private final List<Task> list;

    Task() {
      this("", List.of());
    }

    Task(String label, List<Task> list) {
      this.label = label;
      this.list = list;
    }

    boolean runnable() {
      return list.isEmpty();
    }

    void run(Bach bach) {}
  }

  static class Tasks {

    static Task of(Runnable runnable) {
      class RunnableTask extends Task {
        @Override
        void run(Bach bach) {
          runnable.run();
        }
      }
      return new RunnableTask();
    }

    static Task run(String tool, String... args) {
      return new RunTool(ToolProvider.findFirst(tool).orElseThrow(), args);
    }

    static Task sequence(String label, List<Task> tasks) {
      return new Task(label, tasks);
    }

    static Task sequence(String label, Task... tasks) {
      return sequence(label, List.of(tasks));
    }

    static class PrintBasics extends Task {
      @Override
      void run(Bach bach) {
        System.out.println("System Properties");
        System.out.println("\tos.name=" + System.getProperty("os.name"));
        System.out.println("\tjava.version=" + System.getProperty("java.version"));
        System.out.println("\tuser.dir=" + System.getProperty("user.dir"));
        var base = bach.base;
        System.out.println("Base");
        System.out.println("\tpath=" + base.path + " -> " + base.path.toUri());
      }
    }

    static class PrintProject extends Task {

      @Override
      void run(Bach bach) {
        var project = bach.project;
        System.out.println("Project");
        System.out.println("\trealms=" + project.realms);
        System.out.println("Realm");
        for (var realm : project.realms) {
          System.out.println("\t" + realm.name);
          System.out.println("\t\tdeclares=" + realm.declares);
        }
      }
    }

    static class RunTool extends Task {
      private final ToolProvider tool;
      private final String[] args;

      RunTool(ToolProvider tool, String... args) {
        this.tool = tool;
        this.args = args;
      }

      @Override
      void run(Bach bach) {
        System.out.println(tool.name() + ' ' + String.join(" ", args));
        tool.run(System.out, System.err, args);
      }
    }
  }

  static class Bach {
    Base base;
    Project project;

    void build() {
      run(generateBuildTask());
    }

    Task generateBuildTask() {
      return Tasks.sequence(
          "Build project",
          Tasks.of(() -> System.out.println("BuildJigsawQuickStartWorldWithBach")),
          new Tasks.PrintBasics(),
          new Tasks.PrintProject(),
          Tasks.run("javac", "--version"));
    }

    void run(Task task) {
      var label = task.label.isEmpty() ? task.getClass().getSimpleName() : task.label;
      if (task.runnable()) {
        System.out.printf("[task] %s%n", label);
        task.run(this);
        return;
      }
      System.out.println("[list] " + label + " begin");
      task.list.forEach(this::run);
      System.out.println("[list] " + label + " end");
    }
  }
}
