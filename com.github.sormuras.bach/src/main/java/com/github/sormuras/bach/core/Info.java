package com.github.sormuras.bach.core;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.ToolOperator;
import java.io.PrintWriter;

public class Info implements ToolOperator {
  @Override
  public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
    out.printf("%s", bach.configuration());
    out.printf("%s", bach.project());
    return 0;
  }
}
