package com.github.sormuras.bach.core;

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.spi.ToolProvider;

public /*sealed*/ interface PrintTrait extends BachTrait {
  default void printModules() {
    printModules(EnumSet.allOf(ModuleRealm.class));
  }

  default void printDeclaredModules() {
    printModules(EnumSet.of(ModuleRealm.DECLARED));
  }

  default void printExternalModules() {
    printModules(EnumSet.of(ModuleRealm.EXTERNAL));
  }

  default void printSystemModules() {
    printModules(EnumSet.of(ModuleRealm.SYSTEM));
  }

  default void printLayerModules() {
    printModules(getClass().getModule().getLayer());
  }

  default void printModules(Set<ModuleRealm> origins) {
    var out = bach().logbook().printer().out();
    var finders = new ArrayList<ModuleFinder>();
    if (origins.contains(ModuleRealm.DECLARED)) out.printf("TODO %s%n", ModuleRealm.DECLARED);
    if (origins.contains(ModuleRealm.EXTERNAL)) finders.add(ModuleFinder.of(bach().project().folders().externals()));
    if (origins.contains(ModuleRealm.SYSTEM)) finders.add(ModuleFinder.ofSystem());
    printModules(ModuleFinder.compose(finders.toArray(ModuleFinder[]::new)));
  }

  default void printModules(ModuleFinder finder) {
    printModules(finder.findAll().stream().map(ModuleReference::descriptor).toList());
  }

  default void printModules(ModuleLayer layer) {
    printModules(layer.modules().stream().map(Module::getDescriptor).filter(Objects::nonNull).toList());
  }

  default void printModules(List<ModuleDescriptor> descriptors) {
    var out = bach().logbook().printer().out();
    descriptors.stream()
        .map(ModuleDescriptor::toNameAndVersion)
        .sorted()
        .forEach(out::println);
    var size = descriptors.size();
    var s = size == 1 ? "" : "s";
    out.println("  %d module%s".formatted(size, s));
  }

  default void printTools() {
    var before = ModuleFinder.of(bach().project().folders().externals());
    var after = ModuleFinder.ofSystem();
    var providers = new ToolProviders(before, after);
    printTools(providers.stream().toList());
  }

  private void printTools(List<ToolProvider> providers) {
    var out = bach().logbook().printer().out();
    providers
        .stream()
        .map(ToolProviders::nameAndModule)
        .sorted()
        .forEach(bach().logbook().printer().out()::println);
    var size = providers.size();
    var s = size == 1 ? "" : "s";
    out.println("  %d tool%s".formatted(size, s));
  }

  default void printToolDescription(String name) {
    var out = bach().logbook().printer().out();
    ToolProviders.of(ModuleFinder.of(bach().project().folders().externals()))
        .find(name)
        .map(ToolProviders::describe)
        .ifPresentOrElse(out::println, () -> out.println(name + " not found"));
  }
}
