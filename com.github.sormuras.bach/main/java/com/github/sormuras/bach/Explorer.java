package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.ModuleInfoFinder;
import com.github.sormuras.bach.internal.PathSupport;

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Requires;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record Explorer(Bach bach) {

  public ModuleFinder newModuleInfoFinder(Path root) {
    return ModuleInfoFinder.of(root);
  }

  public List<Path> findJavaFiles() {
    return findJavaFiles(bach.path().root());
  }

  public List<Path> findJavaFiles(Path root) {
    return PathSupport.find(root, 99, PathSupport::isJavaFile);
  }

  public List<Path> findModuleInfoJavaFiles() {
    return findModuleInfoJavaFiles(bach.path().root());
  }

  public List<Path> findModuleInfoJavaFiles(Path root) {
    return PathSupport.find(root, 99, PathSupport::isModuleInfoJavaFile);
  }

  public List<String> listMissingExternalModules(String... more) {
    return listMissingExternalModules(ModuleFinder.of(bach.path().externalModules()), more);
  }

  public List<String> listMissingExternalModules(ModuleFinder finder, String... more) {
    return listMissingExternalModules(List.of(finder), more);
  }

  public List<String> listMissingExternalModules(List<ModuleFinder> finders, String... more) {
    // Populate a set with all module names being in a "requires MODULE;" directive
    var requires = new TreeSet<>(List.of(more)); // more required modules
    for (var finder : finders) requires.addAll(required(finder)); // main, test, and other modules
    // Remove names of declared modules from various module finders
    requires.removeAll(declared(ModuleFinder.ofSystem()));
    for (var finder : finders) requires.removeAll(declared(finder));
    return List.copyOf(requires);
  }

  public static TreeSet<String> declared(ModuleFinder finder) {
    return declared(finder.findAll().stream().map(ModuleReference::descriptor));
  }

  public static TreeSet<String> declared(Stream<ModuleDescriptor> descriptors) {
    return descriptors.map(ModuleDescriptor::name).collect(Collectors.toCollection(TreeSet::new));
  }

  public static TreeSet<String> required(ModuleFinder finder) {
    return required(finder.findAll().stream().map(ModuleReference::descriptor));
  }

  public static TreeSet<String> required(Stream<ModuleDescriptor> descriptors) {
    return descriptors
        .map(ModuleDescriptor::requires)
        .flatMap(Set::stream)
        .filter(requires -> !requires.modifiers().contains(Requires.Modifier.MANDATED))
        .filter(requires -> !requires.modifiers().contains(Requires.Modifier.STATIC))
        .filter(requires -> !requires.modifiers().contains(Requires.Modifier.SYNTHETIC))
        .map(Requires::name)
        .collect(Collectors.toCollection(TreeSet::new));
  }
}
