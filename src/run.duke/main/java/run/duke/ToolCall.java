package run.duke;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;

/**
 * A tool call.
 *
 * @param provider an optional provider instance running this tool call
 * @param tool an identifier or nickname of the tool to run
 * @param arguments a list of arguments to pass to the tool
 */
public record ToolCall(Optional<ToolProvider> provider, String tool, List<String> arguments) {
  public static ToolCall of(ToolProvider provider) {
    return new ToolCall(provider);
  }

  public static ToolCall of(String tool) {
    return new ToolCall(tool);
  }

  public static ToolCall of(String tool, Object... args) {
    if (args.length == 0) return new ToolCall(tool);
    if (args.length == 1) return new ToolCall(tool, args[0].toString().trim());
    if (args.length == 2)
      return new ToolCall(tool, args[0].toString().trim(), args[1].toString().trim());
    return new ToolCall(tool).with(Stream.of(args));
  }

  // command = ["tool-name", "tool-args", ...]
  public static ToolCall ofCommand(List<String> command) {
    var size = command.size();
    if (size == 0) throw new IllegalArgumentException("Empty command");
    var tool = command.get(0);
    if (size == 1) return new ToolCall(tool);
    if (size == 2) return new ToolCall(tool, command.get(1).trim());
    if (size == 3) return new ToolCall(tool, command.get(1).trim(), command.get(2).trim());
    return new ToolCall(tool).with(command.stream().skip(1).map(String::trim));
  }

  // line = "tool-name [tool-args...]"
  public static ToolCall ofCommandLine(String line) {
    return ToolCall.ofCommand(List.of(line.trim().split("\\s+")));
  }

  private ToolCall(String tool, String... args) {
    this(Optional.empty(), tool, List.of(args));
  }

  private ToolCall(ToolProvider provider) {
    this(Optional.of(provider), provider.name(), List.of());
  }

  public String toCommandLine() {
    return toCommandLine(" ");
  }

  public String toCommandLine(String delimiter) {
    if (arguments.isEmpty()) return tool;
    if (arguments.size() == 1) return tool + delimiter + arguments.get(0);
    var joiner = new StringJoiner(delimiter).add(tool);
    arguments.forEach(joiner::add);
    return joiner.toString();
  }

  public ToolCall with(Stream<?> objects) {
    var strings = objects.map(Object::toString).map(String::trim);
    return new ToolCall(provider, tool, Stream.concat(arguments.stream(), strings).toList());
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

  public ToolCall withTweak(int position, Tweak tweak) {
    var call = new ToolCall(provider, tool, List.of()).with(arguments.stream().limit(position));
    return tweak.tweak(call).with(arguments.stream().skip(position));
  }

  public ToolCall withTweaks(Iterable<Tweak> tweaks) {
    var tweaked = this;
    for (var tweak : tweaks) tweaked = tweak.tweak(tweaked);
    return tweaked;
  }

  /** Represents a unary operation on a tool call producing a new tool call with other arguments. */
  @FunctionalInterface
  public interface Tweak {
    ToolCall tweak(ToolCall call);
  }
}
