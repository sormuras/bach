package de.sormuras.bach;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.spi.ToolProvider;

/*BODY*/
/** Bach consuming task. */
public interface Task extends Consumer<Bach> {

  /** Parse passed arguments and convert them into a list of tasks. */
  static List<Task> of(Bach bach, Collection<String> args) {
    bach.log("Parsing argument(s): %s", args);
    var arguments = new ArrayDeque<>(args);
    var tasks = new ArrayList<Task>();
    var lookup = MethodHandles.publicLookup();
    var type = MethodType.methodType(void.class);
    while (!arguments.isEmpty()) {
      var name = arguments.pop();
      // Try Bach API method w/o parameter -- single argument is consumed
      try {
        try {
          lookup.findVirtual(Object.class, name, type);
        } catch (NoSuchMethodException e) {
          var handle = lookup.findVirtual(bach.getClass(), name, type);
          tasks.add(new Task.MethodHandler(name, handle));
          continue;
        }
      } catch (ReflectiveOperationException e) {
        // fall through
      }
      // Try provided tool -- all remaining arguments are consumed
      var tool = ToolProvider.findFirst(name);
      if (tool.isPresent()) {
        tasks.add(new Task.ToolRunner(tool.get(), arguments));
        break;
      }
      throw new IllegalArgumentException("Unsupported task named: " + name);
    }
    return List.copyOf(tasks);
  }

  /** MethodHandler invoking task. */
  class MethodHandler implements Task {
    private final String name;
    private final MethodHandle handle;

    MethodHandler(String name, MethodHandle handle) {
      this.name = name;
      this.handle = handle;
    }

    @Override
    public void accept(Bach bach) {
      try {
        bach.log("Invoking %s()...", name);
        handle.invokeExact(bach);
      } catch (Throwable t) {
        throw new AssertionError("Running method failed: " + name, t);
      }
    }

    @Override
    public String toString() {
      return "MethodHandler[name=" + name + "]";
    }
  }

  /** ToolProvider running task. */
  class ToolRunner implements Task {

    private final ToolProvider tool;
    private final String name;
    private final String[] arguments;

    ToolRunner(ToolProvider tool, Collection<?> arguments) {
      this.tool = tool;
      this.name = tool.name();
      this.arguments = arguments.stream().map(Object::toString).toArray(String[]::new);
    }

    @Override
    public void accept(Bach bach) {
      var code = bach.run(tool, arguments);
      if (code != 0) {
        throw new AssertionError(name + " returned non-zero exit code: " + code);
      }
    }

    @Override
    public String toString() {
      return "ToolRunner[name=" + name + ", arguments=" + List.of(arguments) + "]";
    }
  }
}
