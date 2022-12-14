package run.duke;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import run.duke.internal.CollectionToolFinder;
import run.duke.internal.JavaProgramsToolFinder;
import run.duke.internal.ModuleLayerToolFinder;
import run.duke.internal.ModulePathToolFinder;
import run.duke.internal.NativeProcessToolProvider;
import run.duke.internal.ToolCallsToolOperator;

@FunctionalInterface
public interface ToolFinder {
  /** {@return a description of the finder, or an empty {@code String} if none is available} */
  default Optional<String> description() {
    return Optional.of(getClass().getSimpleName());
  }

  Collection<Tool> findTools();

  /**
   * Find a tool by its identifier or its nickname.
   *
   * @param string the identifier or the short variant of the tool to look for
   * @return a tool instance wrapped in an optional, or an empty optional wrapper
   */
  default Optional<Tool> findTool(String string) {
    return findTools().stream().filter(tool -> tool.test(string)).findFirst();
  }

  static ToolFinder ofTools(String description, Tool... tools) {
    return ToolFinder.ofTools(description, List.of(tools));
  }

  static ToolFinder ofTools(String description, Collection<Tool> tools) {
    return new CollectionToolFinder(Optional.of(description), List.copyOf(tools));
  }

  static ToolFinder ofTools(String description, ModuleLayer layer) {
    return new ModuleLayerToolFinder(description, layer);
  }

  static ToolFinder ofTools(String description, Path... paths) {
    return new ModulePathToolFinder(Optional.of(description), paths);
  }

  static ToolFinder ofToolCalls(String description, Collection<ToolCalls> calls) {
    var tools =
        calls.stream()
            .map(toolCalls -> new ToolCallsToolOperator(toolCalls.name(), toolCalls))
            .map(operator -> Tool.of(operator.name(), operator))
            .toList();
    return ToolFinder.ofTools(description, tools);
  }

  static ToolFinder ofJavaPrograms(String description, Path path, Path java) {
    return new JavaProgramsToolFinder(Optional.of(description), path, java);
  }

  static ToolFinder ofNativeTools(
      String description, UnaryOperator<String> renamer, Path directory, List<String> names) {
    var tools = new ArrayList<Tool>();
    for (var name : names) {
      var executable = directory.resolve(name);
      var renamed = renamer.apply(name);
      var command = List.of(executable.toString());
      var provider = new NativeProcessToolProvider(renamed, command);
      tools.add(Tool.of(renamed, provider));
    }
    return ToolFinder.ofTools(description, tools);
  }
}
