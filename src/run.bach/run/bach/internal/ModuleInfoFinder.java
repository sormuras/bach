package run.bach.internal;

import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public record ModuleInfoFinder(List<ModuleInfoReference> references) implements ModuleFinder {

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
