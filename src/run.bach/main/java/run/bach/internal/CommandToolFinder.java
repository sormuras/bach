package run.bach.internal;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import run.bach.Command;
import run.bach.Tool;
import run.bach.ToolFinder;
import run.bach.ToolRunner;

public record CommandToolFinder(Map<String, Command> commands) implements ToolFinder {
  @Override
  public String description() {
    return "Command Finder";
  }

  @Override
  public Optional<Tool> find(String tool, ToolRunner runner) {
    var command = lookupCommand(tool);
    if (command == null) return Optional.empty();
    var provider = new CommandToolProvider(command, runner);
    var namespace = "<namespace>"; //
    var name = provider.name();
    return Optional.of(new Tool(namespace, name, provider));
  }

  Command lookupCommand(String tool) {
    var command = commands.get(tool); // <namespace>/<tool>
    if (command != null) return command;
    var nick = '/' + tool; // **/<tool>
    for (var entry : commands.entrySet()) {
      if (entry.getKey().endsWith(nick)) return entry.getValue();
    }
    return null;
  }

  @Override
  public List<String> identifiers() {
    return List.copyOf(commands.keySet());
  }
}
