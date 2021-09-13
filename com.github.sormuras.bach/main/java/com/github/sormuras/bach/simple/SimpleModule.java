package com.github.sormuras.bach.simple;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Describes a source module in a conventional "simple" project space. */
public record SimpleModule(String name, Optional<String> main, List<Path> resources) {
  public static SimpleModule of(String name) {
    return new SimpleModule(name, Optional.empty(), List.of());
  }

  public SimpleModule main(String mainClassName) {
    return new SimpleModule(name, Optional.ofNullable(mainClassName), resources);
  }

  public SimpleModule withResourcePath(String first, String... more) {
    return withResourcePath(Path.of(first, more));
  }

  public SimpleModule withResourcePath(Path path) {
    var resources = new ArrayList<>(this.resources);
    resources.add(path);
    return new SimpleModule(name, main, resources);
  }
}
