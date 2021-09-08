package com.github.sormuras.bach.conventional;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record ConventionalModule(String name, Optional<String> main, List<Path> resources) {
  public static ConventionalModule of(String name) {
    return new ConventionalModule(name, Optional.empty(), List.of());
  }

  public ConventionalModule main(String mainClassName) {
    return new ConventionalModule(name, Optional.ofNullable(mainClassName), resources);
  }

  public ConventionalModule resourcesAdd(Path path, Path... more) {
    var resources = new ArrayList<>(this.resources);
    resources.add(path);
    if (more.length > 0) resources.addAll(List.of(more));
    return new ConventionalModule(name, main, resources);
  }
}
