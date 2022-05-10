package com.github.sormuras.bach.project;

import java.lang.module.ModuleFinder;
import java.util.ArrayList;
import java.util.List;

public record ProjectSpaces(ProjectSpace init, ProjectSpace main, ProjectSpace test)
    implements Project.Component {
  public List<ProjectSpace> list() {
    return List.of(init, main, test);
  }

  public ProjectSpaces with(ProjectSpace space) {
    return new ProjectSpaces(
        space.name().equals(init.name()) ? space : init,
        space.name().equals(main.name()) ? space : main,
        space.name().equals(test.name()) ? space : test);
  }

  public ProjectSpace space(String name) {
    return switch (name) {
      case "init" -> init;
      case "main" -> main;
      case "test" -> test;
      default -> throw new IllegalArgumentException("No such space: " + name);
    };
  }

  public ModuleFinder toModuleFinder() {
    var finders = new ArrayList<ModuleFinder>();
    finders.add(init.modules().toModuleFinder());
    finders.add(main.modules().toModuleFinder());
    finders.add(test.modules().toModuleFinder());
    return ModuleFinder.compose(finders.toArray(ModuleFinder[]::new));
  }
}