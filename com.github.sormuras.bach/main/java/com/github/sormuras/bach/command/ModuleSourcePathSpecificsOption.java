package com.github.sormuras.bach.command;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/** An option collecting module source path elements in module-specific form. */
public record ModuleSourcePathSpecificsOption(List<String> values)
    implements Option.Values<String> {
  public static ModuleSourcePathSpecificsOption empty() {
    return new ModuleSourcePathSpecificsOption(List.of());
  }

  public static ModuleSourcePathSpecificsOption of(Map<String, List<Path>> specifics) {
    var values = new ArrayList<String>();
    specifics.entrySet().stream().map(ModuleSourcePathSpecificsOption::toString).forEach(values::add);
    return new ModuleSourcePathSpecificsOption(List.copyOf(values));
  }

  public ModuleSourcePathSpecificsOption withModuleSpecificForm(String module, Path... paths) {
    return withModuleSpecificForm(module, List.of(paths));
  }

  public ModuleSourcePathSpecificsOption withModuleSpecificForm(String module, List<Path> paths) {
    var values = new ArrayList<>(this.values);
    values.add(toString(module, paths));
    return new ModuleSourcePathSpecificsOption(List.copyOf(values));
  }

  private static String toString(Map.Entry<String, List<Path>> entry) {
    return toString(entry.getKey(), entry.getValue());
  }

  private static String toString(String module, List<Path> paths) {
    var joiner = new StringJoiner(File.pathSeparator);
    paths.stream().map(Path::toString).forEach(joiner::add);
    return module + '=' + joiner;
  }
}
