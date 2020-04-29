package test.base.sandbox;

import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.lang.module.ModuleDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.spi.ToolProvider;

import org.junit.jupiter.api.Test;

public interface Box3 {

  class Main {
    @Test
    void test() {
      var foo =
          new Unit(
              ModuleDescriptor.newModule("foo").build(),
              RunTool.of("javac", "--version"),
              RunTool.of("jar", "--version"));
      var realm =
          new Realm(
              "main",
              List.of(foo),
              RunTool.of("javac", "--module-source-path", "."),
              List.of(RunTool.of("javadoc"), RunTool.of("jlink")));
      var project = new Project("Box III");

      var bach = new Bach(project);
      bach.execute(
          Task.sequence(
              "Main Sequence", new PrintSystemProperties(), new PrintProjectComponents()));
      assertLinesMatch(
          List.of(
              "+ Main Sequence",
              "* PrintSystemProperties",
              "* PrintProjectComponents",
              "= Main Sequence"),
          bach.executedTaskLabels);
    }
  }

  final class Unit {

    private final ModuleDescriptor descriptor;
    private final Task compile;
    private final Task jar;

    public Unit(ModuleDescriptor descriptor, Task compile, Task jar) {
      this.descriptor = descriptor;
      this.compile = compile;
      this.jar = jar;
    }

    public String name() {
      return descriptor.name();
    }
  }

  final class Realm {
    private final String name;
    private final List<Unit> units;
    private final Task compile;
    private final List<Task> more;

    public Realm(String name, List<Unit> units, Task compile, List<Task> more) {
      this.name = name;
      this.units = units;
      this.compile = compile;
      this.more = more;
    }
  }

  final class Project {
    private final String title;

    public Project(String title) {
      this.title = title;
    }
  }

  class Bach {

    private final Project project;
    private final List<String> executedTaskLabels = new ArrayList<>();

    public Bach(Project project) {
      this.project = project;
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

    public static Task sequence(String label, Task... tasks) {
      return new Task(label, List.of(tasks));
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
