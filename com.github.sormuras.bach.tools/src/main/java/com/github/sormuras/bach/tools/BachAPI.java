package com.github.sormuras.bach.tools;

import com.github.sormuras.bach.project.Project;

public interface BachAPI {

  default void banner(String text) {
    run(Command.of("banner").with(text));
  }

  Project project();

  void run(Command command);
}
