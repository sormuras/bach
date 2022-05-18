package com.github.sormuras.bach;

public record Configuration(Printer printer, Flags flags, Paths paths, ToolFinder finder) {

  public static Configuration ofDefaults() {
    var printer = Printer.ofSystem();
    var paths = Paths.ofCurrentWorkingDirectory();
    var flags = Flags.of();
    var finder =
        ToolFinder.compose(
            ToolFinder.ofToolsInModulePath(paths.externalModules()),
            ToolFinder.ofJavaTools(paths.externalTools()),
            ToolFinder.ofSystemTools());
    return new Configuration(printer, flags, paths, finder);
  }

  public Configuration with(Printer printer) {
    return new Configuration(printer, flags, paths, finder);
  }

  public Configuration with(Flags flags) {
    return new Configuration(printer, flags, paths, finder);
  }

  public Configuration with(Paths paths) {
    return new Configuration(printer, flags, paths, finder);
  }

  public Configuration with(ToolFinder finder) {
    return new Configuration(printer, flags, paths, finder);
  }

  public boolean isDryRun() {
    return flags.set().contains(Flag.DRY_RUN);
  }

  public boolean isVerbose() {
    return flags.set().contains(Flag.VERBOSE);
  }
}
