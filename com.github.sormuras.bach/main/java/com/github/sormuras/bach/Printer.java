package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.DurationSupport;
import com.github.sormuras.bach.internal.ToolProviderSupport;
import java.util.Comparator;
import java.util.spi.ToolProvider;

/** A printer of various types using the configured output writer. */
public record Printer(Bach bach) {

  public void printTools() {
    print(bach.configuration().tooling().finder());
  }

  public void print(ToolFinder finder) {
    var tools = finder.findAll().stream().sorted(Comparator.comparing(ToolProvider::name)).toList();
    tools.stream().map(Printer::toString).forEach(bach.out()::println);
    bach.out().printf("  %d tool%s%n", tools.size(), tools.size() == 1 ? "" : "s");
  }

  public void print(ToolRun run) {
    print(run, false, 4);
  }

  public void print(ToolRun run, boolean summarize, int indentation) {
    var output = run.output();
    var errors = run.errors();
    if (!output.isEmpty()) bach.out().println(output.indent(indentation).stripTrailing());
    if (!errors.isEmpty()) bach.err().println(errors.indent(indentation).stripTrailing());
    if (summarize) {
      var printer = run.isError() ? bach.err() : bach.out();
      printer.printf(
          "Run of %s with %d argument%s took %s and finished with exit code %d%n",
          run.name(),
          run.args().size(),
          run.args().size() == 1 ? "" : "s",
          DurationSupport.toHumanReadableString(run.duration()),
          run.code());
    }
  }

  static String toString(ToolProvider provider) {
    return String.format("- `%s` by `%s`", provider.name(), ToolProviderSupport.describe(provider));
  }
}
