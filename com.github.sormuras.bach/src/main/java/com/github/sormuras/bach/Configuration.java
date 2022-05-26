package com.github.sormuras.bach;

import java.util.ArrayList;
import java.util.Formattable;
import java.util.Formatter;
import java.util.List;
import java.util.TreeMap;

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
          Tools
        %s
        """,
        flags().set(), paths.root(), paths.out(), formatTools());
  }

  private String formatTools() {
    var names = new TreeMap<String, List<Tool>>();
    for (var tool : finder.findAll()) {
      var name = tool.name().substring(tool.name().lastIndexOf('/') + 1);
      names.computeIfAbsent(name, key -> new ArrayList<>()).add(tool);
    }
    var lines = new ArrayList<String>();
    for (var entry : names.entrySet()) {
      var name = entry.getKey();
      var tools = entry.getValue();
      var first = tools.get(0);
      lines.add(formatLine("%20s -> %s [%s]", name, first));
      tools.stream().skip(1).forEach(tool -> lines.add(formatLine("%20s    %s [%s]", "", tool)));
    }
    return String.join("\n", lines);
  }

  private static String formatLine(String format, String name, Tool tool) {
    return format.formatted(name, tool.name(), tool.provider().getClass().getSimpleName());
  }

  public boolean isDryRun() {
    return flags.set().contains(Flag.DRY_RUN);
  }

  public boolean isVerbose() {
    return flags.set().contains(Flag.VERBOSE);
  }
}
