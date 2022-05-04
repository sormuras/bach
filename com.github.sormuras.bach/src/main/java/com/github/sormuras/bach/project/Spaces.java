package com.github.sormuras.bach.project;

import java.util.List;

public record Spaces(Space init, Space main, Space test) implements Project.Component {
  public List<Space> list() {
    return List.of(init, main, test);
  }

  Spaces with(Space space) {
    return new Spaces(
        space.name().equals(init.name()) ? space : init,
        space.name().equals(main.name()) ? space : main,
        space.name().equals(test.name()) ? space : test);
  }

  public Space space(String name) {
    return switch (name) {
      case "init" -> init;
      case "main" -> main;
      case "test" -> test;
      default -> throw new IllegalArgumentException("No such space: " + name);
    };
  }
}
