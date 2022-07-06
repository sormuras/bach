package com.github.sormuras.bach.core;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.ToolOperator;
import com.github.sormuras.bach.internal.BachInfoFormatter;
import java.io.PrintWriter;

public class Info implements ToolOperator {
  @Override
  public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
    var formatter = new BachInfoFormatter(bach.configuration(), bach.project());
    out.println(formatter.formatConfiguration());
    out.println(formatter.formatProject());
    return 0;
  }
}
