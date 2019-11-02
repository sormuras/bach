/*
 * Bach - Java Shell Builder
 * Copyright (C) 2019 Christian Stein
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

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*BODY*/
/** Command-line program argument list builder. */
public /*STATIC*/ class Command {

  private final String name;
  private final List<String> arguments = new ArrayList<>();

  /** Initialize Command instance with zero or more arguments. */
  public Command(String name, Object... args) {
    this.name = name;
    addEach(args);
  }

  /** Initialize Command instance with zero or more arguments. */
  public Command(String name, Iterable<?> arguments) {
    this.name = name;
    addEach(arguments);
  }

  @Override
  @SuppressWarnings("MethodDoesntCallSuperMethod")
  public Command clone() {
    return new Command(name, arguments);
  }

  /** Add single argument by invoking {@link Object#toString()} on the given argument. */
  public Command add(Object argument) {
    arguments.add(argument.toString());
    return this;
  }

  /** Add two arguments by invoking {@link #add(Object)} for the key and value elements. */
  public Command add(Object key, Object value) {
    return add(key).add(value);
  }

  /** Add two (or zero) arguments, the key and the paths joined by system's path separator. */
  public Command add(Object key, Collection<Path> paths) {
    return add(key, paths.stream(), UnaryOperator.identity());
  }

  /** Add two (or zero) arguments, the key and the paths joined by system's path separator. */
  public Command add(Object key, Stream<Path> stream, UnaryOperator<String> operator) {
    var value = stream.map(Object::toString).collect(Collectors.joining(File.pathSeparator));
    if (value.isEmpty()) {
      return this;
    }
    return add(key, operator.apply(value));
  }

  /** Add all arguments by invoking {@link #add(Object)} for each element. */
  public Command addEach(Object... arguments) {
    return addEach(List.of(arguments));
  }

  /** Add all arguments by invoking {@link #add(Object)} for each element. */
  public Command addEach(Iterable<?> arguments) {
    arguments.forEach(this::add);
    return this;
  }

  /** Add all arguments by invoking {@link #add(Object)} for each element. */
  public Command addEach(Stream<?> arguments) {
    arguments.forEach(this::add);
    return this;
  }

  /** Add all arguments by delegating to the passed visitor for each element. */
  public <T> Command addEach(Iterable<T> arguments, BiConsumer<Command, T> visitor) {
    arguments.forEach(argument -> visitor.accept(this, argument));
    return this;
  }

  /** Add a single argument iff the conditions is {@code true}. */
  public Command addIff(boolean condition, Object argument) {
    return condition ? add(argument) : this;
  }

  /** Add two arguments iff the conditions is {@code true}. */
  public Command addIff(boolean condition, Object key, Object value) {
    return condition ? add(key, value) : this;
  }

  /** Add two arguments iff the given optional value is present. */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public Command addIff(Object key, Optional<?> optionalValue) {
    return optionalValue.isPresent() ? add(key, optionalValue.get()) : this;
  }

  /** Let the consumer visit, usually modify, this instance iff the conditions is {@code true}. */
  public Command addIff(boolean condition, Consumer<Command> visitor) {
    if (condition) {
      visitor.accept(this);
    }
    return this;
  }

  /** Return the command's name. */
  public String getName() {
    return name;
  }

  /** Return the command's arguments. */
  public List<String> getArguments() {
    return arguments;
  }

  @Override
  public String toString() {
    var args = arguments.isEmpty() ? "<empty>" : "'" + String.join("', '", arguments) + "'";
    return "Command{name='" + name + "', args=[" + args + "]}";
  }

  /** Return an array of {@link String} containing all of the collected arguments. */
  public String[] toStringArray() {
    return arguments.toArray(String[]::new);
  }

  /** Return program's name and all arguments as single string using space as the delimiter. */
  public String toCommandLine() {
    return toCommandLine(" ");
  }

  /** Return program's name and all arguments as single string using passed delimiter. */
  public String toCommandLine(String delimiter) {
    if (arguments.isEmpty()) {
      return name;
    }
    return name + delimiter + String.join(delimiter, arguments);
  }
}
