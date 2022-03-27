package com.github.sormuras.bach.tools;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.stream.Stream;

public record Command(String name, List<String> arguments) {
  public static Command of(String name, Object... arguments) {
    if (arguments.length == 0) return new Command(name, List.of());
    if (arguments.length == 1) return new Command(name, List.of(arguments[0].toString()));
    return new Command(name, List.of()).with(Stream.of(arguments));
  }

  public Stream<String> stream() {
    return Stream.concat(Stream.of(name), arguments.stream());
  }

  public Command with(Stream<?> objects) {
    var strings = objects.map(Object::toString);
    return new Command(name, Stream.concat(arguments.stream(), strings).toList());
  }

  public Command with(Object argument) {
    return with(Stream.of(argument));
  }

  public Command with(String key, Object value, Object... values) {
    var call = with(Stream.of(key, value));
    return values.length == 0 ? call : call.with(Stream.of(values));
  }

  public Command withFindFiles(String glob) {
    return withFindFiles(Path.of(""), glob);
  }

  public Command withFindFiles(Path start, String glob) {
    return withFindFiles(start, "glob", glob);
  }

  public Command withFindFiles(Path start, String syntax, String pattern) {
    var syntaxAndPattern = syntax + ':' + pattern;
    var matcher = start.getFileSystem().getPathMatcher(syntaxAndPattern);
    return withFindFiles(start, Integer.MAX_VALUE, matcher);
  }

  public Command withFindFiles(Path start, int maxDepth, PathMatcher matcher) {
    try (var files = Files.find(start, maxDepth, (p, a) -> matcher.matches(p))) {
      return with(files);
    } catch (Exception exception) {
      throw new RuntimeException("Find files failed in: " + start, exception);
    }
  }
}
