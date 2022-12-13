package run.duke;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.spi.ToolProvider;
import run.duke.internal.CollectionToolFinder;
import run.duke.internal.JavaProgramsToolFinder;
import run.duke.internal.ModulePathToolFinder;
import run.duke.internal.NativeProcessToolProvider;
import run.duke.internal.ToolCallsToolOperator;

@FunctionalInterface
public interface ToolFinder {
  /** {@return a description of the finder, or an empty {@code String} if none is available} */
  default String description() {
    return getClass().getSimpleName();
  }

  Collection<? extends Tool> findTools();

  /**
   * Find a tool by its identifier or its nickname.
   *
   * @param string the identifier or the short variant of the tool to look for
   * @return a tool instance wrapped in an optional, or an empty optional wrapper
   */
  default Optional<? extends Tool> findTool(String string) {
    return findTools().stream().filter(tool -> tool.test(string)).findFirst();
  }

  static ToolFinder ofTools(String description, Tool... tools) {
    return ToolFinder.ofTools(description, List.of(tools));
  }

  static ToolFinder ofTools(String description, Collection<? extends Tool> tools) {
    return new CollectionToolFinder(description, List.copyOf(tools));
  }

  static ToolFinder ofToolCalls(String description, Collection<ToolCalls> calls) {
    var tools = calls.stream().map(c -> new ToolCallsToolOperator(c.name(), c)).map(Tool::of);
    return ToolFinder.ofTools(description, tools.toList());
  }

  static ToolFinder ofToolProviders(String description, Iterable<ToolProvider> providers) {
    var tools = new ArrayList<Tool>();
    for (var provider : providers) tools.add(Tool.of(provider));
    return ToolFinder.ofTools(description, tools);
  }

  static ToolFinder ofJavaPrograms(String description, Path path, Path java) {
    return new JavaProgramsToolFinder(description, path, java);
  }

  static ToolFinder ofNativeTools(
      String description, UnaryOperator<String> renamer, Path directory, List<String> names) {
    var tools = new ArrayList<Tool>();
    for (var name : names) {
      var executable = directory.resolve(name);
      var renamed = renamer.apply(name);
      var command = List.of(executable.toString());
      var provider = new NativeProcessToolProvider(renamed, command);
      tools.add(new Tool.OfProvider(renamed, provider));
    }
    return ToolFinder.ofTools(description, tools);
  }

  static ToolFinder ofToolProviders(String description, Path... paths) {
    return new ModulePathToolFinder(description, paths);
  }
}
