package com.github.sormuras.bach.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** An option that holds zero or more additional command-line arguments. */
public record AdditionalArgumentsOption(List<String> values) implements Option {
  public static AdditionalArgumentsOption empty() {
    return new AdditionalArgumentsOption(List.of());
  }

  public AdditionalArgumentsOption add(Object value) {
    var values = new ArrayList<>(this.values);
    values.add(value.toString());
    return new AdditionalArgumentsOption(List.copyOf(values));
  }

  public AdditionalArgumentsOption add(String option, Object value, Object... more) {
    var values = new ArrayList<>(this.values);
    values.add(option);
    values.add(value.toString());
    for (var additional : more) values.add(additional.toString());
    return new AdditionalArgumentsOption(List.copyOf(values));
  }

  public AdditionalArgumentsOption addAll(Object... arguments) {
    if (arguments.length == 0) return this;
    if (arguments.length == 1) return add(arguments[0]);
    return addAll(List.of(arguments));
  }

  public AdditionalArgumentsOption addAll(Collection<?> arguments) {
    if (arguments.isEmpty()) return this;
    var values = new ArrayList<>(this.values);
    arguments.stream().map(Object::toString).forEach(values::add);
    return new AdditionalArgumentsOption(List.copyOf(values));
  }
}
