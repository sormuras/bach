package de.sormuras.bach;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/*BODY*/
/** Bach consuming no-arg task operating via side-effects. */
@FunctionalInterface
public interface Task {

  /** Performs this task on the given Bach instance. */
  void perform(Bach bach) throws Exception;

  /** Transform a name and arguments into a task object. */
  static Task of(String name, Deque<String> arguments) {
    // try {
    //   var method = Bach.class.getMethod(name);
    //   return bach -> method.invoke(bach);
    // } catch (ReflectiveOperationException e) {
    //   // fall-through
    // }
    try {
      var taskClass = Class.forName(name);
      if (Task.class.isAssignableFrom(taskClass)) {
        return (Task) taskClass.getConstructor().newInstance();
      }
      throw new IllegalArgumentException(taskClass + " doesn't implement " + Task.class);
    } catch (ReflectiveOperationException e) {
      // fall-through
    }
    var defaultTask = Task.Default.valueOf(name.toUpperCase());
    return defaultTask.consume(arguments);
  }

  /** Transform strings to tasks. */
  static List<Task> of(List<String> args) {
    var tasks = new ArrayList<Task>();
    if (args.isEmpty()) {
      tasks.add(Default.BUILD);
    } else {
      var arguments = new ArrayDeque<>(args);
      while (!arguments.isEmpty()) {
        var argument = arguments.removeFirst();
        tasks.add(of(argument, arguments));
      }
    }
    return tasks;
  }

  /** Default task delegating to Bach API methods. */
  enum Default implements Task {
    BUILD(Bach::build, "Build modular Java project in base directory."),
    COMPILE(Bach::compile, "Compile (javac and jar) sources to binary assets."),
    DOCUMENT(Bach::document, "Generate documentation for this project."),
    // CLEAN(Bach::clean, "Delete all generated assets - but keep caches intact."),
    // ERASE(Bach::erase, "Delete all generated assets - and also delete caches."),
    HELP(Bach::help, "Print this help screen on standard out... F1, F1, F1!"),
    // LAUNCH(Bach::launch, "Start project's main program."),
    SYNC(Bach::sync, "Resolve required external assets, like 3rd-party modules."),
    TEST(Bach::test, "Launch the JUnit Platform Console scanning modules for tests."),
    TOOL(
        null,
        "Run named tool consuming all remaining arguments:",
        "  tool <name> <args...>",
        "  tool java --show-version Program.java") {
      /** Return new Task instance running the named tool and consuming all remaining arguments. */
      @Override
      Task consume(Deque<String> arguments) {
        var name = arguments.removeFirst();
        var args = arguments.toArray(String[]::new);
        arguments.clear();
        return bach -> bach.run.run(name, args);
      }
    };

    final Task task;
    final String[] description;

    Default(Task task, String... description) {
      this.task = task;
      this.description = description;
    }

    @Override
    public void perform(Bach bach) throws Exception {
      //        var key = "bach.task." + name().toLowerCase() + ".enabled";
      //        var enabled = Boolean.parseBoolean(bach.get(key, "true"));
      //        if (!enabled) {
      //          bach.run.info("Task " + name() + " disabled.");
      //          return;
      //        }
      task.perform(bach);
    }

    /** Return this default task instance without consuming any argument. */
    Task consume(Deque<String> arguments) {
      return this;
    }
  }
}
