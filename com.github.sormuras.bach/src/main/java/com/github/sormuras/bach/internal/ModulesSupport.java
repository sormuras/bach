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

public interface ModulesSupport {
  static List<String> listMissingModules(List<ModuleFinder> finders, Set<String> more) {
    // Populate a set with all module names being in a "requires MODULE;" directive
    var requires = new TreeSet<>(more); // more required modules
    for (var finder : finders) requires.addAll(required(finder)); // main, test, and other modules
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
        .map(Requires::name)
        .collect(Collectors.toCollection(TreeSet::new));
  }

  static boolean required(Requires requires) {
    var modifiers = requires.modifiers();
    if (modifiers.isEmpty() || modifiers.contains(Requires.Modifier.TRANSITIVE)) return true;
    if (modifiers.contains(Requires.Modifier.MANDATED)) return false;
    if (modifiers.contains(Requires.Modifier.SYNTHETIC)) return false;
    return !modifiers.contains(Requires.Modifier.STATIC);
  }
}
