package com.github.sormuras.bach.internal;

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Requires;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Module-related utilities. */
public class Modules {

  public static Set<String> declared(ModuleFinder finder) {
    return declared(finder.findAll().stream().map(ModuleReference::descriptor));
  }

  public static Set<String> declared(Stream<ModuleDescriptor> descriptors) {
    return descriptors.map(ModuleDescriptor::name).collect(Collectors.toCollection(TreeSet::new));
  }

  public static Set<String> required(ModuleFinder finder) {
    return required(finder.findAll().stream().map(ModuleReference::descriptor));
  }

  public static Set<String> required(Stream<ModuleDescriptor> descriptors) {
    return descriptors
        .map(ModuleDescriptor::requires)
        .flatMap(Set::stream)
        .filter(requires -> !requires.modifiers().contains(Requires.Modifier.MANDATED))
        .filter(requires -> !requires.modifiers().contains(Requires.Modifier.STATIC))
        .filter(requires -> !requires.modifiers().contains(Requires.Modifier.SYNTHETIC))
        .map(Requires::name)
        .collect(Collectors.toCollection(TreeSet::new));
  }

  public static ModuleLayer layer(ModuleFinder finder) {
    var boot = ModuleLayer.boot();
    var configuration = boot.configuration().resolveAndBind(ModuleFinder.of(), finder, Set.of());
    var parent = Modules.class.getClassLoader();
    var controller = ModuleLayer.defineModulesWithOneLoader(configuration, List.of(boot), parent);
    return controller.layer();
  }

  /** Hide default constructor. */
  private Modules() {}
}
