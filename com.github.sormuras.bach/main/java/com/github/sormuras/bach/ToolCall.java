package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.ModuleLaunchingToolCall;
import com.github.sormuras.bach.internal.ProcessStartingToolCall;
import com.github.sormuras.bach.internal.ToolRunningToolCall;
import java.io.File;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public sealed interface ToolCall
    permits ToolRunningToolCall, ProcessStartingToolCall, ModuleLaunchingToolCall {

  String name();

  List<String> arguments();

  static ToolCall of(String name, Object... arguments) {
    return ToolCall.of(null, name, arguments);
  }

  static ToolCall of(ToolFinder finder, String name, Object... arguments) {
    return new ToolRunningToolCall(Optional.ofNullable(finder), name, new ArrayList<>())
        .withAll(arguments);
  }

  static ToolCall java(Object... arguments) {
    var java = Configuration.computeJavaExecutablePath("java");
    return ToolCall.process(java, arguments);
  }

  static ToolCall process(Path executable, Object... arguments) {
    return new ProcessStartingToolCall(executable, new ArrayList<>()).withAll(arguments);
  }

  static ToolCall module(ModuleFinder finder, String name, Object... arguments) {
    return new ModuleLaunchingToolCall(finder, name, new ArrayList<>()).withAll(arguments);
  }

  default ToolCall with(Object argument) {
    arguments().add(argument.toString());
    return this;
  }

  default ToolCall with(String option, Object value, Object... more) {
    return with(option).with(value).withAll(more);
  }

  default ToolCall with(String option, Collection<Path> paths) {
    return with(option).with(paths, File.pathSeparator);
  }

  default ToolCall with(Collection<?> arguments, CharSequence delimiter) {
    if (arguments.isEmpty()) throw new IllegalArgumentException("Collection must not be empty");
    return with(arguments.stream().map(Object::toString).collect(Collectors.joining(delimiter)));
  }

  default ToolCall withAll(Object... arguments) {
    return withAll(List.of(arguments));
  }

  default ToolCall withAll(Collection<?> arguments) {
    arguments.stream().map(Object::toString).forEach(arguments()::add);
    return this;
  }
}
