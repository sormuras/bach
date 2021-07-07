package com.github.sormuras.bach.project;

import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

public record DeclaredModules(Set<DeclaredModule> set) {
  public static DeclaredModules of(DeclaredModule... modules) {
    return new DeclaredModules(new TreeSet<>(Stream.of(modules).toList()));
  }

  public boolean isEmpty() {
    return set.isEmpty();
  }

  public DeclaredModules with(DeclaredModule module) {
    var set = new TreeSet<>(this.set);
    set.add(module);
    return new DeclaredModules(set);
  }

  public ModuleSourcePaths toModuleSourcePaths() {
    return isEmpty() ? ModuleSourcePaths.ofPatterns(".") : ModuleSourcePaths.of(this);
  }
}
