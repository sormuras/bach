package run.bach;

import java.lang.System.Logger.Level;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;
import run.duke.CommandLineInterface;
import run.duke.CommandLineInterface.Help;
import run.duke.CommandLineInterface.Name;

/**
 * Bach's option.
 *
 * @param verbose Prints more and finer detailed messages
 * @param silent Prints no messages
 * @param dryRun Composes everything, but doesn't run any tool call
 * @param help Prints the help text
 * @param __printer_margin Printer margin option. For a margin value of 10 characters, the following
 *     strings are printed as {@code "1234567890" -> "1234567890"} and {@code "hello world" ->
 *     "hello w..."}.
 * @param __printer_threshold Lowest logger level to print messages for
 * @param __trust Trusted identities
 * @param calls A sequence of tool calls separated by {@code +} characters
 */
public record Options(
    @Name({"--verbose", "--forte"}) @Help("Prints more and finer detailed messages")
        boolean verbose,
    @Name({"--very-quiet", "--silent", "--piano"}) @Help("Prints no message") boolean silent,
    @Name("--dry-run") @Help("Composes everything, but doesn't run any tool call") boolean dryRun,
    @Name({"--help", "-help", "-h", "/?", "?"}) boolean help,
    @Help("Maximum line width used by the message printer") Optional<String> __printer_margin,
    @Help("Lowest logger level to print message for") Optional<String> __printer_threshold,
    @Name("--trust") @Help("Trusted identities") List<String> __trust,
    @Help("A sequence of tool calls separated by + characters") String... calls) {

  private static final CommandLineInterface<Options> PARSER =
      new CommandLineInterface<>(MethodHandles.lookup(), Options.class);

  public static final Options DEFAULTS = Options.of(/* no arguments */ );

  public static Options of(String... args) {
    return PARSER.split(args);
  }

  public static String toHelp() {
    return PARSER.help();
  }

  public int printerMargin(int other) {
    return Integer.parseInt(__printer_margin.orElse(Integer.toString(other)));
  }

  public Level printerThreshold(Level other) {
    return Level.valueOf(__printer_threshold.orElse(other.name()).toUpperCase());
  }
}
