package run.duke.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Predicate;
import java.util.spi.ToolProvider;
import run.duke.Tool;
import run.duke.ToolFinder;

public record ModuleLayerToolFinder(ModuleLayer layer, Predicate<Module> include)
    implements ToolFinder {
  @Override
  public List<Tool> tools() {
    var tools = new ArrayList<Tool>();
    ServiceLoader.load(layer, ToolProvider.class).stream()
        .filter(service -> include.test(service.type().getModule()))
        .map(ServiceLoader.Provider::get)
        .map(Tool::of)
        .forEach(tools::add);
    return List.copyOf(tools);
  }
}
