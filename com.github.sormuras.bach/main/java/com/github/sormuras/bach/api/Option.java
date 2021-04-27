package com.github.sormuras.bach.api;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public enum Option {
  CHROOT("Change into the specified directory.", ".", Modifier.HIDDEN),

  BACH_INFO_MODULE("Name of the `bach.info` module", "bach.info", Modifier.HIDDEN),

  VERBOSE("Output messages about what Bach is doing"),

  VERSION("Print version and exit.", Modifier.EXIT),

  HELP("Print this help message and exit.", Modifier.EXIT),

  HELP_EXTRA("Print help on extra options and exit.", Modifier.EXIT),

  SHOW_CONFIGURATION("Print effective configuration and continue."),

  LIST_MODULES("List modules and exit.", Modifier.EXIT),

  LIST_TOOLS("List provided tools and exit.", Modifier.EXIT),

  DESCRIBE_TOOL("Describe a tool and exit.", 1, Modifier.EXIT),

  PROJECT_NAME("The name of project.", ProjectInfo.DEFAULT_PROJECT_NAME),

  PROJECT_REQUIRES("A name of a module required by this project.", 1, Modifier.REPEATABLE),

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

  LOAD_EXTERNAL_MODULE("Load an external module.", 1, Modifier.EXIT),

  LOAD_MISSING_EXTERNAL_MODULES("Load all missing external modules.", Modifier.EXIT),

  TOOL("Run the specified tool and exit with its return value.", -1, Modifier.EXIT),

  ACTION("The name of the action to be executed.", 1, Modifier.REPEATABLE);

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
   * @param description the help message of this option
   * @param modifiers the modifiers of this option
   */
  Option(String description, Modifier... modifiers) {
    this(description, null, 0, modifiers);
  }

  /**
   * Key-value(s) option constructor with no default value.
   *
   * @param description the help message of this option
   * @param cardinality the number of command-line arguments read by this option
   * @param modifiers the modifiers of this option
   */
  Option(String description, int cardinality, Modifier... modifiers) {
    this(description, null, cardinality, modifiers);
  }

  /**
   * Key-value option constructor with an explicit default value.
   *
   * @param description the help message of this option
   * @param defaultValue the single default value of this option
   * @param modifiers the modifiers of this option
   */
  Option(String description, String defaultValue, Modifier... modifiers) {
    this(description, Value.of(defaultValue), 1, modifiers);
  }

  /**
   * Canonical option constructor.
   *
   * @param description the help message of this option
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

  public boolean isGreedy() {
    return cardinality < 0;
  }

  public boolean isExit() {
    return modifiers.contains(Modifier.EXIT);
  }

  public boolean isHidden() {
    return modifiers.contains(Modifier.HIDDEN);
  }

  public boolean isRepeatable() {
    return modifiers.contains(Modifier.REPEATABLE);
  }

  public boolean isVisible() {
    return !isHidden();
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

  enum Modifier {
    EXIT,
    HIDDEN,
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
