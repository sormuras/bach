package run.bach;

import java.util.List;

@FunctionalInterface
public interface ToolRunner {
  void run(ToolCall call);

  default void run(String name, String... args) {
    run(new ToolCall(name, List.of(args)));
  }

  default Options options() {
    return Options.DEFAULTS;
  }

  default Printer printer() {
    return Printer.BROKEN;
  }

  default Toolbox toolbox() {
    return Toolbox.EMPTY;
  }
}
