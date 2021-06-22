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

public final class DeclaredModules {

  public static DeclaredModules of(Set<Project.Module> modules) {
    return new DeclaredModules(Finder.of(modules));
  }

  private final Finder finder;

  public DeclaredModules(Finder finder) {
    this.finder = finder;
  }

  public boolean isEmpty() {
    return finder.map.isEmpty();
  }

  public Stream<ModuleDescriptor> descriptors() {
    return finder.map.values().stream().map(ModuleReference::descriptor);
  }

  public Stream<String> names() {
    return descriptors().map(ModuleDescriptor::name);
  }

  private record Finder(Map<String, Reference> map) implements ModuleFinder {

    private static Finder of(Set<Project.Module> modules) {
      var map = new TreeMap<String, Reference>();
      for (var module : modules) map.put(module.name(), new Reference(module));
      return new Finder(map);
    }

    @Override
    public Optional<ModuleReference> find(String name) {
      return Optional.ofNullable(map.get(name));
    }

    @Override
    public Set<ModuleReference> findAll() {
      return Set.copyOf(map.values());
    }
  }

  private static final class Reference extends ModuleReference {

    private Reference(Project.Module module) {
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
