package run.bach.internal;

import java.util.List;
import java.util.Optional;
import run.bach.Tool;
import run.bach.ToolFinder;
import run.bach.ToolRunner;

public record InternalToolFinder() implements ToolFinder {
  @Override
  public String description() {
    return "Bach's Built-in Tools";
  }

  @Override
  public List<String> identifiers() {
    return List.of("run.bach/list");
  }

  @Override
  public Optional<Tool> find(String name, ToolRunner runner) {
    return Optional.ofNullable(
        switch (name) {
          case "run.bach/list", "list" -> new Tool(new ListTool(runner));
          default -> null;
        });
  }
}
