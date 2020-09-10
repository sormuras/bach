package test.modules;

import de.sormuras.bach.ToolShell;
import java.lang.module.FindException;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.module.ResolutionException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.StringJoiner;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;

class TestShell extends ToolShell {
  ToolProvider javac() {
    return computeToolProvider("javac", "java.base", List.of());
  }

  Stream<ToolProvider> providers() {
    return computeToolProviders("java.base", List.of());
  }

  protected ToolProvider computeToolProvider(String name, String module, List<Path> modulePaths) {
    return computeToolProvider(name, computeToolProviders(module, modulePaths));
  }

  /** Return all tool providers found by resolving the specified module. */
  protected Stream<ToolProvider> computeToolProviders(String module, List<Path> modulePaths) {
    var roots = Set.of(module);
    var finder = ModuleFinder.of(modulePaths.toArray(Path[]::new));
    var parent = ClassLoader.getSystemClassLoader();
    try {
      var boot = ModuleLayer.boot();
      var configuration = boot.configuration().resolveAndBind(finder, ModuleFinder.of(), roots);
      var controller = ModuleLayer.defineModulesWithOneLoader(configuration, List.of(boot), parent);
      var layer = controller.layer();
      var loader = layer.findLoader(module);
      if (loader != null) loader.setDefaultAssertionStatus(true);
      var serviceLoader = ServiceLoader.load(layer, ToolProvider.class);
      return serviceLoader.stream().map(ServiceLoader.Provider::get);
    } catch (FindException | ResolutionException exception) {
      var message = new StringJoiner(System.lineSeparator());
      var modules = finder.findAll();
      message.add(exception.getMessage());
      message.add("Module path" + (modulePaths.isEmpty() ? " is empty" : ":"));
      modulePaths.forEach(path -> message.add("\t" + path));
      message.add("Finder found " + modules.size() + " module(s)");
      modules.stream()
          .sorted(Comparator.comparing(ModuleReference::descriptor))
          .forEach(reference -> message.add("\t" + reference));
      message.add("");
      throw new RuntimeException(message.toString(), exception);
    }
  }
}
