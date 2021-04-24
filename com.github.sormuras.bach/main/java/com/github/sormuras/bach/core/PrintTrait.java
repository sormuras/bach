package com.github.sormuras.bach.core;

import java.lang.module.ModuleFinder;

public interface PrintTrait extends BachTrait {
  default void printToolListing() {
    var out = bach().logbook().printer().out();
    providers().stream().map(ToolProviders::nameAndModule).sorted().forEach(out::println);
  }

  default void printToolDescription(String name) {
    var out = bach().logbook().printer().out();
    providers()
        .find(name)
        .map(ToolProviders::describe)
        .ifPresentOrElse(out::println, () -> out.println(name + " not found"));
  }

  private ToolProviders providers() {
    return ToolProviders.of(ModuleFinder.of(bach().project().folders().externals()));
  }
}
