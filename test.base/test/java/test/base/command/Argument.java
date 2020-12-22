package test.base.command;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

record Argument(String option, List<?> values) {

  Argument {
    if (option.isEmpty() && values.isEmpty())
      throw new IllegalArgumentException("Option and values must not be empty at the same time");
  }

  static Argument of(String option) {
    return new Argument(option, List.of());
  }

  static Argument of(String option, Object... values) {
    return new Argument(option, List.of(values));
  }

  static Argument of(String option, Collection<?> values) {
    return new Argument(option, List.copyOf(values));
  }

  void accept(Consumer<String> consumer) {
    if (!option.isEmpty()) consumer.accept(option);
    if (values.isEmpty()) return;
    values.stream().map(Object::toString).forEach(consumer);
  }
}
