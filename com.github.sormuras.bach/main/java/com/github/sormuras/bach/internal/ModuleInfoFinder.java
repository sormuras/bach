package com.github.sormuras.bach.internal;

import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public record ModuleInfoFinder(List<ModuleInfoReference> references) implements ModuleFinder {

  public static ModuleInfoFinder of(Path root) {
    var infos = PathSupport.find(root, 99, PathSupport::isModuleInfoJavaFile);
    var references = infos.stream().map(ModuleInfoReference::new).toList();
    return new ModuleInfoFinder(references);
  }

  @Override
  public Optional<ModuleReference> find(String name) {
    return references.stream()
        .filter(reference -> reference.name().equals(name))
        .map(ModuleReference.class::cast)
        .findFirst();
  }

  @Override
  public Set<ModuleReference> findAll() {
    return Set.copyOf(references);
  }
}
