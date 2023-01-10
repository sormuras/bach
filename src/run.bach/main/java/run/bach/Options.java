package run.bach;

import static java.lang.invoke.MethodHandles.lookup;

import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import run.duke.CommandLineInterface.Help;
import run.duke.CommandLineInterface.Name;
import run.duke.CommandLineInterface.Splitter;
import run.duke.Duke;

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
    @Name({"--help", "-help", "-h", "/?", "?"}) Flag help,
    @Name({"--verbose", "--forte"}) @Help("Prints more and finer detailed messages")
        boolean verbose,
    @Name({"--very-quiet", "--silent", "--piano"}) @Help("Prints no message") boolean silent,
    @Name("--dry-run") @Help("Composes everything, but doesn't run any tool call") boolean dryRun,
    @Help("Change root directory of the project") Optional<String> __chroot,
    @Help("Change output directory for generated assets") Optional<String> __output_directory,
    @Help("Maximum line width used by the message printer") Optional<String> __printer_margin,
    @Help("Lowest logger level to print message for") Optional<String> __printer_threshold,
    @Name("--trust") @Help("Trusted identities") List<String> __trust,
    @Help("Name of the project") Optional<String> __project_name,
    @Help("Version information of the build") Optional<String> __project_version,
    @Help("Zone date time of the build") Optional<String> __project_version_timestamp,
    @Help("A sequence of tool calls separated by + characters") String... calls) {

  public record Flag() {}

  private static final Splitter<Options> SPLITTER = Duke.splitter(lookup(), Options.class);

  public static Options of(String... args) {
    return SPLITTER.split(args);
  }

  public static String toHelp() {
    return "HERE BE HELP!";
  }

  public Path rootDirectory() {
    return __chroot.map(Path::of).orElse(Path.of(""));
  }

  public Path outputDirectory(String defaultOutputDirectory) {
    return __output_directory.map(Path::of).orElse(rootDirectory().resolve(defaultOutputDirectory));
  }

  public int printerMargin(int other) {
    return Integer.parseInt(__printer_margin.orElse(Integer.toString(other)));
  }

  public Level printerThreshold(Level other) {
    return Level.valueOf(__printer_threshold.orElse(other.name()).toUpperCase());
  }

  public String projectName(String defaultName) {
    return __project_name.orElse(defaultName);
  }

  public String projectVersion(String defaultVersion) {
    return __project_version.orElse(defaultVersion);
  }

  public ZonedDateTime projectVersionTimestampOrNow() {
    return __project_version_timestamp().map(ZonedDateTime::parse).orElseGet(ZonedDateTime::now);
  }
}
