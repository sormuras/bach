package com.github.sormuras.bach.project;

import com.github.sormuras.bach.Project;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

public record ProjectSpaces(List<ProjectSpace> values)
    implements Project.Component, Iterable<ProjectSpace> {

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
    return with(operator.apply(ProjectSpace.of(name)));
  }

  public ProjectSpaces withSpace(String name, Set<String> parents, ProjectSpace.Operator operator) {
    var parentSpaces = values.stream().filter(space -> parents.contains(space.name())).toList();
    var space = ProjectSpace.of(name).withParents(parentSpaces);
    return with(operator.apply(space));
  }

  public ProjectSpaces tweakModule(DeclaredModule.Tweak tweak) {
    return new ProjectSpaces(values.stream().map(space -> space.tweak(tweak)).toList());
  }
}
