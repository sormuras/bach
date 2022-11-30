package run.duke.base;

import java.util.List;
import java.util.Optional;
import run.duke.Tool;
import run.duke.ToolFinder;
import run.duke.ToolRunner;

public record BaseToolFinder() implements ToolFinder {
  @Override
  public String description() {
    return "Duke's built-in tools";
  }

  @Override
  public List<String> identifiers() {
    return List.of("run.duke/list");
  }

  @Override
  public Optional<Tool> find(String name, ToolRunner runner) {
    return Optional.ofNullable(
        switch (name) {
          case "run.duke/list", "list" -> new Tool(new ListTool(runner));
          default -> null;
        });
  }
}
