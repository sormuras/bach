package de.sormuras.bach;

import de.sormuras.bach.util.Paths;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** Tool call builder. */
public class Call {
  private final boolean argumentFileSupport;
  private final String name;
  private final List<String> arguments;

  public Call(String name, String... args) {
    this(name, false, args);
  }

  public Call(String name, boolean argumentFileSupport, String... args) {
    this.name = name;
    this.argumentFileSupport = argumentFileSupport;
    this.arguments = new ArrayList<>();
    if (args.length > 0) Arrays.stream(args).forEach(this::add);
  }

  public Call(Call that) {
    this.argumentFileSupport = that.argumentFileSupport;
    this.name = that.name;
    this.arguments = new ArrayList<>(that.arguments);
  }

  public String getName() {
    return name;
  }

  public Call add(Object object) {
    var argument = object.toString();
    if (argument == null) throw new IllegalArgumentException();
    if (argumentFileSupport && argument.charAt(0) == '@') {
      var path = Path.of(argument.substring(1));
      if (!Files.isRegularFile(path)) throw new Error("Could not open: " + path);
      Paths.readAllLines(path).stream()
          .filter(Predicate.not(line -> line.startsWith("#")))
          .filter(Predicate.not(String::isEmpty))
          .forEach(arguments::add);
      return this;
    }
    arguments.add(argument);
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

  public Call iff(boolean predicate, Consumer<Call> then, Consumer<Call> otherwise) {
    if (predicate) then.accept(this);
    else otherwise.accept(this);
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
