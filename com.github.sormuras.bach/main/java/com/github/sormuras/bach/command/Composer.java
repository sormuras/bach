package com.github.sormuras.bach.command;

import java.util.function.UnaryOperator;

/** A command component and additional arguments operator. */
public interface Composer<T extends Command<T>> extends UnaryOperator<T> {

  /** {@return a command composer that always returns the same command instance} */
  static <C extends Command<C>> Composer<C> identity() {
    return command -> command;
  }
}
