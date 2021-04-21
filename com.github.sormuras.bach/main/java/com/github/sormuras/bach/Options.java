package com.github.sormuras.bach;

import com.github.sormuras.bach.api.Action;
import com.github.sormuras.bach.api.Option;
import com.github.sormuras.bach.api.Option.Value;
import com.github.sormuras.bach.api.ProjectInfo;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

public final class Options {

  public static Options of() {
    return new Options(new EnumMap<>(Option.class));
  }

  public static Options of(Option option, Object... objects) {
    return of().with(option, objects);
  }

  public static Options ofDefaultValues() {
    var map = new EnumMap<Option, Value>(Option.class);
    for (var option : Option.values()) {
      var value = option.defaultValue();
      if (value == Value.EMPTY) continue;
      map.put(option, value);
    }
    return new Options(map);
  }

  public static Options ofCommandLineArguments(String... arguments) {
    return ofCommandLineArguments(List.of(arguments));
  }

  public static Options ofCommandLineArguments(List<String> arguments) {
    var options = of();
    if (arguments.isEmpty()) return options;
    var deque = new ArrayDeque<String>();
    arguments.stream().flatMap(String::lines).map(String::strip).forEach(deque::add);
    while (!deque.isEmpty()) {
      var argument = deque.removeFirst();
      if (!argument.startsWith("--")) {
        options = options.with(Action.ofCli(argument));
        for (var remaining : deque) options = options.with(Action.ofCli(remaining));
        deque.clear();
        break;
      }
      var option = Option.ofCli(argument);
      if (option.isFlag()) { // no value-representing argument available
        options = options.with(option, Value.TRUE);
        continue;
      }
      var needs = Math.abs(option.cardinality());
      var remaining = deque.size();
      if (deque.size() < needs) {
        var mode = option.cardinality() >= 0 ? "exactly" : "at least";
        var format = "Too few arguments for option %s: need %s %d, but only %d remaining";
        throw new IllegalArgumentException(String.format(format, option, needs, mode, remaining));
      }
      if (option.isTerminal()) { // drain all remaining arguments
        var value = Value.of(deque.toArray(String[]::new));
        options = options.with(option, value);
        deque.clear();
        break;
      }
      var line = new String[needs]; // get exact number of arguments
      for (int i = 0; i < needs; i++) line[i] = deque.removeFirst();
      var value = Value.of(line);
      options = options.with(option, value);
    }
    return options;
  }

  public static Options ofProjectInfoElements(ProjectInfo info) {
    return Options.of()
        .with(Option.PROJECT_NAME, info.name())
        //.with(Option.PROJECT_VERSION, info.version())
        ;
  }

  public static Options ofProjectInfoOptions(ProjectInfo.Options options) {
    var arguments = new ArrayList<String>();
    for (var flag : options.flags()) {
      arguments.add(flag.cli()); // "--verbose"
    }
    for (var info : options.properties()) {
      var option = info.option();
      arguments.add(option.cli()); // "--project-name"
      if (option.isFlag()) continue; // "--verbose"
      for (var value : info.value()) {
        arguments.add(value.strip()); // "NAME"
      }
    }
    for (var action : options.actions()) {
      arguments.add(Option.ACTION.cli()); // "--action"
      arguments.add(action.name()); // "ACTION"
    }
    return ofCommandLineArguments(arguments);
  }

  public static Options compose(Options... options) {
    var map = new EnumMap<Option, Value>(Option.class);
    option:
    for (var option : Option.values()) {
      for (var next : options) {
        var value = next.value(option);
        if (value != null) {
          map.put(option, value);
          continue option;
        }
      }
    }
    return new Options(map);
  }

  private final EnumMap<Option, Value> map;

  private Options(Map<Option, Value> map) {
    this.map = map.isEmpty() ? new EnumMap<>(Option.class) : new EnumMap<>(map);
  }

  @Override
  public boolean equals(Object that) {
    return this == that || that instanceof Options other && map.equals(other.map);
  }

  @Override
  public int hashCode() {
    return map.hashCode();
  }

  @Override
  public String toString() {
    return "Options{" + map + '}';
  }

  public boolean isEmpty() {
    return map.isEmpty();
  }

  public Value value(Option option) {
    return map.get(option);
  }

  public boolean is(Option option) {
    if (!option.isFlag()) throw new IllegalArgumentException("Not a flag: " + option);
    return value(option) == Value.TRUE;
  }

  public String get(Option option) {
    return value(option).origin();
  }

  public Options with(Action action) {
    return with(Option.ACTION, action.toCli());
  }

  public Options with(Option option, Object... objects) {
    // trivial cases
    if (objects instanceof Value[] value) return with(option, value[0]);
    if (objects instanceof String[][] strings) return with(option, new Value(strings));
    // flag case
    if (option.isFlag()) return with(option, Value.ofBoolean(objects));
    // convert to strings and wrap 'em up
    var strings = Arrays.stream(objects).map(Object::toString).toArray(String[]::new);
    return with(option, new Value(new String[][] {strings}));
  }

  public Options with(Option option) {
    if (!option.isFlag()) throw new IllegalArgumentException("Not a flag option: " + option);
    return with(option, Value.TRUE);
  }

  public Options with(Option option, Value value) {
    var copy = new EnumMap<>(map);
    copy.merge(option, value, option.isRepeatable() ? Value::concat : (o, n) -> value);
    return new Options(copy);
  }

  public Stream<Action> actions() {
    var action = value(Option.ACTION);
    if (action == null) return Stream.empty();
    return action.stream().map(String::toUpperCase).map(Action::ofCli);
  }

  public Stream<String> lines() {
    return lines(__ -> true);
  }

  public Stream<String> lines(Predicate<Option> filter) {
    var lines = new ArrayList<String>();
    for (var entry : map.entrySet()) {
      var option = entry.getKey();
      if (!filter.test(option)) continue;
      if (option.isFlag()) {
        if (is(option)) lines.add(option.cli());
        continue;
      }
      var value = entry.getValue();
      if (value == Value.EMPTY) continue;
      for (var strings : entry.getValue().strings()) {
        lines.add(option.cli());
        for (var line : strings) {
          lines.add("  " + line);
        }
      }
    }
    return lines.stream();
  }
}
