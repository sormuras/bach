package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.project.DeclaredModule;

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

public final class DeclaredModules implements ModuleFinder {

  public static DeclaredModules of(Set<DeclaredModule> modules) {
    return new DeclaredModules(modules);
  }

  private final Map<String, Reference> map;

  private DeclaredModules(Set<DeclaredModule> modules) {
    this.map = new TreeMap<>();
    for (var module : modules) map.put(module.name(), new Reference(module));
  }

  @Override
  public Optional<ModuleReference> find(String name) {
    return Optional.ofNullable(map.get(name));
  }

  @Override
  public Set<ModuleReference> findAll() {
    return Set.copyOf(map.values());
  }

  public boolean isEmpty() {
    return map.isEmpty();
  }

  public Stream<ModuleDescriptor> descriptors() {
    return map.values().stream().map(ModuleReference::descriptor);
  }

  public Stream<String> names() {
    return descriptors().map(ModuleDescriptor::name);
  }

  private static final class Reference extends ModuleReference {

    private Reference(DeclaredModule module) {
      super(module.descriptor(), module.location());
    }

    @Override
    public ModuleReader open() {
      return new EmptyModuleReader();
    }
  }

  private record EmptyModuleReader() implements ModuleReader {

    @Override
    public Optional<URI> find(String name) {
      return Optional.empty();
    }

    @Override
    public Stream<String> list() {
      return Stream.empty();
    }

    @Override
    public void close() {}
  }
}
