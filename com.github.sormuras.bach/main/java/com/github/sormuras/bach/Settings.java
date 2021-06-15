package com.github.sormuras.bach;

public record Settings(Workflows workflows) {
  public static Settings of() {
    return new Settings(Workflows.of());
  }
}
