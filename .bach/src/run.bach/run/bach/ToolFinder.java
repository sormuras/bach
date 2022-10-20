package run.bach;

import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.spi.ToolProvider;
import run.bach.internal.ModulesSupport;
import run.bach.toolfinder.ArrayToolFinder;
import run.bach.toolfinder.JavaProgramsToolFinder;
import run.bach.toolfinder.ServiceLoaderToolFinder;
import run.bach.toolfinder.SupplierToolFinder;

@FunctionalInterface
public interface ToolFinder {
  List<Tool> findAll();

  default Optional<Tool> findFirst(String string) {
    return findAll().stream().filter(tool -> tool.matches(string)).findFirst();
  }

  default String description() {
    return getClass().getSimpleName();
  }

  static ToolFinder ofTools(String description, List<Tool> tools) {
    return new ArrayToolFinder(description, List.copyOf(tools));
  }

  static ToolFinder ofNativeTools(
      String description,
      UnaryOperator<String> renamer,
      Path directory,
      String tool,
      String... more) {
    var names = new ArrayList<String>();
    names.add(tool);
    names.addAll(List.of(more));
    var tools = new ArrayList<Tool>();
    for (var name : names) {
      var executable = directory.resolve(name);
      tools.add(Tool.ofNativeProcess(renamer.apply(name), List.of(executable.toString())));
    }
    return ToolFinder.ofTools(description, tools);
  }

  static ToolFinder ofJavaPrograms(String description, Path directory, Path java) {
    return new JavaProgramsToolFinder(description, directory, java);
  }

  static ToolFinder ofToolProviders(String description, Path... paths) {
    return ofSupplier(description, () -> ofModuleFinder(ModuleFinder.of(paths), false));
  }

  static ToolFinder ofModuleFinder(ModuleFinder finder, boolean assertions, String... roots) {
    var layer = ModulesSupport.buildModuleLayer(finder, roots);
    if (assertions) for (var root : roots) layer.findLoader(root).setDefaultAssertionStatus(true);
    return ofModuleLayer(layer);
  }

  static ToolFinder ofModuleLayer(ModuleLayer layer) {
    var loader = ServiceLoader.load(layer, ToolProvider.class);
    return new ServiceLoaderToolFinder(layer, loader);
  }

  static ToolFinder ofSupplier(String description, Supplier<ToolFinder> supplier) {
    return new SupplierToolFinder(description, supplier);
  }
}
