package com.github.sormuras.bach.command;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/** An option collecting module source path elements in module-pattern form. */
public record ModuleSourcePathSpecificsOption(List<String> values) implements Option.Values<String> {
  public static ModuleSourcePathSpecificsOption empty() {
    return new ModuleSourcePathSpecificsOption(List.of());
  }

  public ModuleSourcePathSpecificsOption withModuleSpecificForm(
      String module, Path path, Path... more) {
    var values = new ArrayList<>(this.values);
    var joiner = new StringJoiner(File.pathSeparator);
    joiner.add(path.toString());
    if (more.length > 0) for (var next : more) joiner.add(next.toString());
    values.add(module + "=" + joiner);
    return new ModuleSourcePathSpecificsOption(List.copyOf(values));
  }
}
