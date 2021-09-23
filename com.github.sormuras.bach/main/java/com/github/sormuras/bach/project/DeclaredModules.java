package com.github.sormuras.bach.project;

import com.github.sormuras.bach.internal.ModuleInfoFinder;
import com.github.sormuras.bach.internal.ModuleInfoReference;
import java.lang.module.ModuleFinder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/** A sequence of declared modules. */
public record DeclaredModules(List<DeclaredModule> values) implements Iterable<DeclaredModule> {

  public static DeclaredModules of(DeclaredModule... modules) {
    return new DeclaredModules(Stream.of(modules).sorted().toList());
  }

  public Optional<DeclaredModule> find(String name) {
    return values.stream().filter(module -> module.name().equals(name)).findFirst();
  }

  @Override
  public Iterator<DeclaredModule> iterator() {
    return values.iterator();
  }

  public List<String> names() {
    return values.stream().map(DeclaredModule::name).toList();
  }

  public ModuleFinder toModuleFinder() {
    var moduleInfoReferences =
        values.stream()
            .map(module -> new ModuleInfoReference(module.info(), module.descriptor()))
            .toList();
    return new ModuleInfoFinder(moduleInfoReferences);
  }

  public DeclaredModules with(DeclaredModule module) {
    var values = new ArrayList<>(this.values);
    values.add(module);
    Collections.sort(values);
    return new DeclaredModules(List.copyOf(values));
  }

  DeclaredModules tweak(DeclaredModule.Tweak tweak) {
    return new DeclaredModules(values.stream().map(module -> module.tweak(tweak)).toList());
  }
}
