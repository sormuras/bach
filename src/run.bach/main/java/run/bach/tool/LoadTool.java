package run.bach.tool;

import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.nio.file.Path;
import java.util.spi.ToolProvider;
import run.bach.Browser;
import run.bach.ProjectTool;
import run.duke.CommandLineInterface;
import run.duke.Workbench;

public class LoadTool extends ProjectTool {
  record Options(boolean __help, String what, String that, String... more) {}

  public LoadTool() {}

  protected LoadTool(Workbench workbench) {
    super(workbench);
  }

  @Override
  public final String name() {
    return "load";
  }

  @Override
  public ToolProvider provider(Workbench workbench) {
    return new LoadTool(workbench);
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    var options = CommandLineInterface.of(MethodHandles.lookup(), Options.class).split(args);
    if (options.__help()) {
      out.println("Usage: %s <what> <that> <more...>".formatted(name()));
      return 0;
    }
    var browser = workbench().workpiece(Browser.class);
    switch (options.what) {
      case "file" -> browser.load(URI.create(options.that), Path.of(options.more[0]));
      case "head" -> out.println(browser.head(URI.create(options.that)));
      case "headers" -> {
        for (var entry : browser.head(URI.create(options.that)).headers().map().entrySet()) {
          out.println(entry.getKey());
          for (var line : entry.getValue()) out.println("  " + line);
        }
      }
      case "text" -> out.println(browser.read(URI.create(options.that)));
      default -> {
        err.println("Unknown load type: " + options.what);
        return 1;
      }
    }
    return 0;
  }
}
