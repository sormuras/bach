package test.base.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;

interface ToolCall<T> {

  default String tool() {
    return getClass().getSimpleName().toLowerCase(Locale.ROOT);
  }

  List<Argument> arguments();

  T with(List<Argument> arguments);

  default T with(Argument argument, Argument... arguments) {
    var list = new ArrayList<>(arguments());
    list.add(argument);
    if (arguments.length > 0) list.addAll(List.of(arguments));
    return with(list);
  }

  default T with(String option) {
    return with(Argument.of(option));
  }

  default T with(String option, Object... values) {
    return with(Argument.of(option, values));
  }

  default T without(String option) {
    return without(argument -> argument.option().equals(option));
  }

  default T without(Predicate<Argument> filter) {
    var arguments = arguments();
    if (arguments.isEmpty()) return with(arguments);
    if (arguments.size() == 1) {
      var singleton = arguments.get(0);
      return filter.test(singleton) ? with(List.of()) : with(arguments);
    }
    var list = new ArrayList<>(arguments);
    return list.removeIf(filter) ? with(list) : with(arguments);
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

  default String[] toStringArray() {
    return toStrings().toArray(String[]::new);
  }
}
