package run.bach;

import java.io.PrintWriter;
import java.io.Writer;
import java.lang.System.Logger.Level;
import java.util.spi.ToolProvider;
import run.duke.ToolCall;
import run.duke.ToolNotFoundException;
import run.duke.Toolbox;

public record Bach(Options options, Folders folders, Printer printer, Toolbox toolbox)
    implements ProjectToolRunner {
  public Bach {
    printer.log(Level.DEBUG, "Bach initialized");
    printer.log(Level.DEBUG, "  printer: " + printer.threshold());
    printer.log(Level.DEBUG, "  toolbox: " + toolbox.size());
  }

  @Override
  public void run(ToolCall call) {
    var tool = call.name();
    var args = call.arguments().toArray(String[]::new);
    printer().log(Level.INFO, "+ " + tool + " " + String.join(" ", args));
    var found = toolbox.find(tool, this);
    if (found.isEmpty()) throw new ToolNotFoundException(tool);
    var code = run(found.get().provider(), args);
    if (code != 0) throw new RuntimeException(tool + " failed with error " + code);
  }

  private int run(ToolProvider tool, String... args) {
    var silent = options().silent();
    try {
      var out = silent ? new PrintWriter(Writer.nullWriter()) : printer().out();
      var err = silent ? new PrintWriter(Writer.nullWriter()) : printer().err();
      return tool.run(out, err, args);
    } catch (Throwable throwable) {
      throwable.printStackTrace(System.err);
      return -1;
    }
  }
}
