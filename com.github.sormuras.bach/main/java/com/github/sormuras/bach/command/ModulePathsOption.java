package com.github.sormuras.bach.command;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** An option collecting paths specifying where to find application modules. */
public record ModulePathsOption(List<Path> values) implements Option.Values<Path> {
  public static ModulePathsOption empty() {
    return new ModulePathsOption(List.of());
  }

  public ModulePathsOption add(Path path, Path... more) {
    var values = new ArrayList<>(this.values);
    values.add(path);
    if (more.length > 0) values.addAll(List.of(more));
    return new ModulePathsOption(values);
  }
}
