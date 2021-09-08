package com.github.sormuras.bach.conventional;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Describes a source module in a conventional project space. */
public record ConventionalModule(String name, Optional<String> main, List<Path> resources) {
  public static ConventionalModule of(String name) {
    return new ConventionalModule(name, Optional.empty(), List.of());
  }

  public ConventionalModule main(String mainClassName) {
    return new ConventionalModule(name, Optional.ofNullable(mainClassName), resources);
  }

  public ConventionalModule resourcesAddPath(String first, String... more) {
    return resourcesAddPath(Path.of(first, more));
  }

  public ConventionalModule resourcesAddPath(Path path) {
    var resources = new ArrayList<>(this.resources);
    resources.add(path);
    return new ConventionalModule(name, main, resources);
  }
}
