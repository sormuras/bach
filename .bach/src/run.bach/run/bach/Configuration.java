package run.bach;

import java.util.StringJoiner;

public record Configuration(CLI cli, Printer printer) {
  public String toString(int indent) {
    var joiner = new StringJoiner("\n");
    joiner.add("Command-Line Interface");
    joiner.add(cli.toString(2));
    joiner.add("Printer");
    joiner.add(printer.toString(2));
    return joiner.toString().indent(indent).stripTrailing();
  }
}
