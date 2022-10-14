package run.bach.project;

import java.util.List;

public record ProjectSpaces(ProjectSpace init, ProjectSpace main, ProjectSpace test)
    implements ProjectComponent {
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
}
