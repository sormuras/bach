package com.github.sormuras.bach;

import java.nio.file.Path;

public record Paths(Path root, Path out) {
  public Path root(String first, String... more) {
    return root.resolve(Path.of(first, more));
  }

  public Path out(String first, String... more) {
    return out.resolve(Path.of(first, more));
  }
}
