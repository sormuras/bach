package com.github.sormuras.bach;

public interface Checkpoint {
  Workflow workflow();

  @FunctionalInterface
  interface Handler {
    void handle(Checkpoint checkpoint);
  }
}
