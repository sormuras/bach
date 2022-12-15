package run.duke.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Predicate;
import java.util.spi.ToolProvider;
import run.duke.Tool;
import run.duke.ToolOperator;
import run.duke.Toolbox;

public record ModuleLayerToolbox(ModuleLayer layer, Predicate<Module> include) implements Toolbox {
  public ModuleLayerToolbox(ModuleLayer layer) {
    this(layer, module -> true);
  }

  @Override
  public List<Tool> tools() {
    var tools = new ArrayList<Tool>();
    ServiceLoader.load(layer, ToolOperator.class).stream()
        .filter(service -> include.test(service.type().getModule()))
        .map(ServiceLoader.Provider::get)
        .map(Tool::of)
        .forEach(tools::add);
    ServiceLoader.load(layer, ToolProvider.class).stream()
        .filter(service -> include.test(service.type().getModule()))
        .map(ServiceLoader.Provider::get)
        .map(Tool::of)
        .forEach(tools::add);
    return List.copyOf(tools);
  }
}
