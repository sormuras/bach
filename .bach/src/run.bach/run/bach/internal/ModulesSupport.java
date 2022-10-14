package run.bach.internal;

import static java.lang.ModuleLayer.defineModulesWithOneLoader;
import static java.lang.module.Configuration.resolveAndBind;

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface ModulesSupport {
  static ModuleLayer buildModuleLayer(ModuleFinder finder, String... roots) {
    var parentClassLoader = ModulesSupport.class.getClassLoader();
    var parentModuleLayer = ModuleLayer.boot();
    var parents = List.of(parentModuleLayer.configuration());
    var configuration = resolveAndBind(ModuleFinder.of(), parents, finder, Set.of(roots));
    var layers = List.of(parentModuleLayer);
    var controller = defineModulesWithOneLoader(configuration, layers, parentClassLoader);
    return controller.layer();
  }

  static void consumeAllNames(ModuleFinder finder, Consumer<String> consumer) {
    finder.findAll().stream()
        .map(ModuleReference::descriptor)
        .map(ModuleDescriptor::toNameAndVersion)
        .sorted()
        .map(string -> string.indent(2).stripTrailing())
        .forEach(consumer);
  }

  static List<String> listMissingNames(List<ModuleFinder> finders, Set<String> more) {
    // Populate a set with all module names being in a "requires MODULE;" directive
    var requires = new TreeSet<>(more); // more required modules
    for (var finder : finders) requires.addAll(required(finder)); // main, test, and others
    // Remove names of declared modules from various module finders
    requires.removeAll(declared(ModuleFinder.ofSystem()));
    for (var finder : finders) requires.removeAll(declared(finder));
    return List.copyOf(requires);
  }

  static TreeSet<String> declared(ModuleFinder finder) {
    return declared(finder.findAll().stream().map(ModuleReference::descriptor));
  }

  static TreeSet<String> declared(Stream<ModuleDescriptor> descriptors) {
    return descriptors.map(ModuleDescriptor::name).collect(Collectors.toCollection(TreeSet::new));
  }

  static TreeSet<String> required(ModuleFinder finder) {
    return required(finder.findAll().stream().map(ModuleReference::descriptor));
  }

  static TreeSet<String> required(Stream<ModuleDescriptor> descriptors) {
    return descriptors
        .map(ModuleDescriptor::requires)
        .flatMap(Set::stream)
        .filter(ModulesSupport::required)
        .map(ModuleDescriptor.Requires::name)
        .collect(Collectors.toCollection(TreeSet::new));
  }

  static boolean required(ModuleDescriptor.Requires requires) {
    var modifiers = requires.modifiers();
    if (modifiers.isEmpty() || modifiers.contains(ModuleDescriptor.Requires.Modifier.TRANSITIVE))
      return true;
    if (modifiers.contains(ModuleDescriptor.Requires.Modifier.MANDATED)) return false;
    if (modifiers.contains(ModuleDescriptor.Requires.Modifier.SYNTHETIC)) return false;
    return !modifiers.contains(ModuleDescriptor.Requires.Modifier.STATIC);
  }
}
