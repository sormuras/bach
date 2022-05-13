package com.github.sormuras.bach.project;

import java.util.Objects;

public record ProjectName(String value) implements ProjectComponent {
  public ProjectName {
    Objects.requireNonNull(value);
  }

  @Override
  public String toString() {
    return value;
  }
}
