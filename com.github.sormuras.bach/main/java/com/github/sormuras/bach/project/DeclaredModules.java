package com.github.sormuras.bach.project;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/** A collection of declared modules. */
public record DeclaredModules(Collection<DeclaredModule> values) {
  public static DeclaredModules of(DeclaredModule... modules) {
    return new DeclaredModules(Stream.of(modules).sorted().toList());
  }

  public DeclaredModules add(DeclaredModule module) {
    var values = new ArrayList<>(this.values);
    values.add(module);
    Collections.sort(values);
    return new DeclaredModules(List.copyOf(values));
  }
}
