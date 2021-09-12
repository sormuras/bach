package com.github.sormuras.bach.project;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

public record ProjectSpaces(List<ProjectSpace> values) implements Iterable<ProjectSpace> {

  @FunctionalInterface
  public interface Operator extends UnaryOperator<ProjectSpaces> {}

  public Optional<ProjectSpace> find(String name) {
    return values.stream().filter(space -> space.name().equals(name)).findFirst();
  }

  @Override
  public Iterator<ProjectSpace> iterator() {
    return values.iterator();
  }

  public ProjectSpaces with(ProjectSpace value) {
    var values = new ArrayList<>(values());
    values.add(value);
    return new ProjectSpaces(List.copyOf(values));
  }

  public ProjectSpaces withSpace(String name, ProjectSpace.Operator operator) {
    return with(operator.apply(new ProjectSpace(name, List.of(), DeclaredModules.of())));
  }

  public ProjectSpaces withSpace(
      String name, Set<String> parentSpaceNames, ProjectSpace.Operator operator) {
    var parents = values.stream().filter(space -> parentSpaceNames.contains(space.name())).toList();
    var space = new ProjectSpace(name, parents, DeclaredModules.of());
    return with(operator.apply(space));
  }

  public ProjectSpaces tweakModule(DeclaredModule.Tweak tweak) {
    return new ProjectSpaces(values.stream().map(space -> space.tweak(tweak)).toList());
  }
}
