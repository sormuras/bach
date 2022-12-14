package run.duke.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Predicate;
import java.util.spi.ToolProvider;
import run.duke.Tool;
import run.duke.ToolFinder;
import run.duke.ToolOperator;

public record ModuleLayerToolFinder(
    Optional<String> description,
    ModuleLayer moduleLayer,
    Set<Class<?>> services,
    Predicate<Module> includeModulePredicate)
    implements ToolFinder {

  public ModuleLayerToolFinder(String description, ModuleLayer layer) {
    this(
        Optional.of(description),
        layer,
        Set.of(ToolFinder.class, ToolOperator.class, ToolProvider.class),
        module -> true);
  }

  @Override
  public List<Tool> findTools() {
    var tools = new ArrayList<Tool>();
    if (services.contains(ToolFinder.class))
      ServiceLoader.load(moduleLayer, ToolFinder.class).stream()
          .filter(service -> includeModulePredicate.test(service.type().getModule()))
          .map(ServiceLoader.Provider::get)
          .flatMap(finder -> finder.findTools().stream())
          .forEach(tools::add);
    if (services.contains(ToolOperator.class))
      ServiceLoader.load(moduleLayer, ToolOperator.class).stream()
          .filter(service -> includeModulePredicate.test(service.type().getModule()))
          .map(ServiceLoader.Provider::get)
          .map(Tool::of)
          .forEach(tools::add);
    if (services.contains(ToolProvider.class))
      ServiceLoader.load(moduleLayer, ToolProvider.class).stream()
          .filter(service -> includeModulePredicate.test(service.type().getModule()))
          .map(ServiceLoader.Provider::get)
          .map(Tool::of)
          .forEach(tools::add);
    return List.copyOf(tools);
  }
}
