package com.github.sormuras.bach;

import com.github.sormuras.bach.tool.Jar;
import com.github.sormuras.bach.tool.Javac;
import com.github.sormuras.bach.tool.Javadoc;
import com.github.sormuras.bach.tool.Tool;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.function.Predicate;

public interface Command<C> {

  static Tool of(String name, String... args) {
    return new Tool(name, args.length == 0 ? List.of() : List.of(Argument.of("", (Object[]) args)));
  }

  static Jar jar() {
    return new Jar();
  }

  static Javac javac() {
    return new Javac();
  }

  static Javadoc javadoc() {
    return new Javadoc();
  }

  String name();

  List<Argument> arguments();

  C arguments(List<Argument> arguments);

  default C add(Argument argument, Argument... arguments) {
    var list = new ArrayList<>(arguments());
    list.add(argument);
    if (arguments.length > 0) list.addAll(List.of(arguments));
    return arguments(list);
  }

  default C add(String option) {
    return add(Argument.of(option));
  }

  default C add(String option, Object... values) {
    return add(Argument.of(option, values));
  }

  default C clear(String option) {
    return clear(argument -> argument.option().equals(option));
  }

  default C clear(Predicate<Argument> filter) {
    var arguments = arguments();
    if (arguments.isEmpty()) return arguments(arguments);
    if (arguments.size() == 1) {
      var singleton = arguments.get(0);
      return filter.test(singleton) ? arguments(List.of()) : arguments(arguments);
    }
    var list = new ArrayList<>(arguments);
    return list.removeIf(filter) ? arguments(list) : arguments(arguments);
  }

  default Optional<Argument> findFirstArgument(String option) {
    return arguments().stream().filter(it -> it.option().equals(option)).findFirst();
  }

  default List<String> toStrings() {
    var arguments = arguments();
    if (arguments.isEmpty()) return List.of();
    var strings = new ArrayList<String>();
    arguments.forEach(argument -> argument.accept(strings::add));
    return List.copyOf(strings);
  }

  default String toLine() {
    var joiner = new StringJoiner(" ");
    joiner.add(name());
    toStrings().forEach(joiner::add);
    return joiner.toString();
  }

  record Argument(String option, List<?> values) {

    public Argument {
      if (option.isEmpty() && values.isEmpty())
        throw new IllegalArgumentException("Option and values must not be empty at the same time");
    }

    public static Argument of(String option) {
      return new Argument(option, List.of());
    }

    public static Argument of(String option, Object... values) {
      return new Argument(option, List.of(values));
    }

    public void accept(Consumer<String> consumer) {
      if (!option.isEmpty()) consumer.accept(option);
      if (values.isEmpty()) return;
      values.stream().map(Object::toString).forEach(consumer);
    }
  }
}
