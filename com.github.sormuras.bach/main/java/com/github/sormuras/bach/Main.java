package com.github.sormuras.bach;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayDeque;
import java.util.List;

/** Bach's main program. */
public class Main {

  /**
   * An annotated method indicates that it is intended to be run from Bach's main program's CLI.
   *
   * @see #performAction(String)
   */
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  @Documented
  @Inherited
  public @interface Action {
    String[] value() default {};
  }

  public static void main(String... args) {
    System.exit(run("build", args));
  }

  public static int run(String module, String... args) {
    var bach = Bach.of(module);
    if (args.length == 0) {
      bach.print("No argument, no action.");
      return 0;
    }
    if (args[0].equals("tool")) {
      var deque = new ArrayDeque<>(List.of(args));
      deque.removeFirst(); // "tool"
      if (deque.isEmpty()) {
        bach.print("Main action 'tool' requires at a name: bach tool NAME <ARGS...>");
        return 1;
      }
      var command = Command.of(deque.removeFirst());
      var recording = bach.run(deque.isEmpty() ? command : command.add("", deque.toArray()));
      if (!recording.errors().isEmpty()) bach.print(recording.errors());
      if (!recording.output().isEmpty()) bach.print(recording.output());
      if (recording.isError())
        bach.print("Tool %s returned exit code %d", command.name(), recording.code());
      return recording.code();
    }
    return new Main(bach).performActions(args);
  }

  private final Bach bach;

  public Main(Bach bach) {
    this.bach = bach;
  }

  public int performActions(String... actions) {
    var list = List.of(actions);
    bach.debug("Perform %d action%s: %s", list.size(), list.size() == 1 ? "" : "s", list);
    for (var action : list) {
      var status = performAction(action);
      if (status != 0) return status;
    }
    return 0;
  }

  public int performAction(String action) {
    bach.debug("Perform main action: `%s`", action);
    try {
      switch (action) {
        case "build" -> bach.build();
        case "clean" -> bach.clean();
        case "help", "usage" -> bach.printHelp();
        case "info" -> bach.printInfo();
        case "version" -> bach.printVersion();
        default -> {
          bach.print("Unsupported action: %s", action);
          return 4;
        }
      }
      return 0;
    } catch (Exception exception) {
      bach.print("Action %s failed: %s", action, exception);
      exception.printStackTrace(System.err);
      return 1;
    }
  }
}
