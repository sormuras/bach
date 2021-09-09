package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.DurationSupport;

/** A printer of various types using the configured output writer. */
public record Printer(Bach bach) {

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
}
