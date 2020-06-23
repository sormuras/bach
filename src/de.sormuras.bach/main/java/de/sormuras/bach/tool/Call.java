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
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.spi.ToolProvider;

/** A tool call configuration. */
public interface Call<T extends Call<T>> {

  /**
   * Return the name of this tool call configuration.
   *
   * @return A string representation of a tool
   * @see #tool()
   */
  String name();

  /**
   * Return the arguments of this tool call configuration.
   *
   * @return A possible empty list of argument instances
   */
  List<Argument> arguments();

  /**
   * Create new instance of a tool call configuration with the given arguments.
   *
   * @param arguments The possible empty list of argument objects
   * @return An instance of {@code T} with the given arguments
   */
  T with(List<Argument> arguments);

  /**
   * Return the activation state of this tool call configuration.
   *
   * @return {@code true} if this tool call is to executed, else {@code false}
   */
  default boolean activated() {
    return !arguments().isEmpty();
  }

  /**
   * Return a tool provider instance in an optional object.
   *
   * @return An optional tool provider instance
   * @see #name()
   */
  default Optional<ToolProvider> tool() {
    return ToolProvider.findFirst(name());
  }

  /**
   * Find first argument by its option key and return its sole attached value as a string.
   *
   * @param option The option key to look for
   * @return An empty optional if the key was not found, else the wrapped string value
   * @throws NoSuchElementException If the argument was found but it doesn't provide a singleton
   */
  default Optional<String> find(String option) {
    var first = arguments().stream().filter(argument -> argument.key().equals(option)).findFirst();
    if (first.isEmpty()) return Optional.empty();
    var values = first.get().values();
    if (values.size() == 1) return Optional.of(values.get(0));
    throw new NoSuchElementException("Expected one value for " + option + ", but got: " + values);
  }

  /**
   * Create new call instance with the given additional arguments.
   *
   * @param argument The first additional argument
   * @param arguments The array of more additional arguments
   * @return A new call instance with the given arguments
   */
  default T with(Argument argument, Argument... arguments) {
    var list = new ArrayList<>(arguments());
    list.add(argument);
    if (arguments.length > 0) Collections.addAll(list, arguments);
    return with(list);
  }

  /**
   * Create new call instance with one additional argument.
   *
   * @param option The option to used as an additional argument
   * @return A new call instance with the given argument
   */
  default T with(String option) {
    return with(new Argument(option, List.of()));
  }

  /**
   * Create new call instance with one or more additional arguments.
   *
   * @param option The option to used as an additional argument
   * @param values The possible empty array of additional arguments
   * @return A new call instance with the given arguments
   */
  default T with(String option, Object... values) {
    var strings = new ArrayList<String>();
    for (var value : values) strings.add(value.toString());
    return with(new Argument(option, strings));
  }

  /**
   * Create new call instance for each given element.
   *
   * @param elements The elements to iterate and pass the function
   * @param function The function to be applied to this tool and each element
   * @param <E> The element type of the iterable
   * @return A new call instance with the given function applied
   */
  default <E> T with(Iterable<E> elements, BiFunction<T, E, T> function) {
    @SuppressWarnings("unchecked")
    var that = (T) this; // var that = with(arguments());
    for (var element : elements) that = function.apply(that, element);
    return that;
  }

  /**
   * Create new call instance with one or more arguments removed by their name.
   *
   * @param option The option to be removed from the list of arguments
   * @return A new call instance without the named argument(s)
   */
  default T without(String option) {
    var arguments = new ArrayList<>(arguments());
    arguments.removeIf(argument -> argument.key().equals(option));
    return with(arguments);
  }

  /**
   * Return the arguments of this tool call configuration as a list of string objects.
   *
   * @return A list of strings representing the arguments of this tool call.
   */
  default List<String> toStrings() {
    var list = new ArrayList<String>();
    for (var argument : arguments()) {
      list.add(argument.key());
      list.addAll(argument.values());
    }
    return list;
  }

  /**
   * Create new arbitrary tool call configuration for the given name.
   *
   * @param name The name of the tool to call
   * @return A new named tool call object
   */
  static Tool tool(String name) {
    return Tool.of(name);
  }
}
