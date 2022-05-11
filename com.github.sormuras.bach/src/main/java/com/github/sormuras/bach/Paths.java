package com.github.sormuras.bach;

import java.nio.file.Path;

public record Paths(Path root, Path out) {

  public static Paths ofCurrentWorkingDirectory() {
    return Paths.ofRoot("");
  }

  public static Paths ofRoot(String first, String... more) {
    var root = Path.of(first, more);
    return new Paths(root, root.resolve(".bach/out"));
  }

  public Path root(String first, String... more) {
    return root.resolve(Path.of(first, more));
  }

  public Path out(String first, String... more) {
    return out.resolve(Path.of(first, more));
  }

  public Path externalModules() {
    return root(".bach", "external-modules");
  }

  public Path externalTools() {
    return root(".bach", "external-tools");
  }

  public Path externalTools(String first, String... more) {
    return externalTools().resolve(Path.of(first, more));
  }
}
