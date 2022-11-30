package run.duke;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Stream;

public record ToolCall(String name, List<String> arguments) {
  // line = "tool-name [tool-args...]"
  public static ToolCall ofLine(String line) {
    var args = line.trim().split("\\s+");
    if (args.length == 0) throw new IllegalArgumentException("No tool name in: " + line);
    var name = args[0];
    if (args.length == 1) return new ToolCall(name);
    if (args.length == 2) return new ToolCall(name, List.of(args[1]));
    if (args.length == 3) return new ToolCall(name, List.of(args[1], args[2]));
    return new ToolCall(name, List.of(Arrays.copyOfRange(args, 1, args.length)));
  }

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

  public String toCommandLine(String delimiter) {
    if (arguments.isEmpty()) return name;
    if (arguments.size() == 1) return name + delimiter + arguments.get(0);
    var joiner = new StringJoiner(delimiter).add(name);
    arguments.forEach(joiner::add);
    return joiner.toString();
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

  public ToolCall withTweak(Tweak tweak) {
    return tweak.tweak(this);
  }

  public ToolCall withTweak(Tweak tweak, int position) {
    var call = ToolCall.of(name).with(arguments.stream().limit(position));
    return tweak.tweak(call).with(arguments.stream().skip(position));
  }

  public ToolCall withTweaks(List<Tweak> tweaks) {
    var tweaked = this;
    for (var tweak : tweaks) tweaked = tweak.tweak(tweaked);
    return tweaked;
  }

  @FunctionalInterface
  public interface Tweak {
    ToolCall tweak(ToolCall call);
  }
}
