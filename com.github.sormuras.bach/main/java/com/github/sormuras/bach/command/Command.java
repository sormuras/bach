package com.github.sormuras.bach.command;

import java.util.Collection;
import java.util.List;

/**
 * An aggregator of command-line arguments.
 *
 * @param <T> the implementing type
 */
public interface Command<T extends Command<T>> {

  /** {@return the name of the command} */
  String name();

  default T composing(Composer<T> composer) {
    @SuppressWarnings("unchecked")
    T self = (T) this;
    return composer.apply(self);
  }

  /** {@return the option object holding zero or more additional arguments} */
  AdditionalArgumentsOption additionals();

  /**
   * Creates a new instance of the implementing class with the given option object.
   *
   * @param additionals the option object to use
   * @return a new instance of the implementing class
   * @see #option(Option)
   */
  T additionals(AdditionalArgumentsOption additionals);

  static DefaultCommand of(String name) {
    return new DefaultCommand(name);
  }

  static JarCommand jar() {
    return new JarCommand();
  }

  static JavacCommand javac() {
    return new JavacCommand();
  }

  /**
   * Creates a new instance of the implementing class with the given option object.
   *
   * @param option the option object to use
   * @return a new instance of the implementing class
   */
  default T option(Option option) {
    if (option instanceof AdditionalArgumentsOption additionals) return additionals(additionals);
    throw new UnsupportedOperationException("Option class " + option.getClass());
  }

  default T add(Object argument) {
    return additionals(additionals().add(argument));
  }

  default T add(String option, Object value, Object... more) {
    return additionals(additionals().add(option, value, more));
  }

  default T addAll(Object... arguments) {
    return additionals(additionals().addAll(arguments));
  }

  default T addAll(Collection<?> arguments) {
    return additionals(additionals().addAll(arguments));
  }

  /** {@return a list of all aggregated arguments} */
  default List<String> toArguments() {
    return List.copyOf(additionals().values());
  }
}
