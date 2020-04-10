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

package de.sormuras.bach.tool;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/** An arguments collector can be used to build a list of strings by adding objects to it. */
public /*static*/ class Arguments {
  private final List<String> list = new ArrayList<>();

  public Arguments(Object... arguments) {
    addAll(arguments);
  }

  /** Return an immutable list of all added argument strings. */
  public List<String> build() {
    return List.copyOf(list);
  }

  /** Append a single non-null argument. */
  public Arguments add(Object argument) {
    list.add(argument.toString());
    return this;
  }

  /** Append two arguments, a key and a value. */
  public Arguments add(String key, Object value) {
    return add(key).add(value);
  }

  /** Append three arguments, a key and two values. */
  public Arguments add(String key, Object first, Object second) {
    return add(key).add(first).add(second);
  }

  /** Conditionally append one or more arguments. */
  public Arguments add(boolean predicate, Object first, Object... more) {
    return predicate ? add(first).addAll(more) : this;
  }

  /** Append all given arguments, potentially none. */
  public Arguments addAll(Object... arguments) {
    for (var argument : arguments) add(argument);
    return this;
  }

  /** Walk the given iterable and expect this instance to be changed via side effects. */
  public <T> Arguments forEach(Iterable<T> iterable, BiConsumer<Arguments, T> consumer) {
    iterable.forEach(argument -> consumer.accept(this, argument));
    return this;
  }
}
