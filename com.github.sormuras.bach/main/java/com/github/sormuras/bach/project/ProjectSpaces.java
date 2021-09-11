package com.github.sormuras.bach.project;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

public record ProjectSpaces(List<ProjectSpace> values) {

  @FunctionalInterface
  public interface Operator extends UnaryOperator<ProjectSpaces> {}

  public ProjectSpace main() {
    return find("main").orElseThrow();
  }

  public Optional<ProjectSpace> find(String name) {
    return values.stream().filter(space -> space.name().equals(name)).findFirst();
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
