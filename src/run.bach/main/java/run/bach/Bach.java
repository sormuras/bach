package run.bach;

import java.io.PrintWriter;
import java.io.Writer;
import java.lang.System.Logger.Level;
import java.util.List;
import java.util.Optional;
import java.util.spi.ToolProvider;
import run.bach.internal.FlightRecorderEvent;
import run.bach.internal.StringPrintWriterMirror;
import run.duke.Tool;
import run.duke.ToolCall;
import run.duke.ToolContext;
import run.duke.ToolLogger;
import run.duke.ToolOperator;

public record Bach(
    ToolContext context,
    Browser browser,
    Folders folders,
    Options options,
    Printer printer,
    Project project,
    Toolkit toolkit,
    Tweaks tweaks)
    implements ProjectRunner {
  public Bach(ToolContext context) {
    this(
        context,
        context.getConstant(Browser.class),
        context.getConstant(Folders.class),
        context.getConstant(Options.class),
        context.getConstant(Printer.class),
        context.getConstant(Project.class),
        context.getConstant(Toolkit.class),
        context.getConstant(Tweaks.class));
    printer.log(Level.DEBUG, "Bach initialized");
  }

  @Override
  public <R extends Record> R getConstant(Class<R> key) {
    return context.getConstant(key);
  }

  @Override
  public List<Tool> tools() {
    return List.copyOf(toolkit.finder().tools());
  }

  @Override
  public Optional<Tool> findTool(String tool) {
    return toolkit.finder().findTool(tool);
  }

  private ToolProvider findToolProvider(ToolCall call) {
    var provider = call.provider();
    if (provider.isPresent()) return provider.get();
    var tool = call.tool();
    var found = findTool(tool);
    if (found.isPresent()) return found.get().provider();
    throw new ToolNotFoundException(tool);
  }

  @Override
  public void run(ToolCall toolCall) {
    var call = toolCall.withTweaks(tweaks);
    printer.log(Level.INFO, "+ " + call.toCommandLine());
    var provider = findToolProvider(call);
    run(provider, call.arguments());
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
    var silent = options.silent();
    try {
      var out = newPrintWriter(silent, printer.out());
      var err = newPrintWriter(silent, printer.err());
      event.begin();
      if (provider instanceof ToolOperator operator) {
        var logger = new ToolLogger(event.name, out, err);
        event.code = operator.run(this, logger, args);
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
}
