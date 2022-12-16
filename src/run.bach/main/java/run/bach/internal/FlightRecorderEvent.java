package run.bach.internal;

import jdk.jfr.Category;
import jdk.jfr.Enabled;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

public sealed interface FlightRecorderEvent {
  @Category("Bach")
  @Enabled
  @Label("Tool Run")
  @Name("Bach.ToolRun")
  @StackTrace(false)
  final class ToolRun extends Event implements FlightRecorderEvent {
    @Label("Tool Name")
    public String name;

    @Label("Tool Arguments")
    public String args;

    @Label("Exit Code")
    public int code;

    @Label("Output")
    public String out;

    @Label("Errors")
    public String err;
  }
}
