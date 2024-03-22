/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * A command composed of a tool and its arguments.
 *
 * @param tool a carrier of the tool to run, either as a name or an instance of {@link Tool}
 * @param arguments the arguments to be passed to the tool
 */
public record ToolCall(Carrier tool, List<String> arguments) {
  public static ToolCall of(Tool tool, String... arguments) {
    return new ToolCall(new Carrier.Direct(tool), List.of(arguments));
  }

  public static ToolCall of(String name, String... arguments) {
    return new ToolCall(new Carrier.Nominal(name), List.of(arguments));
  }

  public sealed interface Carrier {
    String name();

    record Nominal(String name) implements Carrier {}

    record Direct(Tool tool) implements Carrier {
      @Override
      public String name() {
        return tool.identifier().toNamespaceAndNameAndVersion();
      }
    }
  }

  public ToolCall add(Object argument) {
    return addAll(Stream.of(argument));
  }

  public ToolCall add(String key, Object value, Object... more) {
    return switch (more.length) {
      case 0 -> addAll(Stream.of(key, value));
      case 1 -> addAll(Stream.of(key, value, more[0]));
      case 2 -> addAll(Stream.of(key, value, more[0], more[1]));
      case 3 -> addAll(Stream.of(key, value, more[0], more[1], more[2]));
      default -> addAll(Stream.of(key, value)).addAll(Stream.of(more));
    };
  }

  public ToolCall addAll(String... arguments) {
    return addAll((Object[]) arguments);
  }

  public ToolCall addAll(Object... arguments) {
    return switch (arguments.length) {
      case 0 -> this;
      case 1 -> addAll(Stream.of(arguments[0]));
      case 2 -> addAll(Stream.of(arguments[0], arguments[1]));
      case 3 -> addAll(Stream.of(arguments[0], arguments[1], arguments[2]));
      default -> addAll(Stream.of(arguments));
    };
  }

  public ToolCall addAll(Stream<?> stream) {
    var tip = arguments.stream();
    var tail = stream.map(Object::toString);
    return new ToolCall(tool, Stream.concat(tip, tail).toList());
  }

  public ToolCall addFiles(String glob) {
    return addFiles(Path.of(""), glob);
  }

  public ToolCall addFiles(Path start, String glob) {
    return addFiles(start, "glob", glob);
  }

  public ToolCall addFiles(Path start, String syntax, String pattern) {
    var syntaxAndPattern = syntax + ':' + pattern;
    var matcher = start.getFileSystem().getPathMatcher(syntaxAndPattern);
    return addFiles(start, Integer.MAX_VALUE, matcher);
  }

  public ToolCall addFiles(Path start, int maxDepth, PathMatcher matcher) {
    try (var files = Files.find(start, maxDepth, (p, _) -> matcher.matches(p))) {
      return addAll(files);
    } catch (Exception exception) {
      throw new RuntimeException("Find files failed in: " + start, exception);
    }
  }

  public ToolCall when(boolean condition, Object argument, Object... more) {
    return condition ? add(argument).addAll(more) : this;
  }

  public ToolCall when(boolean condition, UnaryOperator<ToolCall> then) {
    return condition ? then.apply(this) : this;
  }

  public <T> ToolCall when(Collection<T> collection, Operator<Collection<T>> then) {
    return collection.isEmpty() ? this : then.apply(this, collection);
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public <T> ToolCall when(Optional<T> optional, Operator<T> then) {
    return optional.isPresent() ? then.apply(this, optional.get()) : this;
  }

  public void run() {
    var name = tool.name();
    var args = arguments.toArray(String[]::new);
    System.out.println("| " + name + (args.length == 0 ? "" : " " + String.join(" ", args)));

    var provider =
        switch (tool) {
          case Carrier.Nominal _ -> Tool.of(name).provider();
          case Carrier.Direct carrier -> carrier.tool().provider();
        };
    var out = new PrintWriter(System.out, true);
    var err = new PrintWriter(System.err, true);
    var code = provider.run(out, err, args);
    if (code != 0) throw new RuntimeException(this + " failed with exit code " + code);
  }

  @FunctionalInterface
  public interface Operator<T> extends BiFunction<ToolCall, T, ToolCall> {
    @Override
    ToolCall apply(ToolCall call, T operator);
  }
}
