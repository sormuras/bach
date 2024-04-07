/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach;

import static run.bach.ToolCall.Carrier.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.System.Logger.Level;
import run.bach.internal.FlightRecorderEvent;

/** Extendable tool runner implementation. */
public class Workbench implements ToolRunner {
  protected final ToolFinder finder;

  public Workbench(ToolFinder finder) {
    this.finder = finder;
  }

  @Override
  public ToolRun run(ToolCall call) {
    announce(call);
    var event = new FlightRecorderEvent.ToolRunEvent();
    try {
      var tool = computeToolInstance(call);
      var args = computeArgumentsArray(call);
      var out = new StringPrintWriterMirror(computePrintWriter(Level.INFO));
      var err = new StringPrintWriterMirror(computePrintWriter(Level.ERROR));
      event.name = call.tool().name();
      event.tool = tool.provider().getClass();
      event.args = String.join(" ", args);

      int code;
      RuntimeException uncheckedRuntimeException = null;
      try {
        Thread.currentThread().setContextClassLoader(tool.getClass().getClassLoader());
        event.begin();
        code = tool.provider().run(out, err, args);
        event.end();
      } catch (RuntimeException exception) {
        uncheckedRuntimeException = exception; // rethrown later
        code = Integer.MIN_VALUE;
      }

      var run = new ToolRun(call, tool, code, out.toString(), err.toString());
      event.code = run.code();
      event.out = run.out();
      event.err = run.err();
      verify(run);

      if (uncheckedRuntimeException != null) throw uncheckedRuntimeException;

      return run;
    } finally {
      event.commit();
    }
  }

  protected void announce(ToolCall call) {
    System.out.println("| " + call.toCommandLine());
  }

  protected PrintWriter computePrintWriter(Level level) {
    var severity = level.getSeverity();
    var stream = severity >= Level.ERROR.getSeverity() ? System.err : System.out;
    return new PrintWriter(stream, true);
  }

  protected Tool computeToolInstance(ToolCall call) {
    return switch (call.tool()) {
      case ByName(String name) -> finder.findToolOrElseThrow(name);
      case Direct(Tool tool) -> tool;
    };
  }

  protected String[] computeArgumentsArray(ToolCall call) {
    return call.arguments().toArray(String[]::new);
  }

  protected void verify(ToolRun run) {
    var code = run.code();
    if (code == 0) return;
    var name = run.call().tool().name();
    throw new RuntimeException("%s finished with exit code %d".formatted(name, code));
  }

  private static class StringPrintWriter extends PrintWriter {
    StringPrintWriter() {
      super(new StringWriter());
    }

    @Override
    public String toString() {
      return super.out.toString().stripTrailing();
    }
  }

  private static class StringPrintWriterMirror extends StringPrintWriter {
    private final PrintWriter other;

    StringPrintWriterMirror(PrintWriter other) {
      this.other = other != null ? other : new PrintWriter(Writer.nullWriter());
    }

    @Override
    public void flush() {
      super.flush();
      other.flush();
    }

    @Override
    public void write(int c) {
      super.write(c);
      other.write(c);
    }

    @Override
    public void write(char[] buf, int off, int len) {
      super.write(buf, off, len);
      other.write(buf, off, len);
    }

    @Override
    public void write(String s, int off, int len) {
      super.write(s, off, len);
      other.write(s, off, len);
    }

    @Override
    public void println() {
      super.println();
      other.println();
    }
  }
}
