package test.base.magnificat.api;

import java.util.Locale;

public enum Option {
  CLI_BACH_ROOT("."),
  CLI_BACH_INFO_FOLDER(".bach"),
  CLI_BACH_INFO_MODULE("bach.info"),

  VERBOSE,

  PROJECT_NAME(ProjectInfo.DEFAULT_PROJECT_NAME),

  PROJECT_VERSION(ProjectInfo.DEFAULT_PROJECT_VERSION),

  /**
   * The list of {@link Action} names to be executed in the specified order.
   *
   * <p>Usage example:
   *
   * <pre>{@code
   * --action clean
   * --action main-compile
   * --action test-compile
   * --action test
   * }</pre>
   *
   * @see Action
   */
  ACTION(1, true, ProjectInfo.DEFAULT_ACTIONS);

  public static Option ofCli(String cli) {
    var name = cli.substring(2).toUpperCase(Locale.ROOT).replace('-', '_');
    return valueOf(name);
  }

  public static String toCli(Option option) {
    return "--" + option.name().toLowerCase(Locale.ROOT).replace('_', '-');
  }

  final String cli;
  final String[] defaults;
  final int cardinality;
  final boolean repeatable;

  Option() {
    this(0, false, "false");
  }

  Option(String defaultValue) {
    this(1, false, defaultValue);
  }

  Option(int cardinality, boolean repeatable, String... defaults) {
    this.cli = toCli(this);
    this.defaults = defaults;
    this.cardinality = cardinality;
    this.repeatable = repeatable;
  }

  public String cli() {
    return cli;
  }

  public int cardinality() {
    return cardinality;
  }

  public boolean isRepeatable() {
    return repeatable;
  }

  public String[] defaults() {
    return defaults;
  }

  public boolean isFlag() {
    return cardinality == 0;
  }
}
