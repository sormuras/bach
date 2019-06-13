package de.sormuras.bach;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/*BODY*/
/** Command-line program argument list builder. */
public /*STATIC*/ class Command {

  final String name;
  final List<String> list = new ArrayList<>();

  /** Initialize Command instance with zero or more arguments. */
  Command(String name, Object... args) {
    this.name = name;
    addEach(args);
  }

  /** Add single argument by invoking {@link Object#toString()} on the given argument. */
  Command add(Object argument) {
    list.add(argument.toString());
    return this;
  }

  /** Add two arguments by invoking {@link #add(Object)} for the key and value elements. */
  Command add(Object key, Object value) {
    return add(key).add(value);
  }

  /** Add two arguments, i.e. the key and the paths joined by system's path separator. */
  Command add(Object key, Collection<Path> paths) {
    return add(key, paths, UnaryOperator.identity());
  }

  /** Add two arguments, i.e. the key and the paths joined by system's path separator. */
  Command add(Object key, Collection<Path> paths, UnaryOperator<String> operator) {
    var stream = paths.stream() /*.filter(Files::isDirectory)*/.map(Object::toString);
    return add(key, operator.apply(stream.collect(Collectors.joining(File.pathSeparator))));
  }

  /** Add all arguments by invoking {@link #add(Object)} for each element. */
  Command addEach(Object... arguments) {
    return addEach(List.of(arguments));
  }

  /** Add all arguments by invoking {@link #add(Object)} for each element. */
  Command addEach(Iterable<?> arguments) {
    arguments.forEach(this::add);
    return this;
  }

  /** Add a single argument iff the conditions is {@code true}. */
  Command addIff(boolean condition, Object argument) {
    return condition ? add(argument) : this;
  }

  /** Let the consumer visit, usually modify, this instance iff the conditions is {@code true}. */
  Command addIff(boolean condition, Consumer<Command> visitor) {
    if (condition) {
      visitor.accept(this);
    }
    return this;
  }

  @Override
  public String toString() {
    var args = list.isEmpty() ? "<empty>" : "'" + String.join("', '", list) + "'";
    return "Command{name='" + name + "', list=[" + args + "]}";
  }

  /** Returns an array of {@link String} containing all of the collected arguments. */
  String[] toStringArray() {
    return list.toArray(String[]::new);
  }
}
