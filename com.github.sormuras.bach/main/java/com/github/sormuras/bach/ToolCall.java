package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.ModuleLaunchingToolCall;
import com.github.sormuras.bach.internal.ProcessStartingToolCall;
import com.github.sormuras.bach.internal.ToolRunningToolCall;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/** Builds named tool calls with their arguments. */
public sealed interface ToolCall
    permits ToolRunningToolCall, ProcessStartingToolCall, ModuleLaunchingToolCall {

  Command<?> command();

  default String name() {
    return command().name();
  }

  default List<String> arguments() {
    return command().toArguments();
  }

  static ToolCall of(String name, Object... arguments) {
    return ToolCall.of(null, name, arguments);
  }

  static ToolCall of(ToolFinder finder, String name, Object... arguments) {
    return ToolCall.of(finder, Command.of(name, arguments));
  }

  static ToolCall of(ToolFinder finder, Command<?> command) {
    return new ToolRunningToolCall(Optional.ofNullable(finder), command);
  }

  static ToolCall java(Object... arguments) {
    var java = Configuration.computeJavaExecutablePath("java");
    return ToolCall.process(java, arguments);
  }

  static ToolCall process(Path executable, Object... arguments) {
    return new ProcessStartingToolCall(executable, Command.of(executable.toString(), arguments));
  }

  static ToolCall module(ModuleFinder finder, String name, Object... arguments) {
    return new ModuleLaunchingToolCall(finder, Command.of(name, arguments));
  }
}
