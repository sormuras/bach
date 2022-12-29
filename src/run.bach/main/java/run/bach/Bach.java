package run.bach;

import java.io.PrintWriter;
import java.io.Writer;
import java.lang.System.Logger.Level;
import java.util.List;
import java.util.spi.ToolProvider;
import run.bach.internal.FlightRecorderEvent;
import run.bach.internal.StringPrintWriterMirror;
import run.duke.ToolCall;
import run.duke.ToolOperator;
import run.duke.ToolRunner;
import run.duke.Workbench;
import run.duke.Workpieces;

public record Bach(Workpieces workpieces) implements Workbench, ToolRunner {
  public Bach {
    var printer = workpieces.get(Printer.class);
    var project = workpieces.get(Project.class);
    var toolkit = workpieces.get(Toolkit.class);
    printer.log(Level.DEBUG, "Bach initialized");
    printer.log(Level.DEBUG, "  printer: " + printer.threshold());
    printer.log(Level.DEBUG, "  project: " + project.toNameAndVersion());
    printer.log(Level.DEBUG, "  toolbox: " + toolkit.toolbox().tools().size());
    printer.log(Level.DEBUG, "  tweaks : " + toolkit.tweaks().list().size());
  }

  public Project project() {
    return workpiece(Project.class);
  }

  public Options options() {
    return workpiece(Options.class);
  }

  public Folders folders() {
    return workpiece(Folders.class);
  }

  public Printer printer() {
    return workpiece(Printer.class);
  }

  public Toolkit toolkit() {
    return workpiece(Toolkit.class);
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
    var provider = call.provider();
    if (provider.isPresent()) return provider.get();
    var tool = call.tool();
    return find(tool).orElseThrow(() -> new ToolNotFoundException(tool)).provider();
  }

  private void run(ToolProvider provider, List<String> arguments) {
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
      if (provider instanceof Bach.Operator operator) {
        event.code = operator.run(this, out, err, args);
      } else if (provider instanceof ToolOperator operator) {
        event.code = operator.run(this, out, err, args);
      } else {
        event.code = provider.run(out, err, args);
      }
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

  @FunctionalInterface
  public interface Operator extends ToolOperator {
    default int run(Workbench workbench, PrintWriter out, PrintWriter err, String... args) {
      throw new UnsupportedOperationException();
    }

    int run(Bach bach, PrintWriter out, PrintWriter err, String... args);
  }
}
