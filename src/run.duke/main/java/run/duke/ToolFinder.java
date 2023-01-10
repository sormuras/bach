package run.duke;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import run.duke.internal.CompositeToolFinder;
import run.duke.internal.JavaProgramsToolFinder;
import run.duke.internal.ModuleLayerToolFinder;
import run.duke.internal.ModulePathToolFinder;
import run.duke.internal.NativeProcessToolProvider;
import run.duke.internal.SimpleToolFinder;

/** A finder of tools. */
@FunctionalInterface
public interface ToolFinder {
  static ToolFinder empty() {
    return new SimpleToolFinder(List.of());
  }

  static ToolFinder compose(ToolFinder... toolboxes) {
    return new CompositeToolFinder(List.of(toolboxes));
  }

  static ToolFinder of(Tool tool, Tool... more) {
    return new SimpleToolFinder(Stream.concat(Stream.of(tool), Stream.of(more)).toList());
  }

  static ToolFinder of(Collection<Tool> tools) {
    return new SimpleToolFinder(List.copyOf(tools));
  }

  static ToolFinder ofJavaPrograms(Path path, Path java) {
    return new JavaProgramsToolFinder(path, java);
  }

  static ToolFinder ofModuleLayer(ModuleLayer layer) {
    return new ModuleLayerToolFinder(layer, module -> true);
  }

  static ToolFinder ofModulePath(Path... entries) {
    return new ModulePathToolFinder(entries);
  }

  static ToolFinder ofNativeToolsInJavaHome(String... names) {
    var bin = Path.of(System.getProperty("java.home"), "bin");
    return ofNativeTools(name -> "java.home/" + name, bin, List.of(names));
  }

  static ToolFinder ofNativeTools(
      UnaryOperator<String> renamer, Path directory, List<String> names) {
    var tools = new ArrayList<Tool>();
    for (var name : names) {
      var executable = directory.resolve(name);
      var command = List.of(executable.toString());
      var provider = new NativeProcessToolProvider(name, command);
      var tool = Tool.of(renamer.apply(name), provider);
      tools.add(tool);
    }
    return of(tools);
  }

  /** {@return a list of runnable tool instances} */
  List<Tool> tools();

  /**
   * {@return the first tool matching the query wrapped in an optional, or empty optional}
   *
   * @param identifierOrNickname the query string, typically an identifier or a nickname of a tool
   */
  default Optional<Tool> findTool(String identifierOrNickname) {
    return tools().stream().filter(tool -> tool.matches(identifierOrNickname)).findFirst();
  }
}
