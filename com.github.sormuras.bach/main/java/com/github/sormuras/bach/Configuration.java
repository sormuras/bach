package com.github.sormuras.bach;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;

public record Configuration(
    boolean verbose,
    boolean lenient,
    int timeout,
    Printing printing,
    Tooling tooling,
    Options.ProjectOptions projectOptions) {

  public static Configuration of() {
    return new Configuration(
        false,
        false,
        9,
        new Printing(new PrintWriter(System.out, true), new PrintWriter(System.err, true)),
        new Tooling(
            ToolFinder.compose(
                ToolFinder.ofSystem(),
                ToolFinder.ofBach(),
                ToolFinder.ofProviders(Path.of(".bach", "external-tool-providers")),
                ToolFinder.ofPrograms(Path.of(".bach", "external-tool-programs"))
                )),
        new Options.ProjectOptions(Optional.empty(), Optional.empty()));
  }

  static final String TIMESTAMP_PATTERN = "yyyyMMdd-HHmmss";

  static final String LOGBOOK_MARKDOWN_FILE = ".bach/workspace/logbook.md";

  static final String LOGBOOK_ARCHIVE_FILE = ".bach/workspace/logbooks/logbook-{TIMESTAMP}.md";

  public record Printing(PrintWriter out, PrintWriter err) {}

  public record Factoring(LogbookFactory logbookFactory) {
    @FunctionalInterface
    public interface LogbookFactory extends Supplier<Logbook> {}
  }

  public record Tooling(ToolFinder finder) {}

  public Configuration with(Options options) {
    return new Configuration(
        options.configurationOptions().verbose().orElse(verbose),
        options.configurationOptions().verbose().orElse(lenient),
        options.configurationOptions().timeout().orElse(timeout),
        printing,
        tooling,
        options.projectOptions());
  }
}
