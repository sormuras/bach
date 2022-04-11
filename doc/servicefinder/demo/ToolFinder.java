import static java.lang.ModuleLayer.defineModulesWithOneLoader;
import static java.lang.module.Configuration.resolveAndBind;

import java.lang.module.ModuleFinder;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.spi.ToolProvider;

/** A finder of {@link ToolProvider tools}. */
@FunctionalInterface
interface ToolFinder extends ServiceFinder<ToolProvider> {

  @Override
  default String nameOf(ToolProvider provider) {
    return provider.name();
  }

  static ToolFinder of(ToolProvider... providers) {
    return new DirectServiceFinder<>(List.of(providers))::findAll;
  }

  static ToolFinder of(ModuleFinder finder, boolean assertions, String... roots) {
    var parentClassLoader = ClassLoader.getPlatformClassLoader();
    var parentModuleLayer = ModuleLayer.boot();
    var parents = List.of(parentModuleLayer.configuration());
    var configuration = resolveAndBind(ModuleFinder.of(), parents, finder, Set.of(roots));
    var layers = List.of(parentModuleLayer);
    var controller = defineModulesWithOneLoader(configuration, layers, parentClassLoader);
    var layer = controller.layer();
    if (assertions) for (var root : roots) layer.findLoader(root).setDefaultAssertionStatus(true);
    return ToolFinder.of(layer);
  }

  static ToolFinder of(ModuleLayer layer) {
    var loader = ServiceLoader.load(layer, ToolProvider.class);
    return new ModuleLayerServiceFinder<>(layer, loader)::findAll;
  }

  static ToolFinder of(ClassLoader loader) {
    return ToolFinder.of(ServiceLoader.load(ToolProvider.class, loader));
  }

  static ToolFinder of(ServiceLoader<ToolProvider> loader) {
    return new ServiceLoaderServiceFinder<>(loader)::findAll;
  }

  static ToolFinder ofSystem() {
    return ToolFinder.of(ClassLoader.getSystemClassLoader());
  }

  static ToolFinder compose(ToolFinder... finders) {
    return new CompositeServiceFinder<>(List.of(finders))::findAll;
  }
}
