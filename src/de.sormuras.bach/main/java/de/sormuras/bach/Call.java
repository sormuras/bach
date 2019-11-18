package de.sormuras.bach;

import de.sormuras.bach.util.Paths;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Tool call builder. */
public class Call {
  private final String name;
  private final List<String> arguments;

  public Call(String name, String... args) {
    this.name = name;
    this.arguments = new ArrayList<>(List.of(args));
  }

  public Call(Call that) {
    this.name = that.name;
    this.arguments = new ArrayList<>(that.arguments);
  }

  public Call add(Object object) {
    arguments.add(object.toString());
    return this;
  }

  public Call add(String key, Object value) {
    return add(key).add(value);
  }

  public Call add(String key, List<Path> paths) {
    if (paths.isEmpty()) return this;
    return add(key).add(Paths.join(paths));
  }

  public <T> Call forEach(Iterable<T> arguments, BiConsumer<Call, T> visitor) {
    arguments.forEach(argument -> visitor.accept(this, argument));
    return this;
  }

  public Call iff(boolean predicate, Consumer<Call> visitor) {
    if (predicate) visitor.accept(this);
    return this;
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public <T> Call iff(Optional<T> optional, BiConsumer<Call, T> visitor) {
    optional.ifPresent(value -> visitor.accept(this, value));
    return this;
  }

  @Override
  @SuppressWarnings("MethodDoesntCallSuperMethod")
  public Call clone() {
    return new Call(this);
  }

  public String[] toArray(boolean named) {
    return (named ? toList(true) : arguments).toArray(String[]::new);
  }

  public List<String> toList(boolean named) {
    if (!named) return List.copyOf(arguments);
    var list = new ArrayList<String>(1 + arguments.size());
    list.add(name);
    list.addAll(arguments);
    return List.copyOf(list);
  }

  @Override
  public String toString() {
    return "Call{name='" + name + "', arguments=" + arguments + '}';
  }
}
