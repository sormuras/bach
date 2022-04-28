package com.github.sormuras.bach;

import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

@Category("Bach")
@Name("Bach.RunEvent")
@Label("Run")
@StackTrace(false)
final class ToolRunEvent extends Event {
  String name;
  String args;
  int code;
  String out;
  String err;
}
