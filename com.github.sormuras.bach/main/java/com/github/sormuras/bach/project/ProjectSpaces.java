package com.github.sormuras.bach.project;

import java.util.stream.Stream;

public record ProjectSpaces(ProjectSpace main, ProjectSpace test) {

  public Stream<ProjectSpace> stream() {
    return Stream.of(main, test);
  }
}
