package com.github.sormuras.bach.command;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** An option of a command. */
public interface Option {

  /** A boolean option that is either not present or {@code true} or {@code false}. */
  interface Flag extends Option {
    /** {@return the state of this flag} */
    Optional<Boolean> value();

    /** {@return {@code true} iff the backing value is present and true, else {@code false}} */
    default boolean isTrue() {
      return value().orElse(false);
    }
  }

  /** A value holding option. */
  interface Value<T> extends Option {
    Optional<T> value();

    default boolean isPresent() {
      return value().isPresent();
    }

    default T get() {
      return value().orElseThrow();
    }
  }

  /** A list of values holding option. */
  interface Values<T> extends Option {
    List<T> values();

    default boolean isPresent() {
      return values().size() >= 1;
    }

    default String join(String delimiter) {
      return values().stream().map(Object::toString).collect(Collectors.joining(delimiter));
    }
  }
}
