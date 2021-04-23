package com.github.sormuras.bach.core;

import java.lang.module.ModuleFinder;

public interface ListTrait extends BachTrait {
  default void listTools() {
    var finder = ModuleFinder.of(bach().project().folders().externals());
    var providers = ToolProviders.of(finder);
    var out = bach().logbook().printer().out();
    providers.stream().map(ToolProviders::describe).sorted().forEach(out::println);
  }
}
