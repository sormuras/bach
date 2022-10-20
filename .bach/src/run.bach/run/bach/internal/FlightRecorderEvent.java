package run.bach.internal;

import jdk.jfr.Category;
import jdk.jfr.Enabled;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

public sealed interface FlightRecorderEvent {
  @Category("Bach")
  @Enabled(false)
  @StackTrace(false)
  abstract sealed class BachEvent extends Event implements FlightRecorderEvent {
    @Label("Tool Name")
    public String name;

    @Label("Tool Arguments")
    public String args;
  }

  @Enabled
  @Label("Tool Operator Run")
  @Name("Bach.ToolOperatorRun")
  final class ToolOperatorRun extends BachEvent {}

  @Enabled
  @Label("Tool Provider Run")
  @Name("Bach.ToolProviderRun")
  final class ToolProviderRun extends BachEvent {
    public int code;
    public String out;
    public String err;
  }
}
