package com.github.sormuras.bach.project;

import com.github.sormuras.bach.internal.ModuleInfoFinder;
import com.github.sormuras.bach.internal.ModuleInfoReference;
import java.lang.module.ModuleFinder;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/** A sequence of declared modules. */
public record DeclaredModules(List<DeclaredModule> list) implements Iterable<DeclaredModule> {

  public static DeclaredModules of(DeclaredModule... modules) {
    return of(List.of(modules));
  }

  public static DeclaredModules of(List<DeclaredModule> modules) {
    return new DeclaredModules(modules.stream().sorted().toList());
  }

  public Optional<DeclaredModule> find(String name) {
    return list.stream().filter(module -> module.name().equals(name)).findFirst();
  }

  @Override
  public Iterator<DeclaredModule> iterator() {
    return list.iterator();
  }

  public List<String> names() {
    return list.stream().map(DeclaredModule::name).toList();
  }

  public ModuleFinder toModuleFinder() {
    var moduleInfoReferences =
        list.stream()
            .map(module -> new ModuleInfoReference(module.info(), module.descriptor()))
            .toList();
    return new ModuleInfoFinder(moduleInfoReferences);
  }

  public DeclaredModules with(DeclaredModule... more) {
    var stream = Stream.concat(list.stream(), Stream.of(more)).sorted();
    return new DeclaredModules(stream.toList());
  }
}
