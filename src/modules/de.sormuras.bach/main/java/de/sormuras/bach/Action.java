package de.sormuras.bach;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/*BODY*/
/** Bach consuming no-arg action operating via side-effects. */
@FunctionalInterface
public interface Action {

  /** Performs this action on the given Bach instance. */
  void perform(Bach bach) throws Exception;

  /** Transform a name and arguments into an action object. */
  static Action of(String name, Deque<String> arguments) {
    // try {
    //   var method = Bach.class.getMethod(name);
    //   return bach -> method.invoke(bach);
    // } catch (ReflectiveOperationException e) {
    //   // fall-through
    // }
    try {
      var actionClass = Class.forName(name);
      if (Action.class.isAssignableFrom(actionClass)) {
        return (Action) actionClass.getConstructor().newInstance();
      }
      throw new IllegalArgumentException(actionClass + " doesn't implement " + Action.class);
    } catch (ReflectiveOperationException e) {
      // fall-through
    }
    var defaultAction = Action.Default.valueOf(name.toUpperCase());
    return defaultAction.consume(arguments);
  }

  /** Transform strings to actions. */
  static List<Action> of(List<String> args) {
    var actions = new ArrayList<Action>();
    if (args.isEmpty()) {
      actions.add(Action.Default.HELP);
    } else {
      var arguments = new ArrayDeque<>(args);
      while (!arguments.isEmpty()) {
        var argument = arguments.removeFirst();
        actions.add(of(argument, arguments));
      }
    }
    return actions;
  }

  /** Default action delegating to Bach API methods. */
  enum Default implements Action {
    // BUILD(Bach::build, "Build modular Java project in base directory."),
    // CLEAN(Bach::clean, "Delete all generated assets - but keep caches intact."),
    // ERASE(Bach::erase, "Delete all generated assets - and also delete caches."),
    HELP(Bach::help, "Print this help screen on standard out... F1, F1, F1!"),
    // LAUNCH(Bach::launch, "Start project's main program."),
    SYNC(Bach::synchronize, "Resolve required external assets, like 3rd-party modules."),
    TOOL(
        null,
        "Run named tool consuming all remaining arguments:",
        "  tool <name> <args...>",
        "  tool java --show-version Program.java") {
      /** Return new Action running the named tool and consuming all remaining arguments. */
      @Override
      Action consume(Deque<String> arguments) {
        var name = arguments.removeFirst();
        var args = arguments.toArray(String[]::new);
        arguments.clear();
        return bach -> bach.run.run(name, args);
      }
    };

    final Action action;
    final String[] description;

    Default(Action action, String... description) {
      this.action = action;
      this.description = description;
    }

    @Override
    public void perform(Bach bach) throws Exception {
      //        var key = "bach.action." + name().toLowerCase() + ".enabled";
      //        var enabled = Boolean.parseBoolean(bach.get(key, "true"));
      //        if (!enabled) {
      //          bach.run.info("Action " + name() + " disabled.");
      //          return;
      //        }
      action.perform(bach);
    }

    /** Return this default action instance without consuming any argument. */
    Action consume(Deque<String> arguments) {
      return this;
    }
  }
}
