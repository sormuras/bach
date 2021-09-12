package com.github.sormuras.bach.command;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/** An option collecting module source path segments in module-pattern form. */
public record ModuleSourcePathPatternsOption(List<String> values) implements Option.Values<String> {
  public static ModuleSourcePathPatternsOption empty() {
    return new ModuleSourcePathPatternsOption(List.of());
  }

  public static ModuleSourcePathPatternsOption of(Collection<String> patterns) {
    return new ModuleSourcePathPatternsOption(List.copyOf(patterns));
  }

  public String join() {
    return values.stream()
        .map(segment -> segment.replace('/', File.separatorChar))
        .map(segment -> segment.replace('\\', File.separatorChar))
        .collect(Collectors.joining(File.pathSeparator));
  }

  public ModuleSourcePathPatternsOption add(String segment) {
    var values = new ArrayList<>(this.values);
    values.add(segment);
    return new ModuleSourcePathPatternsOption(List.copyOf(values));
  }
}
