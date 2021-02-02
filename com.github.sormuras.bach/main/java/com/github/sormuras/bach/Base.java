package com.github.sormuras.bach;

import java.nio.file.Path;

public record Base(Path directory, Path externals, Path workspace) {

  public static Base ofSystem() {
    return of(Path.of(""));
  }

  public static Base of(Path path) {
    return new Base(path, path.resolve(Bach.EXTERNALS), path.resolve(Bach.WORKSPACE));
  }

  public Path directory(String first, String... more) {
    return directory.resolve(Path.of(first, more));
  }

  public Path workspace(String first, String... more) {
    return workspace.resolve(Path.of(first, more));
  }
}
