package com.github.sormuras.bach;

import com.github.sormuras.bach.tool.Tool;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/** Bach's main program. */
public class Main {

  /** A command line option parser. */
  record Options(
      // all arguments (raw)
      String[] args,
      // "--version"
      boolean printVersionAndExit,
      // "--show-version"
      boolean showVersionAndContinue,
      // "tool NAME [ARGS...]"
      Optional<Tool> tool,
      // actions
      List<String> actions
      ) {

    static Options parse(String... args) {
      var deque = new ArrayDeque<>(List.of(args));
      var printVersionAndExit = false;
      var showVersionAndContinue = false;
      var tool = new AtomicReference<Tool>(null);
      var actions = new ArrayList<String>();
      while (!deque.isEmpty()) {
        var argument = deque.removeFirst();
        switch (argument) {
          case "--version" -> printVersionAndExit = true;
          case "--show-version" -> showVersionAndContinue = true;
          case "tool" -> {
            if (deque.isEmpty())
              throw new IllegalArgumentException("No tool name given: bach <OPTIONS...> tool NAME <ARGS...>");
            var command = Command.of(deque.removeFirst(), deque.toArray(String[]::new));
            tool.set(command);
            deque.clear();
          }
          default -> actions.add(argument);
        }
      }
      return new Options(
          args,
          printVersionAndExit,
          showVersionAndContinue,
          Optional.ofNullable(tool.get()),
          List.copyOf(actions));
    }
  }

  public static void main(String... args) {
    System.exit(run("configuration", args));
  }

  public static int run(String module, String... args) {
    var bach = Bach.of(module);
    if (args.length == 0) {
      bach.print("No argument, no action.");
      return 0;
    }
    var options = Options.parse(args);
    if (options.showVersionAndContinue || options.printVersionAndExit) {
      bach.printVersion();
      if (options.printVersionAndExit) return 0;
    }
    if (options.tool.isPresent()) {
      var command = options.tool.get();
      var recording = bach.run(command);
      if (!recording.errors().isEmpty()) bach.print(recording.errors());
      if (!recording.output().isEmpty()) bach.print(recording.output());
      if (recording.isError())
        bach.print("Tool %s returned exit code %d", command.name(), recording.code());
      return recording.code();
    }
    return new Main(bach).performActions(options.actions);
  }

  private final Bach bach;

  public Main(Bach bach) {
    this.bach = bach;
  }

  public int performActions(List<String> list) {
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
