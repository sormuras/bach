package com.github.sormuras.bach;

import com.github.sormuras.bach.api.Action;
import com.github.sormuras.bach.api.BachException;
import com.github.sormuras.bach.api.ExternalLibraryName;
import com.github.sormuras.bach.api.ExternalModuleLocation;
import com.github.sormuras.bach.api.Option;
import com.github.sormuras.bach.api.Option.Value;
import com.github.sormuras.bach.api.ProjectInfo;
import java.io.IOException;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public record Options(String title, EnumMap<Option, Value> map) {

  public static Options of() {
    var frame = StackWalker.getInstance().walk(frames -> frames.skip(1).findFirst()).orElseThrow();
    return Options.of(frame.getMethodName() + '@' + frame.getByteCodeIndex());
  }

  public static Options of(String title) {
    return new Options(title, new EnumMap<>(Option.class));
  }

  public static Options ofDefaultValues() {
    var map = new EnumMap<Option, Value>(Option.class);
    for (var option : Option.values())
      option.defaultValue().ifPresent(defaultValue -> map.put(option, defaultValue));
    return new Options("default values", map);
  }

  public static Options ofCommandLineArguments(String title, String... arguments) {
    if (arguments.length == 0) return Options.of(title);
    return Options.ofCommandLineArguments(title, List.of(arguments));
  }

  public static Options ofCommandLineArguments(String title, List<String> arguments) {
    var options = Options.of(title);
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
        options = options.with(option, Value.of("true"));
        continue;
      }
      var needs = Math.abs(option.cardinality());
      var remaining = deque.size();
      if (deque.size() < needs) {
        var mode = option.cardinality() >= 0 ? "exactly" : "at least";
        var message = "Too few arguments for option %s: need %s %d, but only %d remaining";
        throw new BachException(message, option, mode, needs, remaining);
      }
      if (option.isGreedy()) { // get all remaining arguments
        options = options.with(option, new Value(List.copyOf(deque)));
        deque.clear();
        break;
      }
      var exact = deque.stream().limit(needs).toList(); // get exact number of arguments
      for (var ignored : exact) deque.removeFirst(); // remove those elements from the deque (...)
      options = options.with(option, new Value(exact));
    }
    return options;
  }

  public static Options ofProjectInfoElements(ProjectInfo info) {
    var options = Options.of("@ProjectInfo elements");
    options = options.withIfDifferent(Option.PROJECT_NAME, Value.of(info.name()));
    // TODO options = options.withIfDifferent(Option.PROJECT_VERSION, Value.of(info.version()));
    options = options.withIfDifferent(Option.MAIN_JAVA_RELEASE, Value.of(info.main().javaRelease() + ""));
    for (var module : info.requires()) options = options.with(Option.PROJECT_REQUIRES, module);
    for (var module : info.external().externalModules()) {
      var location = ExternalModuleLocation.of(module);
      options = options.with(Option.EXTERNAL_MODULE_LOCATION, location.module(), location.uri());
    }
    for (var library : info.external().externalLibraries()) {
      var name = ExternalLibraryName.ofCli(library.name().cli());
      options = options.with(Option.EXTERNAL_LIBRARY_VERSION, name.cli(), library.version());
    }
    return options;
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
    return ofCommandLineArguments("@ProjectInfo.options()", arguments);
  }

  public static Options ofDirectory(Path directory) {
    var title = "directory options (" + directory + ")";
    if (!Files.isDirectory(directory)) return Options.of(title);
    var name = directory.toAbsolutePath().normalize().getFileName();
    if (name == null) return Options.of(title);
    return Options.of(title).with(Option.PROJECT_NAME, name);
  }

  public static Options ofFile(Path file) {
    var title = "file options (" + file + ")";
    if (Files.notExists(file)) return Options.of(title);
    try {
      var lines = Files.readAllLines(file);
      return Options.ofCommandLineArguments(title, lines);
    } catch (IOException exception) {
      throw new BachException("Read all lines failed for: " + file, exception);
    }
  }

  public static Options compose(String title, Logbook logbook, Options... options) {
    /* DEBUG */ {
      logbook.log(Level.DEBUG, "Compose options from " + options.length + " components");
      for (int i = 0; i < options.length; i++) {
        var next = options[i];
        var size = next.map.size();
        var s = size == 1 ? "" : "s";
        logbook.log(Level.TRACE, "[" + i + "] = " + next.title + " with " + size + " element" + s);
      }
      logbook.log(Level.DEBUG, "[component] --<option> <value...>");
    }
    var map = new EnumMap<Option, Value>(Option.class);
    option:
    for (var option : Option.values()) {
      for (int i = 0; i < options.length; i++) {
        var next = options[i];
        var value = next.value(option);
        if (value == null) continue;
        map.put(option, value);
        /* DEBUG */ {
          var source = "[" + i + "] ";
          var target = " " + value.join();
          logbook.log(Level.DEBUG, source + option.cli() + target);
        }
        continue option;
      }
    }
    return new Options(title, map);
  }

  public Value value(Option option) {
    return map.get(option);
  }

  public String get(Option option) {
    return value(option).elements().get(0);
  }

  public List<String> list(Option option) {
    var value = value(option);
    return value == null ? List.of() : value.elements();
  }

  public boolean is(Option option) {
    if (!option.isFlag()) throw new IllegalArgumentException("Not a flag: " + option);
    return Value.of("true").equals(value(option));
  }

  public Options with(Action action) {
    return with(Option.ACTION, action.cli());
  }

  public Options with(Option option) {
    if (!option.isFlag()) throw new IllegalArgumentException("Not a flag option: " + option);
    return with(option, Value.of("true"));
  }

  public Options with(Option option, Object... objects) {
    return with(option, option.toValue(objects));
  }

  public Options withIfDifferent(Option option, Value value) {
    if (Objects.equals(value, option.defaultValue().orElse(null))) return this;
    return with(option, value);
  }

  public Options with(Option option, Value value) {
    var copy = new EnumMap<>(map);
    copy.merge(option, value, option.isRepeatable() ? Value::concat : (o, n) -> value);
    return new Options(title, copy);
  }

  public Optional<Entry> findFirstEntry(Predicate<Option> filter) {
    return map.entrySet().stream().filter(e -> filter.test(e.getKey())).findFirst().map(Entry::new);
  }

  public Stream<Action> actions() {
    return list(Option.ACTION).stream().map(String::toUpperCase).map(Action::ofCli);
  }

  public Stream<String> lines(Predicate<Option> filter) {
    var lines = new ArrayList<String>();
    for (var entry : map.entrySet()) {
      var option = entry.getKey();
      if (!filter.test(option)) continue;
      var cli = option.cli();
      if (option.isFlag()) {
        if (is(option)) lines.add(cli);
        continue;
      }
      var elements = entry.getValue().elements();
      if (option.isGreedy()) {
        lines.add(cli);
        lines.add("  " + String.join(" ", elements));
        continue;
      }
      var deque = new ArrayDeque<>(elements);
      while (!deque.isEmpty()) {
        lines.add(cli);
        for (int i = 0; i < option.cardinality(); i++) lines.add("  " + deque.removeFirst());
      }
    }
    return lines.stream();
  }

  public record Entry(Option option, Value value) {
    public Entry(EnumMap.Entry<Option, Value> entry) {
      this(entry.getKey(), entry.getValue());
    }
  }
}
