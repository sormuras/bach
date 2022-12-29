package run.duke;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import run.duke.internal.CompositeToolbox;
import run.duke.internal.JavaProgramsToolbox;
import run.duke.internal.ListToolbox;
import run.duke.internal.ModuleLayerToolbox;
import run.duke.internal.ModulePathToolbox;
import run.duke.internal.NativeProcessToolProvider;

@FunctionalInterface
public interface Toolbox extends ToolFinder {
  static Toolbox empty() {
    return new ListToolbox(List.of());
  }

  static Toolbox compose(Toolbox... toolboxes) {
    return new CompositeToolbox(List.of(toolboxes));
  }

  static Toolbox of(Tool tool, Tool... more) {
    return new ListToolbox(Stream.concat(Stream.of(tool), Stream.of(more)).toList());
  }

  static Toolbox of(Collection<Tool> tools) {
    return new ListToolbox(List.copyOf(tools));
  }

  static Toolbox ofJavaPrograms(Path path, Path java) {
    return new JavaProgramsToolbox(path, java);
  }

  static Toolbox ofModuleLayer(ModuleLayer layer) {
    return new ModuleLayerToolbox(layer);
  }

  static Toolbox ofModulePath(Path... entries) {
    return new ModulePathToolbox(entries);
  }

  static Toolbox ofNativeToolsInJavaHome(String... names) {
    var bin = Path.of(System.getProperty("java.home"), "bin");
    return ofNativeTools(name -> "java.home/" + name, bin, List.of(names));
  }

  static Toolbox ofNativeTools(UnaryOperator<String> renamer, Path directory, List<String> names) {
    var tools = new ArrayList<Tool>();
    for (var name : names) {
      var executable = directory.resolve(name);
      var renamed = renamer.apply(name);
      var command = List.of(executable.toString());
      var provider = new NativeProcessToolProvider(renamed, command);
      tools.add(Tool.of(renamed, provider));
    }
    return Toolbox.of(tools);
  }
}
