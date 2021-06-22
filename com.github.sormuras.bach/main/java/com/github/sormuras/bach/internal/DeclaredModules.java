package com.github.sormuras.bach.internal;

import com.github.sormuras.bach.Project;
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

public record DeclaredModules(ModuleFinder finder) {
  public static DeclaredModules of(Set<Project.Module> modules) {
    var map = new TreeMap<String, Reference>();
    for (var module : modules) map.put(module.name(), new Reference(module));
    return new DeclaredModules(new Finder(map));
  }

  public Stream<ModuleDescriptor> descriptors() {
    return finder().findAll().stream().sorted().map(ModuleReference::descriptor);
  }

  public Stream<String> names() {
    return descriptors().map(ModuleDescriptor::name);
  }

  private record Finder(Map<String, Reference> map) implements ModuleFinder {

    @Override
    public Optional<ModuleReference> find(String name) {
      return Optional.ofNullable(map.get(name));
    }

    @Override
    public Set<ModuleReference> findAll() {
      return Set.copyOf(map.values());
    }
  }

  private static final class Reference extends ModuleReference implements Comparable<Reference> {

    private Reference(Project.Module module) {
      super(module.descriptor(), module.location());
    }

    @Override
    public int compareTo(Reference other) {
      return descriptor().name().compareTo(other.descriptor().name());
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
