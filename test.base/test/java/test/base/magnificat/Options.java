package test.base.magnificat;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import test.base.magnificat.api.Action;
import test.base.magnificat.api.Option;
import test.base.magnificat.api.ProjectInfo;

public record Options(Map<Option, List<String>> map) {

  public static Options of() {
    return new Options(Map.of());
  }

  public static Options of(Option option, Object... value) {
    return of().with(option, value);
  }

  public static Options ofAllDefaults() {
    var map = new EnumMap<Option, List<String>>(Option.class);
    for (var key : Option.values()) map.put(key, List.of(key.defaults()));
    return new Options(Map.copyOf(map));
  }

  public static Options ofProjectInfoElements(ProjectInfo info) {
    var map = new EnumMap<Option, List<String>>(Option.class);
    map.put(Option.PROJECT_NAME, List.of(info.name()));
    map.put(Option.PROJECT_VERSION, List.of(info.version()));
    return new Options(Map.copyOf(map));
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

  public static Options ofCommandLineArguments(String... arguments) {
    return ofCommandLineArguments(List.of(arguments));
  }

  public static Options ofCommandLineArguments(List<String> arguments) {
    if (arguments.isEmpty()) return of();
    var map = new EnumMap<Option, List<String>>(Option.class);
    var deque = new ArrayDeque<String>();
    arguments.stream().flatMap(String::lines).map(String::strip).forEach(deque::add);
    while (!deque.isEmpty()) {
      var argument = deque.removeFirst();
      if (!argument.startsWith("--")) throw new IllegalArgumentException(argument);
      var option = Option.ofCli(argument);
      if (option.isFlag()) { // no value, not repeatable
        map.put(option, List.of("true"));
        continue;
      }
      var values = new ArrayList<String>();
      for (int i = 1; i <= option.cardinality(); i++) values.add(deque.removeFirst());
      var value = List.copyOf(values);
      if (option.isRepeatable()) {
        map.merge(option, value, (v, w) -> Stream.concat(v.stream(), w.stream()).toList());
        continue;
      }
      map.putIfAbsent(option, value); // don't override
    }
    return new Options(Map.copyOf(map));
  }

  public static Options compose(Options... options) {
    var map = new EnumMap<Option, List<String>>(Option.class);
    option:
    for (var key : Option.values()) {
      for (var next : options) {
        var value = next.map.get(key);
        if (value != null) {
          map.put(key, List.copyOf(value));
          continue option;
        }
      }
      throw new IllegalArgumentException("No value mapped for option: " + key);
    }
    return new Options(Map.copyOf(map));
  }

  public boolean is(Option option) {
    return map.containsKey(option);
  }

  public String get(Option option) {
    return values(option).get(0);
  }

  public List<String> values(Option option) {
    return map.get(option);
  }

  public List<Action> actions() {
    return values(Option.ACTION).stream().map(String::toUpperCase).map(Action::valueOf).toList();
  }

  public Options with(Option option, Object... value) {
    return with(option, Arrays.stream(value).map(Object::toString).toList());
  }

  public Options with(Option option, List<String> value) {
    var map = new EnumMap<Option, List<String>>(Option.class);
    map.putAll(this.map);
    map.put(option, value);
    return new Options(Map.copyOf(map));
  }
}
