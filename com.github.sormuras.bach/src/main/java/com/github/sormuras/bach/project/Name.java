package com.github.sormuras.bach.project;

import java.util.Objects;

public record Name(String value) implements Project.Component {
  public Name {
    Objects.requireNonNull(value);
  }

  @Override
  public String toString() {
    return value;
  }
}
