package test.base.sandbox;

import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.lang.module.ModuleDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.spi.ToolProvider;

import org.junit.jupiter.api.Test;
import test.base.SwallowSystem;

public interface Box3 {

  class Main {
    @Test
    @SwallowSystem
    void test() {
      var foo =
          new Unit(
              ModuleDescriptor.newModule("foo").build(),
              RunTool.of("javac", "--version"),
              RunTool.of("jar", "--version"));
      var main =
          new Realm(
              List.of(foo),
              RunTool.of("javac", "--version"),
              List.of(RunTool.of("javadoc", "--version"), RunTool.of("jlink", "--version")));
      var project = new Project("Box III", List.of(main));

      var bach = new Bach(project);
      bach.execute(bach.newBuildSequence());
      assertLinesMatch(
          List.of(
              "+ Build Sequence",
              "* PrintSystemProperties",
              "* PrintProjectComponents",
              "* javac --version",
              "* javac --version",
              "* jar --version",
              "* javadoc --version",
              "* jlink --version",
              "= Build Sequence"),
          bach.executedTaskLabels);
    }
  }

  final class Unit {

    private final ModuleDescriptor descriptor;
    private final Task javac;
    private final Task jar;

    public Unit(ModuleDescriptor descriptor, Task javac, Task jar) {
      this.descriptor = descriptor;
      this.javac = javac;
      this.jar = jar;
    }

    public String name() {
      return descriptor.name();
    }
  }

  final class Realm {
    private final List<Unit> units;
    private final Task javac;
    private final List<Task> more;

    public Realm(List<Unit> units, Task javac, List<Task> more) {
      this.units = units;
      this.javac = javac;
      this.more = more;
    }
  }

  final class Project {
    private final String title;
    private final List<Realm> realms;

    public Project(String title, List<Realm> realms) {
      this.title = title;
      this.realms = realms;
    }
  }

  class Bach {

    private final Project project;
    private final List<String> executedTaskLabels = new ArrayList<>();

    public Bach(Project project) {
      this.project = project;
    }

    public Task newBuildSequence() {
      var tasks = new ArrayList<Task>();
      tasks.add(new PrintSystemProperties());
      tasks.add(new PrintProjectComponents());
      for (var realm : project.realms) {
        tasks.add(realm.javac);
        for (var unit : realm.units) {
          tasks.add(unit.javac);
          tasks.add(unit.jar);
        }
        tasks.addAll(realm.more);
      }
      return Task.sequence("Build Sequence", tasks);
    }

    public void execute(Task task) {
      var tasks = task.list;
      if (tasks.isEmpty()) {
        executedTaskLabels.add("* " + task.label);
        try {
          task.execute(this);
        } catch (Exception e) {
          throw new Error("Task execution failed", e);
        }
        return;
      }
      executedTaskLabels.add("+ " + task.label);
      for (var sub : tasks) {
        execute(sub);
      }
      executedTaskLabels.add("= " + task.label);
    }
  }

  class Task {

    public static Task sequence(String label, List<Task> tasks) {
      return new Task(label, tasks);
    }

    private final String label;
    private final List<Task> list;

    public Task() {
      this("", List.of());
    }

    public Task(String label, List<Task> list) {
      this.label = label.isBlank() ? getClass().getSimpleName() : label;
      this.list = list;
    }

    public void execute(Bach bach) {}
  }

  class RunTool extends Task {

    static RunTool of(String name, Object... arguments) {
      var tool = ToolProvider.findFirst(name).orElseThrow();
      var args = new String[arguments.length];
      for (int i = 0; i < args.length; i++) args[i] = arguments[i].toString();
      return new RunTool(tool, args);
    }

    private final ToolProvider tool;
    private final String[] args;

    public RunTool(ToolProvider tool, String... args) {
      super(tool.name() + " " + String.join(" ", args), List.of());
      this.tool = tool;
      this.args = args;
    }

    @Override
    public void execute(Bach bach) {
      var code = tool.run(System.out, System.err, args);
      if (code == 0) return;
      throw new Error("Tool run exit code: " + code);
    }
  }

  class PrintSystemProperties extends Task {

    @Override
    public void execute(Bach bach) {
      var lines = List.of("System Properties", line("os.name"), line("user.dir"));
      System.out.println(String.join(System.lineSeparator(), lines));
    }

    private static String line(String key) {
      return "\t" + key + "=" + System.getProperty(key);
    }
  }

  class PrintProjectComponents extends Task {
    @Override
    public void execute(Bach bach) {
      System.out.println(bach.project.title);
    }
  }
}
