package de.sormuras.bach;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.spi.ToolProvider;

/**
 * A tool call with a list of argument objects.
 *
 * @param <T> The type of the tool
 */
public interface Call<T> {

  static Tool tool(String name) {
    return new Tool(name, List.of());
  }

  String name();

  List<Argument> arguments();

  T with(List<Argument> arguments);

  default T with(Argument... arguments) {
    var list = new ArrayList<>(arguments());
    list.addAll(List.of(arguments));
    return with(list);
  }

  default T with(String option) {
    return with(Argument.of(option));
  }

  default T with(String option, Object... values) {
    return with(Argument.of(option, values));
  }

  default T with(boolean condition, String option, Object... values) {
    if (!condition) return with(arguments());
    return with(option, values);
  }

  default T with(Iterable<?> elements) {
    var list = new ArrayList<>(arguments());
    for (var element : elements) list.add(Argument.of(element.toString()));
    return with(list);
  }

  default <E> T with(Iterable<E> elements, BiFunction<T, E, T> function) {
    var that = with(arguments());
    for (var element : elements) that = function.apply(that, element);
    return that;
  }

  default T without(String option) {
    var list = new ArrayList<>(arguments());
    list.removeIf(argument -> argument.option().equals(option));
    return with(list);
  }

  default String toCommandLine() {
    var command = new ArrayList<String>();
    command.add(name());
    command.addAll(toStrings());
    return String.join(" ", command);
  }

  default List<String> toStrings() {
    var strings = new ArrayList<String>();
    for (var argument : arguments()) {
      strings.add(argument.option());
      strings.addAll(argument.values());
    }
    return strings;
  }

  default String[] toStringArray() {
    return toStrings().toArray(String[]::new);
  }

  default Optional<ToolProvider> findProvider() {
    return ToolProvider.findFirst(name());
  }

  default int run() {
    System.out.println("> " + toCommandLine());
    return findProvider().orElseThrow().run(System.out, System.err, toStringArray());
  }

  /** An argument is a named tool option consisting of an option key and zero or more values. */
  final class Argument {

    public static Argument of(String option) {
      return new Argument(option, List.of());
    }

    public static Argument of(String option, Object... values) {
      return of(option, List.of(values));
    }

    public static Argument of(String option, Collection<?> values) {
      var strings = new ArrayList<String>();
      for (var value : values) strings.add(value.toString());
      return new Argument(option, strings);
    }

    private final String option;
    private final List<String> values;

    public Argument(String option, List<String> values) {
      this.option = option;
      this.values = values;
    }

    public String option() {
      return option;
    }

    public List<String> values() {
      return values;
    }
  }

  /** A named tool call. */
  final class Tool implements Call<Tool> {

    private final String name;
    private final List<Argument> arguments;

    public Tool(String name, List<Argument> arguments) {
      this.name = name;
      this.arguments = arguments;
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public List<Argument> arguments() {
      return arguments;
    }

    @Override
    public Tool with(List<Argument> arguments) {
      return new Tool(name, arguments);
    }
  }
}
