package com.github.sormuras.bach;

import java.util.Formattable;
import java.util.Formatter;

/** Global settings. */
public record Configuration(
    Printer printer, Flags flags, Paths paths, ToolFinder finder, ToolCallTweak tweak)
    implements Formattable {

  public static Configuration ofDefaults() {
    var printer = Printer.ofSystem();
    var paths = Paths.ofCurrentWorkingDirectory();
    var flags = Flags.of();
    var finder =
        ToolFinder.compose(
            ToolFinder.ofToolsInModulePath(paths.externalModules()),
            ToolFinder.ofJavaTools(paths.externalTools()),
            ToolFinder.ofSystemTools());
    var tweak = ToolCallTweak.identity();
    return new Configuration(printer, flags, paths, finder, tweak);
  }

  @Override
  public void formatTo(Formatter formatter, int flags, int width, int precision) {
    formatter.format(
        """
        Configuration
          Flags
                         set = %s
          Paths
                        root = %s
                         out = %s
        """,
        flags().set(), paths.root(), paths.out());
  }



  public boolean isDryRun() {
    return flags.set().contains(Flag.DRY_RUN);
  }

  public boolean isVerbose() {
    return flags.set().contains(Flag.VERBOSE);
  }
}
