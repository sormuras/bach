package com.github.sormuras.bach.trait;

import com.github.sormuras.bach.Trait;
import com.github.sormuras.bach.internal.ToolProviders;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.util.List;
import java.util.StringJoiner;
import java.util.spi.ToolProvider;

public /*sealed*/ interface PrintTrait extends Trait {

  default void printModules() {
    printDeclaredModules();
    printExternalModules();
    printSystemModules();
  }

  default void printDeclaredModules() {
    var out = bach().logbook().printer().out();
    out.println("Declared Modules");

    var main = bach().project().spaces().main().modules();
    var test = bach().project().spaces().test().modules();
    if (main.isEmpty() && test.isEmpty()) {
      out.println("  No modules declared by project " + bach().project().name());
      return;
    }
    out.println("  Main Code Space");
    out.println(toString(main).indent(4).stripTrailing());
    out.println("  Test Code Space");
    out.println(toString(test).indent(4).stripTrailing());
  }

  default void printExternalModules() {
    var out = bach().logbook().printer().out();
    out.println("External Modules");
    var finder = ModuleFinder.of(bach().project().folders().externals());
    out.println(toString(finder).indent(2).stripTrailing());
  }

  default void printSystemModules() {
    var out = bach().logbook().printer().out();
    out.println("System Modules");
    var finder = ModuleFinder.ofSystem();
    out.println(toString(finder).indent(2).stripTrailing());
  }

  private String toString(ModuleFinder finder) {
    return toString(finder.findAll().stream().map(ModuleReference::descriptor).toList());
  }

  private String toString(List<ModuleDescriptor> descriptors) {
    var joiner = new StringJoiner("\n");
    descriptors.stream().map(ModuleDescriptor::toNameAndVersion).sorted().forEach(joiner::add);
    var size = descriptors.size();
    var s = size == 1 ? "" : "s";
    joiner.add("  %d module%s".formatted(size, s));
    return joiner.toString();
  }

  default void printTools() {
    var folders = bach().project().folders();
    var before = ModuleFinder.of(folders.externals());
    var after = ModuleFinder.ofSystem();
    var providers = new ToolProviders(before, after, folders.tools());
    printTools(providers.stream().toList());
  }

  private void printTools(List<ToolProvider> providers) {
    var out = bach().logbook().printer().out();
    providers.stream()
        .map(ToolProviders::nameAndModule)
        .sorted()
        .forEach(bach().logbook().printer().out()::println);
    var size = providers.size();
    var s = size == 1 ? "" : "s";
    out.println("  %d tool%s".formatted(size, s));
  }

  default void printToolDescription(String name) {
    var out = bach().logbook().printer().out();
    var folders = bach().project().folders();
    ToolProviders.of(ModuleFinder.of(folders.externals()), folders.tools())
        .find(name)
        .map(ToolProviders::describe)
        .ifPresentOrElse(out::println, () -> out.println(name + " not found"));
  }
}
