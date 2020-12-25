package test.base.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

interface Command<T> {

  String tool();

  List<Argument> arguments();

  T arguments(List<Argument> arguments);

  default T add(Argument argument, Argument... arguments) {
    var list = new ArrayList<>(arguments());
    list.add(argument);
    if (arguments.length > 0) list.addAll(List.of(arguments));
    return arguments(list);
  }

  default T add(String option) {
    return add(Argument.of(option));
  }

  default T add(String option, Object... values) {
    return add(Argument.of(option, values));
  }

  default T clear(String option) {
    return clear(argument -> argument.option().equals(option));
  }

  default T clear(Predicate<Argument> filter) {
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

  record Argument(String option, List<?> values) {

    public Argument {
      if (option.isEmpty() && values.isEmpty())
        throw new IllegalArgumentException("Option and values must not be empty at the same time");
    }

    static Argument of(String option) {
      return new Argument(option, List.of());
    }

    static Argument of(String option, Object... values) {
      return new Argument(option, List.of(values));
    }

    void accept(Consumer<String> consumer) {
      if (!option.isEmpty()) consumer.accept(option);
      if (values.isEmpty()) return;
      values.stream().map(Object::toString).forEach(consumer);
    }
  }

  static Call call(String tool) {
    return new Call(tool, List.of());
  }

  record Call(String tool, List<Argument> arguments) implements Command<Call> {

    @Override
    public Call arguments(List<Argument> arguments) {
      return new Call(tool, arguments);
    }
  }
}
