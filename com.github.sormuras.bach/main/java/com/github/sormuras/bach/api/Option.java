package com.github.sormuras.bach.api;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public enum Option {
  CHROOT("Change into the specified directory.", ".", Modifier.EXTRA),

  BACH_INFO_MODULE("Name of the `bach.info` module", "bach.info", Modifier.EXTRA),

  VERBOSE("Output messages about what Bach is doing"),

  VERSION("Print version and exit."),

  HELP("Print this help message and exit."),

  HELP_EXTRA("Print help on extra options and exit."),

  SHOW_CONFIGURATION("Print effective configuration and continue."),

  LIST_TOOLS("List provided tools and exit."),

  PROJECT_NAME("The name of project.", ProjectInfo.DEFAULT_PROJECT_NAME),

  EXTERNAL_MODULE_LOCATION(
      """
      An external module-location mapping.
      Example:
        --external-module-location
          org.junit.jupiter
          org.junit.jupiter:junit-jupiter:5.7.1""",
      null,
      2,
      Modifier.REPEATABLE),

  TOOL("Run the specified tool.", null, -1, Modifier.EXTRA),

  ACTION("The name of the action to be executed.", null, 1, Modifier.REPEATABLE);

  public static Option ofCli(String string) {
    if (!string.startsWith("--")) throw new UnsupportedOptionException(string);
    var name = string.substring(2).toUpperCase(Locale.ROOT).replace('-', '_');
    try {
      return valueOf(name);
    } catch (IllegalArgumentException exception) {
      throw new UnsupportedOptionException(string);
    }
  }

  public static String toCli(Option option) {
    return "--" + option.name().toLowerCase(Locale.ROOT).replace('_', '-');
  }

  private final String description;
  private final Value defaultValue;
  private final int cardinality;
  private final Set<Modifier> modifiers;

  /**
   * Flag option constructor.
   *
   * @param modifiers the modifiers of this option
   */
  Option(String description, Modifier... modifiers) {
    this(description, Value.of("false"), 0, modifiers);
  }

  /**
   * Key-value option constructor.
   *
   * @param defaultValue the single default value of this option
   * @param modifiers the modifiers of this option
   */
  Option(String description, String defaultValue, Modifier... modifiers) {
    this(description, Value.of(defaultValue), 1, modifiers);
  }

  /**
   * Canonical option constructor.
   *
   * @param defaultValue the default value of this option
   * @param cardinality the number of command-line arguments read by this option
   * @param modifiers the modifiers of this option
   */
  Option(String description, Value defaultValue, int cardinality, Modifier... modifiers) {
    this.description = description;
    this.defaultValue = defaultValue;
    this.cardinality = cardinality;
    this.modifiers =
        modifiers.length == 0 ? EnumSet.noneOf(Modifier.class) : EnumSet.copyOf(Set.of(modifiers));
  }

  public String cli() {
    return toCli(this);
  }

  public String description() {
    return description;
  }

  public int cardinality() {
    return cardinality;
  }

  public Optional<Value> defaultValue() {
    return Optional.ofNullable(defaultValue);
  }

  public boolean isFlag() {
    return cardinality == 0;
  }

  public boolean isTerminal() {
    return cardinality < 0;
  }

  public boolean isRepeatable() {
    return is(Modifier.REPEATABLE);
  }

  public boolean isExtra() {
    return is(Modifier.EXTRA);
  }

  public boolean isStandard() {
    return !isExtra();
  }

  public boolean is(Modifier modifier) {
    return modifiers.contains(modifier);
  }

  public Value toValue(Object... objects) {
    if (objects.length == 0) {
      if (isFlag()) return Value.of("true");
      throw new BachException("One or more object parameters expected");
    }
    if (objects.length == 1) {
      var object = objects[0];
      if (object instanceof Value value) return value;
      if (object instanceof Boolean flag) return flag ? Value.of("true") : Value.of("false");
      return Value.of(object.toString());
    }
    return new Value(Stream.of(objects).map(Object::toString).toList());
  }

  public enum Modifier {
    EXTRA,
    REPEATABLE
  }

  public record Value(List<String> elements) {

    public static Value of(String... elements) {
      return new Value(List.of(elements));
    }

    public static Value concat(Value oldValue, Value newValue) {
      var newElements = newValue.elements;
      if (newElements.isEmpty()) return oldValue;
      return new Value(Stream.concat(oldValue.elements.stream(), newElements.stream()).toList());
    }

    public String join() {
      return String.join(" ", elements);
    }
  }
}
