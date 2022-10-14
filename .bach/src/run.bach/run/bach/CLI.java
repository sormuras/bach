package run.bach;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

public record CLI(
    Optional<Boolean> __help,
    Optional<Boolean> __offline,
    Optional<Boolean> __verbose,
    Optional<Boolean> __version,
    Optional<String> __printer_threshold,
    Optional<String> __printer_margin,
    Optional<String> __root_path,
    List<String> __trust_signature_email,
    List<Call> calls) {

  public record Call(List<String> command) {}

  public static final String COMMAND_SEPARATOR = "+";

  public static final List<String> HELP_FLAGS = List.of("?", "/?", "-?", "-h", "--help");

  public static final int DEFAULT_PRINTER_MARGIN = 160;

  public CLI() {
    this(
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        List.of(),
        List.of());
  }

  public boolean help() {
    return __help.orElse(false);
  }

  public boolean online() {
    return !offline();
  }

  public boolean offline() {
    return __offline.orElse(false);
  }

  public boolean verbose() {
    return __verbose.orElse(false);
  }

  public boolean version() {
    return __version.orElse(false);
  }

  public System.Logger.Level printerThreshold() {
    var defaultThreshold = verbose() ? System.Logger.Level.DEBUG : System.Logger.Level.INFO;
    return __printer_threshold.map(System.Logger.Level::valueOf).orElse(defaultThreshold);
  }

  public int printerMargin() {
    return __printer_margin.map(Integer::parseInt).orElse(DEFAULT_PRINTER_MARGIN);
  }

  public Path rootPath() {
    return __root_path.map(Path::of).orElse(Path.of(""));
  }

  public List<String> trustSignatureEmails() {
    return __trust_signature_email;
  }

  public String toString(int indent) {
    var joiner = new StringJoiner("\n");
    joiner.add("Optional Flags");
    joiner.add("  --help = " + help());
    joiner.add("  --offline = " + offline() + " implies online = " + online());
    joiner.add("  --verbose = " + verbose());
    joiner.add("  --version = " + version());
    joiner.add("Optional Key-Value Pairs");
    joiner.add("  --printer-margin = " + printerMargin());
    joiner.add("  --printer-threshold = " + printerThreshold());
    joiner.add("  --root-path = \"" + rootPath() + "\" (" + rootPath().toUri() + ")");
    joiner.add("  --trust-signature-email = " + trustSignatureEmails());
    joiner.add("Tool Calls");
    if (calls.isEmpty()) joiner.add("  <empty>");
    calls.forEach(call -> joiner.add("  + " + String.join(" ", call.command())));
    return joiner.toString().indent(indent).stripTrailing();
  }

  public CLI withParsingCommandLineArguments(Path file) {
    if (Files.notExists(file)) return this;
    return withParsingCommandLineArguments(expandArgumentsFile(file));
  }

  public CLI withParsingCommandLineArguments(List<String> args) {
    var arguments = new ArrayDeque<>(args);
    // extract components
    var help = __help.orElse(null);
    var offline = __verbose.orElse(null);
    var verbose = __verbose.orElse(null);
    var version = __verbose.orElse(null);
    var printerThreshold = __printer_threshold.orElse(null);
    var printerMargin = __printer_margin.orElse(null);
    var rootPath = __root_path.orElse(null);
    var trustSignatureEmails = new ArrayList<>(trustSignatureEmails());
    var calls = new ArrayList<>(calls());
    // handle options by parsing flags and key-value paris
    while (!arguments.isEmpty()) {
      var argument = arguments.removeFirst();
      /* expand @file arguments */ {
        if (argument.startsWith("@") && !(argument.startsWith("@@"))) {
          var file = Path.of(argument.substring(1));
          var list = new ArrayList<>(expandArgumentsFile(file));
          Collections.reverse(list);
          list.forEach(arguments::addFirst);
          continue;
        }
      }
      /* parse flags */ {
        if (HELP_FLAGS.contains(argument)) {
          help = Boolean.TRUE;
          continue;
        }
        if (argument.equals("--offline")) {
          offline = Boolean.TRUE;
          continue;
        }
        if (argument.equals("--verbose")) {
          verbose = Boolean.TRUE;
          continue;
        }
        if (argument.equals("--version")) {
          version = Boolean.TRUE;
          continue;
        }
      }
      /* parse key-value pairs */ {
        int separator = argument.indexOf('=');
        var pop = separator == -1;
        var key = pop ? argument : argument.substring(0, separator);
        var val = separator + 1;
        if (key.equals("--printer-threshold")) {
          printerThreshold = pop ? arguments.pop() : argument.substring(val);
          continue;
        }
        if (key.equals("--printer-margin")) {
          printerMargin = pop ? arguments.pop() : argument.substring(val);
          continue;
        }
        if (key.equals("--root-path")) {
          rootPath = pop ? arguments.pop() : argument.substring(val);
          continue;
        }
        if (key.equals("--trust-signature-email")) {
          var value = pop ? arguments.pop() : argument.substring(val);
          trustSignatureEmails.addAll(List.of(value.split(",")));
          continue;
        }
      }
      // restore argument because first unhandled option marks the beginning of the commands
      arguments.addFirst(argument);
      break;
    }
    // parse calls from remaining arguments
    if (!arguments.isEmpty()) {
      var elements = new ArrayList<String>();
      while (true) {
        var empty = arguments.isEmpty();
        if (empty || arguments.peekFirst().equals(COMMAND_SEPARATOR)) {
          calls.add(new Call(List.copyOf(elements)));
          elements.clear();
          if (empty) break;
          arguments.pop(); // consume delimiter
        }
        elements.add(arguments.pop()); // consume element
      }
    }
    // compose configuration
    return new CLI(
        Optional.ofNullable(help),
        Optional.ofNullable(offline),
        Optional.ofNullable(verbose),
        Optional.ofNullable(version),
        Optional.ofNullable(printerThreshold),
        Optional.ofNullable(printerMargin),
        Optional.ofNullable(rootPath),
        List.copyOf(trustSignatureEmails),
        List.copyOf(calls));
  }

  private static List<String> expandArgumentsFile(Path file) {
    if (Files.notExists(file)) throw new RuntimeException("Arguments file not found: " + file);
    var arguments = new ArrayList<String>();
    try {
      for (var line : Files.readAllLines(file)) {
        line = line.strip();
        if (line.isEmpty()) continue;
        if (line.startsWith("#")) continue;
        if (line.startsWith("@") && !line.startsWith("@@")) {
          throw new IllegalArgumentException("Expand arguments file not allowed: " + line);
        }
        arguments.add(line);
      }
    } catch (RuntimeException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new RuntimeException("Read all lines from file failed: " + file, exception);
    }
    return List.copyOf(arguments);
  }
}
