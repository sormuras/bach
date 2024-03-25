/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.internal;

import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;

@Category("Bach")
public abstract sealed class FlightRecorderEvent extends Event {
  @Label("Tool Run")
  @Name("Bach.ToolRun")
  public static final class ToolRunEvent extends FlightRecorderEvent {
    @Label("Tool")
    public Class<?> tool;

    @Label("Name")
    public String name;

    @Label("Arguments")
    public String args;

    @Label("Exit Code")
    public int code;

    @Label("Output")
    public String out;

    @Label("Errors")
    public String err;
  }
}
