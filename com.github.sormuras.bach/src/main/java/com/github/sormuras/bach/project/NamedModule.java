package com.github.sormuras.bach.project;

import java.lang.module.ModuleDescriptor;

public sealed interface NamedModule permits DeclaredModule {
  ModuleDescriptor descriptor();

  default String name() {
    return descriptor().name();
  }
}
