package com.github.sormuras.bach;

import com.github.sormuras.bach.tool.JDeps;
import com.github.sormuras.bach.tool.JLink;
import com.github.sormuras.bach.tool.Jar;
import com.github.sormuras.bach.tool.Javac;
import com.github.sormuras.bach.tool.Javadoc;
import com.github.sormuras.bach.tool.Tool;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

public interface Command<C extends Command<C>> {

  static Tool of(String name, String... args) {
    return new Tool(name, List.of(args));
  }

  static Jar jar() {
    return new Jar();
  }

  static Javac javac() {
    return new Javac();
  }

  static Javadoc javadoc() {
    return new Javadoc();
  }

  static JDeps jdeps() {
    return new JDeps();
  }

  static JLink jlink() {
    return new JLink();
  }

  String name();

  List<String> arguments();

  C arguments(List<String> arguments);

  default C add(String argument) {
    var copy = new ArrayList<>(arguments());
    copy.add(argument);
    return arguments(List.copyOf(copy));
  }

  default C add(String option, Object value) {
    var copy = new ArrayList<>(arguments());
    copy.add(option);
    copy.add(value.toString());
    return arguments(List.copyOf(copy));
  }

  default C add(String option, Object value, Object... more) {
    var copy = new ArrayList<>(arguments());
    copy.add(option);
    copy.add(value.toString());
    for (var next : more) copy.add(next.toString());
    return arguments(List.copyOf(copy));
  }

  default C add(String option, Collection<Path> paths) {
    var strings = paths.stream().map(Path::toString).toList();
    return add(option, String.join(File.pathSeparator, strings));
  }

  default C addAll(String... arguments) {
    var copy = new ArrayList<>(arguments());
    copy.addAll(List.of(arguments));
    return arguments(List.copyOf(copy));
  }

  default C addAll(Object... arguments) {
    return addAll(List.of(arguments));
  }

  default C addAll(Collection<?> collection) {
    var copy = new ArrayList<>(arguments());
    collection.stream().map(Object::toString).forEach(copy::add);
    return arguments(List.copyOf(copy));
  }

  @SuppressWarnings("unchecked")
  default C ifTrue(boolean condition, UnaryOperator<C> operator) {
    return condition ? operator.apply((C) this) : (C) this;
  }

  @SuppressWarnings("unchecked")
  default <E extends Collection<?>> C ifPresent(E collection, BiFunction<C, E, C> function) {
    return collection.isEmpty() ? (C) this : function.apply((C) this, collection);
  }

  @SuppressWarnings("unchecked")
  default <E> C forEach(Collection<E> collection, BiFunction<C, E, C> function) {
    var command = (C) this;
    for (var element : collection) command = function.apply((C) this, element);
    return command;
  }

  default String toDescription(int maxLineLength) {
    var arguments = arguments();
    var line = arguments.isEmpty() ? "</>" : String.join(" ", arguments);
    return line.length() <= maxLineLength ? line : line.substring(0, maxLineLength - 5) + "[...]";
  }

  default String toLine() {
    var joiner = new StringJoiner(" ");
    joiner.add(name());
    arguments().forEach(joiner::add);
    return joiner.toString();
  }
}
