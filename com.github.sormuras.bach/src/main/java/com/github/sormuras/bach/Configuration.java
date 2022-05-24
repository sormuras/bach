package com.github.sormuras.bach;

/** Global settings. */
public record Configuration(
    Printer printer, Flags flags, Paths paths, ToolFinder finder, ToolCallTweak tweak) {

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

  public boolean isDryRun() {
    return flags.set().contains(Flag.DRY_RUN);
  }

  public boolean isVerbose() {
    return flags.set().contains(Flag.VERBOSE);
  }
}
