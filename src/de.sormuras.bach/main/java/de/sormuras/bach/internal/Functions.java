/*
 * Bach - Java Shell Builder
 * Copyright (C) 2020 Christian Stein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.sormuras.bach.internal;

import java.util.Objects;
import java.util.function.Supplier;

/** {@link java.util.function}-related utilities. */
public /*static*/ class Functions {

  /**
   * Cache first result of a supplier.
   *
   * @see <a href="https://stackoverflow.com/a/35335467/1431016">Cached Supplier</a>
   */
  public static <T> Supplier<T> memoize(Supplier<T> supplier) {
    Objects.requireNonNull(supplier, "supplier");
    class CachingSupplier implements Supplier<T> {
      Supplier<T> delegate = this::initialize;
      boolean initialized = false;

      @Override
      public T get() {
        return delegate.get();
      }

      private synchronized T initialize() {
        if (initialized) return delegate.get();
        T value = supplier.get();
        delegate = () -> value;
        initialized = true;
        return value;
      }
    }
    return new CachingSupplier();
  }

  private Functions() {}
}
