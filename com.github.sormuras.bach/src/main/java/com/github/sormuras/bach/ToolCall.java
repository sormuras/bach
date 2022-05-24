package com.github.sormuras.bach;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.stream.Stream;

/** A command consisting of a tool name and a list of arguments. */
public record ToolCall(String name, List<String> arguments) {
  public static ToolCall of(String name, Object... arguments) {
    if (arguments.length == 0) return new ToolCall(name);
    if (arguments.length == 1) return new ToolCall(name, List.of(arguments[0].toString()));
    return new ToolCall(name).with(Stream.of(arguments));
  }

  public static ToolCall of(List<String> command) {
    var size = command.size();
    if (size == 0) throw new IllegalArgumentException("Empty command");
    var name = command.get(0);
    if (size == 1) return new ToolCall(name);
    if (size == 2) return new ToolCall(name, List.of(command.get(1).trim()));
    return new ToolCall(name).with(command.stream().skip(1).map(String::trim));
  }

  public ToolCall(String name) {
    this(name, List.of());
  }

  public ToolCall with(Stream<?> objects) {
    var strings = objects.map(Object::toString);
    return new ToolCall(name, Stream.concat(arguments.stream(), strings).toList());
  }

  public ToolCall with(Object argument) {
    return with(Stream.of(argument));
  }

  public ToolCall with(String key, Object value, Object... values) {
    var call = with(Stream.of(key, value));
    return values.length == 0 ? call : call.with(Stream.of(values));
  }

  public ToolCall with(ToolCallTweak tweak) {
    return tweak.apply(this);
  }

  public ToolCall withFindFiles(String glob) {
    return withFindFiles(Path.of(""), glob);
  }

  public ToolCall withFindFiles(Path start, String glob) {
    return withFindFiles(start, "glob", glob);
  }

  public ToolCall withFindFiles(Path start, String syntax, String pattern) {
    var syntaxAndPattern = syntax + ':' + pattern;
    var matcher = start.getFileSystem().getPathMatcher(syntaxAndPattern);
    return withFindFiles(start, Integer.MAX_VALUE, matcher);
  }

  public ToolCall withFindFiles(Path start, int maxDepth, PathMatcher matcher) {
    try (var files = Files.find(start, maxDepth, (p, a) -> matcher.matches(p))) {
      return with(files);
    } catch (Exception exception) {
      throw new RuntimeException("Find files failed in: " + start, exception);
    }
  }
}
