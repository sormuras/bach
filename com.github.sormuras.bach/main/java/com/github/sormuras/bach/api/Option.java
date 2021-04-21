package com.github.sormuras.bach.api;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
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

  PROJECT_NAME("The name of project.", ProjectInfo.DEFAULT_PROJECT_NAME),

  EXTERNAL_MODULE_LOCATION(
      """
      An external module-location mapping.
      Example:
        --external-module-location
          org.junit.jupiter
          org.junit.jupiter:junit-jupiter:5.7.1""",
      Value.EMPTY,
      2,
      Modifier.REPEATABLE),

  TOOL("Run the specified tool.", Value.EMPTY, -1, Modifier.EXTRA),

  ACTION("The name of the action to be executed.", Value.EMPTY, 1, Modifier.REPEATABLE);

  public static Option ofCli(String cli) {
    var name = cli.substring(2).toUpperCase(Locale.ROOT).replace('-', '_');
    return valueOf(name);
  }

  public static String toCli(Option option) {
    return "--" + option.name().toLowerCase(Locale.ROOT).replace('_', '-');
  }

  private final String helpMessage;
  private final Value defaultValue;
  private final int cardinality;
  private final Set<Modifier> modifiers;

  /**
   * Flag option constructor.
   *
   * @param modifiers the modifiers of this option
   */
  Option(String helpMessage, Modifier... modifiers) {
    this(helpMessage, Value.FALSE, 0, modifiers);
  }

  /**
   * Key-value option constructor.
   *
   * @param defaultValue the single default value of this option
   * @param modifiers the modifiers of this option
   */
  Option(String helpMessage, String defaultValue, Modifier... modifiers) {
    this(helpMessage, defaultValue, 1, modifiers);
  }

  /**
   * Key-value(s) option constructor.
   *
   * @param defaultValue the default value of this option
   * @param cardinality the number of values of this option
   * @param modifiers the modifiers of this option
   */
  Option(String helpMessage, String defaultValue, int cardinality, Modifier... modifiers) {
    this(helpMessage, Value.of(new String[] {defaultValue}), cardinality, modifiers);
  }

  /**
   * Canonical option constructor.
   *
   * @param defaultValue the default value of this option
   * @param cardinality the number of command-line arguments read by this option
   * @param modifiers the modifiers of this option
   */
  Option(String helpMessage, Value defaultValue, int cardinality, Modifier... modifiers) {
    this.helpMessage = helpMessage;
    this.defaultValue = defaultValue;
    this.cardinality = cardinality;
    this.modifiers = modifiers.length == 0 ? EnumSet.noneOf(Modifier.class) : EnumSet.copyOf(Set.of(modifiers));
  }

  public String cli() {
    return toCli(this);
  }

  public String helpMessage() {
    return helpMessage;
  }

  public int cardinality() {
    return cardinality;
  }

  public Value defaultValue() {
    return defaultValue;
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

  public enum Modifier {
    EXTRA,
    REPEATABLE
  }

  public record Value(String[][] strings) {

    public static final Value EMPTY = new Value(new String[][] {{"<empty>"}});
    public static final Value TRUE = new Value(new String[][] {{"true"}});
    public static final Value FALSE = new Value(new String[][] {{"false"}});

    public static Value of(String[]... strings) {
      return strings.length == 0 ? EMPTY : new Value(strings);
    }

    public static Value ofBoolean(Object... objects) {
      if (objects.length == 0) return TRUE;
      if (objects.length > 1) return FALSE;
      var object = objects[0];
      if (object instanceof Boolean b) return ofBoolean(b);
      if (object instanceof String s) return ofBoolean(Boolean.parseBoolean(s));
      return FALSE;
    }

    public static Value ofBoolean(boolean b) {
      return b ? TRUE : FALSE;
    }

    public Value concat(Value other) {
      if (this == EMPTY) return other;
      var others = other.strings;
      var merged = Arrays.copyOf(strings, strings.length + others.length);
      System.arraycopy(others, 0, merged, strings.length, others.length);
      return new Value(merged);
    }

    public String origin() {
      return strings()[0][0];
    }

    public Stream<String> stream() {
      return Arrays.stream(strings).flatMap(Arrays::stream);
    }

    @Override
    public String toString() {
      return Arrays.deepToString(strings);
    }
  }
}
