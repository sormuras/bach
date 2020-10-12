package com.github.sormuras.bach;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** An immutable tool call implementation. */
public final class Command implements ToolCall {

  public static Builder builder(String name) {
    return new Builder(name);
  }

  public static Command of(String name, Object... arguments) {
    var builder = new Builder(name);
    for (var argument : arguments) builder.with(argument);
    return builder.build();
  }

  private final String name;
  private final String[] args;

  public Command(String name, String... args) {
    this.name = name;
    this.args = args;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public String[] args() {
    return args;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Command command = (Command) o;
    return name.equals(command.name) && Arrays.equals(args, command.args);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(name);
    result = 31 * result + Arrays.hashCode(args);
    return result;
  }

  @Override
  public String toString() {
    return args.length == 0 ? name : name + ' ' + String.join(" ", args);
  }

  /** A command builing helper. */
  public static final class Builder {

    private final String name;
    private final List<String> arguments;

    public Builder(String name) {
      this.name = name;
      this.arguments = new ArrayList<>();
    }

    public Command build() {
      return new Command(name, arguments.toArray(String[]::new));
    }

    public Builder with(Object argument) {
      arguments.add(argument.toString());
      return this;
    }

    public Builder with(String option, Object value, Object... values) {
      with(option).with(value);
      for (var more : values) with(more);
      return this;
    }
  }
}
