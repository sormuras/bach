package run.bach;

import java.io.PrintWriter;
import java.io.Writer;
import java.lang.System.Logger.Level;
import java.util.List;
import java.util.spi.ToolProvider;
import run.bach.internal.FlightRecorderEvent;
import run.bach.internal.StringPrintWriterMirror;
import run.duke.Tool;
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
  public <T> T workpiece(Class<T> type) {
    return workpieces.get(type);
  }

  @Override
  public void run(ToolCall toolCall) {
    var call = toolCall.withTweaks(toolkit().tweaks());
    printer().log(Level.INFO, "+ " + call.toCommandLine());
    var provider = switchOverToolCallAndYieldToolProvider(call);
    run(provider, call.arguments());
  }

  private ToolProvider switchOverToolCallAndYieldToolProvider(ToolCall call) {
    var string = call.tool();
    var tool = find(string).orElseThrow(() -> new ToolNotFoundException(string));
    if (tool instanceof Tool.OfOperator of) return of.operator().provider(this);
    if (tool instanceof Tool.OfProvider of) return of.provider();
    throw new AssertionError("Unsupported tool type " + tool.getClass());
  }

  @Override
  public void run(ToolProvider provider, List<String> arguments) {
    var args = arguments.toArray(String[]::new);
    var code = run(provider, args);
    if (code != 0) throw new RuntimeException(provider.name() + " failed with error " + code);
  }

  private int run(ToolProvider provider, String... args) {
    Thread.currentThread().setContextClassLoader(provider.getClass().getClassLoader());
    var event = new FlightRecorderEvent.ToolRun();
    event.name = provider.name();
    event.args = String.join(" ", args);
    var silent = options().silent();
    try {
      var out = newPrintWriter(silent, printer().out());
      var err = newPrintWriter(silent, printer().err());
      event.begin();
      event.code = provider.run(out, err, args);
      event.end();
      event.out = out.toString().strip();
      event.err = err.toString().strip();
    } catch (Throwable throwable) {
      if (throwable instanceof RuntimeException re) throw re;
      throw new RuntimeException("Caught unexpected throwable", throwable);
    } finally {
      event.commit();
    }
    return event.code;
  }

  private PrintWriter newPrintWriter(boolean silent, PrintWriter writer) {
    return silent ? new PrintWriter(Writer.nullWriter()) : new StringPrintWriterMirror(writer);
  }
}
