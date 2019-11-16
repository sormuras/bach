package de.sormuras.bach;

import de.sormuras.bach.task.BuildTask;

public interface Task {
  static Task build() {
    return new BuildTask();
  }

  void execute(Bach bach) throws Exception;
}
