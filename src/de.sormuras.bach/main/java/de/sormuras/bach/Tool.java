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

package de.sormuras.bach;

import de.sormuras.bach.util.Strings;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

/** A {@code String}-based mutable tool call description. */
public /*static*/ class Tool {

  private final String name;
  private final List<String> args = new ArrayList<>();

  public Tool(String name, Object... arguments) {
    this.name = name;
    addAll(arguments);
  }

  /** Return name of the tool to run. */
  public String name() {
    return name;
  }

  /** Return list of argument strings compiled from option properties. */
  public List<String> args() {
    return List.copyOf(args);
  }

  /** Append a single non-null argument. */
  public Tool add(Object argument) {
    args.add(argument.toString());
    return this;
  }

  /** Append two arguments, a key and a value. */
  public Tool add(String key, Object value) {
    return add(key).add(value);
  }

  /** Append three arguments, a key and two values. */
  public Tool add(String key, Object first, Object second) {
    return add(key).add(first).add(second);
  }

  /** Conditionally append one or more arguments. */
  public Tool add(boolean predicate, Object first, Object... more) {
    return predicate ? add(first).addAll(more) : this;
  }

  /** Append all given arguments, potentially none. */
  public Tool addAll(Object... arguments) {
    for (var argument : arguments) add(argument);
    return this;
  }

  /** Walk the given iterable and expect this instance to be changed by side effects. */
  public <T> Tool forEach(Iterable<T> iterable, BiConsumer<Tool, T> visitor) {
    iterable.forEach(argument -> visitor.accept(this, argument));
    return this;
  }

  protected boolean isAssigned(Object object) {
    if (object == null) return false;
    if (object instanceof Number) return ((Number) object).intValue() != 0;
    if (object instanceof Optional) return ((Optional<?>) object).isPresent();
    if (object instanceof Collection) return !((Collection<?>) object).isEmpty();
    return true;
  }

  @Convention
  protected String join(Collection<Path> paths) {
    return Strings.toString(paths).replace("{MODULE}", "*");
  }

  public List<String> toStrings() {
    return Strings.list(name(), args());
  }
}
