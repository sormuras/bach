package de.sormuras.bach.api;

import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/** A module source description unit. */
public /*static*/ final class Unit {

  private final Path info;
  private final ModuleDescriptor descriptor;
  private final Path moduleSourcePath;
  private final List<Source> sources;
  private final List<Path> resources;

  public Unit(
      Path info,
      ModuleDescriptor descriptor,
      Path moduleSourcePath,
      List<Source> sources,
      List<Path> resources) {
    this.info = Objects.requireNonNull(info, "info");
    this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
    this.moduleSourcePath = Objects.requireNonNull(moduleSourcePath, "moduleSourcePath");
    this.sources = List.copyOf(sources);
    this.resources = List.copyOf(resources);
  }

  public Path info() {
    return info;
  }

  public ModuleDescriptor descriptor() {
    return descriptor;
  }

  public Path moduleSourcePath() {
    return moduleSourcePath;
  }

  public List<Source> sources() {
    return sources;
  }

  public List<Path> resources() {
    return resources;
  }

  public String name() {
    return descriptor.name();
  }

  public boolean isMainClassPresent() {
    return descriptor.mainClass().isPresent();
  }

  public <T> List<T> sources(Function<Source, T> mapper) {
    if (sources.isEmpty()) return List.of();
    if (sources.size() == 1) return List.of(mapper.apply(sources.get(0)));
    return sources.stream().map(mapper).collect(Collectors.toList());
  }

  public boolean isMultiRelease() {
    if (sources.isEmpty()) return false;
    if (sources.size() == 1) return sources.get(0).isTargeted();
    return sources.stream().allMatch(Source::isTargeted);
  }
}
