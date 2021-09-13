package com.github.sormuras.bach.project;

import java.nio.file.Path;
import java.util.List;
import java.util.function.UnaryOperator;

public record ProjectSpace(String name, List<ProjectSpace> parents, DeclaredModules modules) {

  @FunctionalInterface
  public interface Operator extends UnaryOperator<ProjectSpace> {}

  public DeclaredModule module(String name) {
    return modules.find(name).orElseThrow();
  }

  public ProjectSpace withModule(DeclaredModule module) {
    return new ProjectSpace(name, parents, modules.with(module));
  }

  public ProjectSpace withModule(String info) {
    return withModule(Path.of(info), module -> module);
  }

  public ProjectSpace withModule(String info, String main) {
    return withModule(Path.of(info), module -> module.withMainClass(main));
  }

  public ProjectSpace withModule(Path info, DeclaredModule.Operator operator) {
    return withModule(operator.apply(DeclaredModule.of(info.toString())));
  }

  ProjectSpace tweak(DeclaredModule.Tweak tweak) {
    return this == tweak.space() ? new ProjectSpace(name, parents, modules.tweak(tweak)) : this;
  }
}
