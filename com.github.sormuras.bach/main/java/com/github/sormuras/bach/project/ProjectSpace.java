package com.github.sormuras.bach.project;

import com.github.sormuras.bach.FindException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.UnaryOperator;

public record ProjectSpace(
    String name, List<ProjectSpace> parents, int release, DeclaredModules modules) {

  @FunctionalInterface
  public interface Operator extends UnaryOperator<ProjectSpace> {}

  public static ProjectSpace of(String name, ProjectSpace... parents) {
    return new ProjectSpace(name, List.of(parents), 0, DeclaredModules.of());
  }

  public DeclaredModule module(String name) {
    return modules.find(name).orElseThrow(() -> new FindException(DeclaredModule.class, name));
  }

  public ProjectSpace withParents(List<ProjectSpace> parents) {
    return new ProjectSpace(name, parents, release, modules);
  }

  public ProjectSpace withRelease(int release) {
    return new ProjectSpace(name, parents, release, modules);
  }

  public ProjectSpace withModule(DeclaredModule module) {
    return new ProjectSpace(name, parents, release, modules.with(module));
  }

  public ProjectSpace withModule(Path info) {
    return withModule(info, module -> module);
  }

  public ProjectSpace withModule(String info) {
    return withModule(Path.of(info), module -> module);
  }

  public ProjectSpace withModule(String info, String main) {
    return withModule(Path.of(info), module -> module.withMainClass(main));
  }

  public ProjectSpace withModule(String info, DeclaredModule.Operator operator) {
    return withModule(Path.of(info), operator);
  }

  public ProjectSpace withModule(Path info, DeclaredModule.Operator operator) {
    return withModule(operator.apply(DeclaredModule.of(info)));
  }

  ProjectSpace tweak(DeclaredModule.Tweak tweak) {
    return this == tweak.space()
        ? new ProjectSpace(name, parents, release, modules.tweak(tweak))
        : this;
  }
}
