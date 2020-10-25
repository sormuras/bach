package com.github.sormuras.bach.internal;

import java.util.function.Supplier;

/** Internal {@link FunctionalInterface}-related utilities. */
public class Functions {

  // https://stackoverflow.com/a/35335467/1431016
  public static <T> Supplier<T> memoize(Supplier<T> original) {
    return new Supplier<>() {
      Supplier<T> delegate = this::firstTime;
      boolean initialized; // = false

      @Override
      public T get() {
        return delegate.get();
      }

      private synchronized T firstTime() {
        if (!initialized) {
          T value = original.get();
          delegate = () -> value;
          initialized = true;
        }
        return delegate.get();
      }
    };
  }

  /** Hidden default constructor. */
  private Functions() {}
}
