package com.github.sormuras.bach;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

public interface ToolCall<C extends ToolCall<C>> {

  String name();

  List<String> arguments();

  C arguments(List<String> arguments);

  default C with(String argument) {
    var copy = new ArrayList<>(arguments());
    copy.add(argument);
    return arguments(List.copyOf(copy));
  }

  default C with(String option, Object value) {
    var copy = new ArrayList<>(arguments());
    copy.add(option);
    copy.add(value.toString());
    return arguments(List.copyOf(copy));
  }

  default C with(String option, Object value, Object... more) {
    var copy = new ArrayList<>(arguments());
    copy.add(option);
    copy.add(value.toString());
    for (var next : more) copy.add(next.toString());
    return arguments(List.copyOf(copy));
  }

  default C with(String option, Collection<Path> paths) {
    var strings = paths.stream().map(Path::toString).toList();
    return with(option, String.join(File.pathSeparator, strings));
  }

  default C withAll(String... arguments) {
    var copy = new ArrayList<>(arguments());
    copy.addAll(List.of(arguments));
    return arguments(List.copyOf(copy));
  }

  default C withAll(Object... arguments) {
    return withAll(List.of(arguments));
  }

  default C withAll(Collection<?> collection) {
    var copy = new ArrayList<>(arguments());
    collection.stream().map(Object::toString).forEach(copy::add);
    return arguments(List.copyOf(copy));
  }

  @SuppressWarnings("unchecked")
  default C ifTrue(boolean condition, UnaryOperator<C> operator) {
    return condition ? operator.apply((C) this) : (C) this;
  }

  @SuppressWarnings("unchecked")
  default <T extends Collection<?>> C ifPresent(T collection, BiFunction<C, T, C> function) {
    return collection.isEmpty() ? (C) this : function.apply((C) this, collection);
  }

  @SuppressWarnings({"unchecked", "OptionalUsedAsFieldOrParameterType"})
  default <T> C ifPresent(Optional<T> optional, BiFunction<C, T, C> function) {
    return optional.isPresent() ? function.apply((C) this, optional.get()) : (C) this;
  }

  @SuppressWarnings("unchecked")
  default <E> C forEach(Collection<E> collection, BiFunction<C, E, C> function) {
    var command = (C) this;
    for (var element : collection) command = function.apply(command, element);
    return command;
  }

  default String toDescription(int maxLineLength) {
    var arguments = arguments();
    var line = arguments.isEmpty() ? "</>" : String.join(" ", arguments);
    return line.length() <= maxLineLength ? line : line.substring(0, maxLineLength - 5) + "[...]";
  }
}
