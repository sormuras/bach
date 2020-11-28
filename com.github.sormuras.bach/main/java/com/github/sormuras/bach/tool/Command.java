package com.github.sormuras.bach.tool;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * An immutable tool call implementation.
 *
 * @param name the name of the tool to call
 * @param args the arguments
 */
public record Command(String name, List<String> args) implements ToolCall {

  /**
   * Instantiates a builder to build a command.
   *
   * @param name the name of the tool
   * @return a new builder
   */
  public static Builder builder(String name) {
    return new Builder(name);
  }

  /**
   * Builds a command with the given tool name and an array of arguments.
   *
   * @param name the name of the tool
   * @param arguments the arguments
   * @return a new command
   */
  public static Command of(String name, Object... arguments) {
    var builder = new Builder(name);
    for (var argument : arguments) builder.with(argument);
    return builder.build();
  }

  @Override
  public String toString() {
    return args.isEmpty() ? name : name + ' ' + String.join(" ", args);
  }

  /**
   * A builder for building {@link Command} objects.
   *
   * <p>Example usage:
   *
   * <pre>{@code
   * var command = Command.builder("name")
   *     .with("first-argument")
   *     .with("option", "value")
   *     .build();
   * }</pre>
   *
   * <p>A Builder checks the components and invariants as components are added to the builder. The
   * rationale for this is to detect errors as early as possible and not defer all validation to the
   * {@link #build()} method.
   */
  public static final class Builder {

    private final String name;
    private final List<String> strings;

    /**
     * Initializes a new builder with the given tool name.
     *
     * @param name the name of the tool
     */
    Builder(String name) {
      this.name = name;
      this.strings = new ArrayList<>();
    }

    /**
     * Builds and returns a command from its components.
     *
     * @return the command object
     */
    public Command build() {
      return new Command(name, List.copyOf(strings));
    }

    /**
     * Adds an argument.
     *
     * @param argument the argument
     * @return this builder instance
     * @throws IllegalArgumentException If the string representation of the argument is blank
     */
    public Builder with(Object argument) {
      var string = Objects.requireNonNull(argument, "argument must not be null").toString();
      if (string.isBlank()) throw new IllegalArgumentException("argument must not be blank");

      strings.add(string);
      return this;
    }

    /**
     * Adds two or more arguments.
     *
     * @param option the first argument
     * @param value the second argument
     * @param values the second argument
     * @return this builder instance
     */
    public Builder with(String option, Object value, Object... values) {
      Objects.requireNonNull(option, "option must not be null");
      Objects.requireNonNull(value, "value must not be null");
      Objects.requireNonNull(values, "values must not be null");

      with(option).with(value);
      for (var more : values) with(more);
      return this;
    }

    /**
     * Calls the given consumer if a value is present in the given optional.
     *
     * @param optional the optional
     * @param consumer the consumer
     * @param <T> the type of the optional element
     * @return this builder instance
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public <T> Builder with(Optional<T> optional, BiConsumer<Builder, T> consumer) {
      optional.ifPresent(it -> consumer.accept(this, it));
      return this;
    }

    /**
     * Calls for each iterable element the given consumer.
     *
     * @param iterable the iterable
     * @param consumer the consumer
     * @param <T> the type of elements
     * @return this builder instance
     */
    public <T> Builder withEach(Iterable<T> iterable, BiConsumer<Builder, T> consumer) {
      for (var element : iterable) consumer.accept(this, element);
      return this;
    }

    /**
     * Adds all arguments provided by the given iterable object.
     *
     * @param arguments the arguments to add
     * @return this builder instance
     */
    public Builder withEach(Iterable<?> arguments) {
      for (var argument : arguments) with(argument);
      return this;
    }
  }
}
