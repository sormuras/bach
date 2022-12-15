package run.bach;

import java.io.PrintWriter;
import java.io.Writer;
import java.lang.System.Logger.Level;
import java.util.spi.ToolProvider;
import run.duke.ToolCall;
import run.duke.Toolbox;
import run.duke.Workbench;
import run.duke.Workpieces;

record Bach(Workpieces workpieces, Toolbox toolbox) implements Workbench, BachRunner {
  Bach {
    var printer = workpieces.get(Printer.class);
    var project = workpieces.get(Project.class);
    printer.log(Level.DEBUG, "Bach initialized");
    printer.log(Level.DEBUG, "  printer: " + printer.threshold());
    printer.log(Level.DEBUG, "  project: " + project.toNameAndVersion());
    printer.log(Level.DEBUG, "  toolbox: " + toolbox.tools().size());
  }

  @Override
  public Workbench workbench() {
    return this;
  }

  @Override
  public <T> T workpiece(Class<T> type) {
    return workpieces.get(type);
  }

  @Override
  public void run(ToolCall call, ToolProvider provider, String... args) {
    printer().log(Level.INFO, "+ " + call.toCommandLine());
    var silent = options().silent();
    try {
      var out = silent ? new PrintWriter(Writer.nullWriter()) : printer().out();
      var err = silent ? new PrintWriter(Writer.nullWriter()) : printer().err();
      var code = provider.run(out, err, args);
      if (code != 0) throw new RuntimeException(provider.name() + " failed with error " + code);
    } catch (Throwable throwable) {
      throwable.printStackTrace(System.err);
    }
  }
}
