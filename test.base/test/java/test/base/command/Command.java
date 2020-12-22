package test.base.command;

import java.util.List;

record Command(String tool, List<Argument> arguments) implements ToolCall<Command> {

  static Command of(String tool) {
    return new Command(tool, List.of());
  }

  @Override
  public Command with(List<Argument> arguments) {
    return new Command(tool, arguments);
  }
}
