package com.github.sormuras.bach;

import java.nio.file.Path;

public record Base(Path directory, Path externals, Path workspace) {

  public static Base of(Path directory) {
    var externals = directory.resolve(Bach.EXTERNALS);
    var workspace = directory.resolve(Bach.WORKSPACE);
    return new Base(directory, externals, workspace);
  }

  public Path directory(String first, String... more) {
    return directory.resolve(Path.of(first, more));
  }

  public Path workspace(String first, String... more) {
    return workspace.resolve(Path.of(first, more));
  }
}
