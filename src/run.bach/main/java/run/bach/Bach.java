package run.bach;

import java.io.PrintWriter;
import java.io.Writer;
import java.lang.System.Logger.Level;
import java.util.spi.ToolProvider;
import run.duke.ToolCall;

public record Bach(
    Options options, Folders folders, Printer printer, Project project, Toolbox toolbox)
    implements ProjectToolRunner {
  public Bach {
    printer.log(Level.DEBUG, "Bach initialized");
    printer.log(Level.DEBUG, "  printer: " + printer.threshold());
    printer.log(Level.DEBUG, "  project: " + project.toNameAndVersion());
    printer.log(Level.DEBUG, "  toolbox: " + toolbox.finders().list().size());
  }

  @Override
  public void run(ToolCall call) {
    printer().log(Level.INFO, "+ " + call.toCommandLine());
    ProjectToolRunner.super.run(call);
  }

  @Override
  public void run(ToolProvider provider, String... args) {
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
