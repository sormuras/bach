package com.github.sormuras.bach;

import java.io.File;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public sealed interface Call permits Call.ToolCall, Call.ProcessCall, Call.ModuleCall {

  List<String> arguments();

  static Call tool(String name, Object... arguments) {
    return Call.tool(null, name, arguments);
  }

  static Call tool(ToolFinder finder, String name, Object... arguments) {
    return new ToolCall(Optional.ofNullable(finder), name, new ArrayList<>()).withAll(arguments);
  }

  static Call java(Object... arguments) {
    return Call.process(computeJavaExecutablePath("java"), arguments);
  }

  static Call process(Path executable, Object... arguments) {
    return new ProcessCall(executable, new ArrayList<>()).withAll(arguments);
  }

  static Call module(ModuleFinder finder, String module, Object... arguments) {
    return new ModuleCall(finder, module, new ArrayList<>()).withAll(arguments);
  }

  static Path computeJavaExecutablePath(String name) {
    var windows = System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("win");
    return Path.of(System.getProperty("java.home"), "bin", name + (windows ? ".exe" : ""));
  }

  default Call with(Object argument) {
    arguments().add(argument.toString());
    return this;
  }

  default Call with(String option, Object value, Object... more) {
    return with(option).with(value).withAll(more);
  }

  default Call with(String option, Collection<Path> paths) {
    return with(option).with(paths, File.pathSeparator);
  }

  default Call with(Collection<?> arguments, CharSequence delimiter) {
    if (arguments.isEmpty()) throw new IllegalArgumentException("Collection must not be empty");
    return with(arguments.stream().map(Object::toString).collect(Collectors.joining(delimiter)));
  }

  default Call withAll(Object... arguments) {
    return withAll(List.of(arguments));
  }

  default Call withAll(Collection<?> arguments) {
    arguments.stream().map(Object::toString).forEach(arguments()::add);
    return this;
  }

  record ModuleCall(ModuleFinder finder, String module, List<String> arguments) implements Call {}

  record ProcessCall(Path executable, List<String> arguments) implements Call {}

  record ToolCall(Optional<ToolFinder> finder, String name, List<String> arguments)
      implements Call {}
}
