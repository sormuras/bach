package run.duke;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import run.duke.internal.CollectionToolbox;
import run.duke.internal.CompositeToolbox;
import run.duke.internal.ModuleLayerToolbox;

@FunctionalInterface
public interface Toolbox {
  Collection<Tool> tools();

  default Optional<Tool> find(String tool) {
    return tools().stream().filter(info -> info.test(tool)).findFirst();
  }

  static Toolbox empty() {
    return new CollectionToolbox(List.of());
  }

  static Toolbox compose(Toolbox... toolboxes) {
    return new CompositeToolbox(List.of(toolboxes));
  }

  static Toolbox of(Tool tool, Tool... more) {
    return new CollectionToolbox(Stream.concat(Stream.of(tool), Stream.of(more)).toList());
  }

  static Toolbox ofModuleLayer(ModuleLayer layer) {
    return new ModuleLayerToolbox(layer);
  }
}
